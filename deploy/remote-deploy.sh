#!/usr/bin/env bash
# EC2 에서 실행되는 배포 스크립트. GitHub Actions 가 stdin 으로 흘려보낸다.
#
#   ssh ubuntu@host "BACKEND_TAG=... AI_TAG=... bash -s" < deploy/remote-deploy.sh
#
# 순서가 중요하다.
#  - DB 백업을 Flyway 마이그레이션보다 먼저 한다. 복제본이 없어 되돌릴 방법이 이것뿐이다.
#  - AI 를 백엔드보다 먼저 올린다. 그래야 구 백엔드가 신 AI 를 만나는 상태가 되어 무해하다.
#    반대 순서면 신 백엔드가 없는 엔드포인트를 호출해 500 이 난다.

set -euo pipefail

: "${BACKEND_TAG:?BACKEND_TAG 가 필요하다}"
: "${AI_TAG:?AI_TAG 가 필요하다}"
: "${GHCR_USER:?}"
: "${GHCR_TOKEN:?}"

cd /opt/iread
COMPOSE="docker compose -f docker-compose.prod.yml"

envval() { grep -E "^$1=" .env | head -1 | cut -d= -f2-; }

wait_healthy() {
    local name="$1" timeout="$2" waited=0 st
    while [ "$waited" -lt "$timeout" ]; do
        st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$name" 2>/dev/null || echo missing)
        case "$st" in
            healthy)   echo "    $name healthy (${waited}초)"; return 0 ;;
            unhealthy) echo "    $name unhealthy"; docker logs --tail 40 "$name" 2>&1 | sed 's/^/      /'; return 1 ;;
        esac
        sleep 5
        waited=$((waited + 5))
    done
    echo "    $name 타임아웃 (${timeout}초, 마지막 상태=$st)"
    docker logs --tail 60 "$name" 2>&1 | sed 's/^/      /'
    return 1
}

PREV_BACKEND=$(envval BACKEND_TAG)
PREV_AI=$(envval AI_TAG)

rollback() {
    echo ">>> 실패했습니다. 이전 태그로 되돌립니다 (backend=$PREV_BACKEND ai=$PREV_AI)"
    sed -i "s|^BACKEND_TAG=.*|BACKEND_TAG=${PREV_BACKEND}|" .env
    sed -i "s|^AI_TAG=.*|AI_TAG=${PREV_AI}|" .env
    $COMPOSE up -d ai backend || true
    exit 1
}

echo ">>> [1/7] ghcr 로그인"
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin >/dev/null
echo "    완료"

echo ">>> [2/7] 배포 대상"
echo "    이전: backend=$PREV_BACKEND  ai=$PREV_AI"
echo "    신규: backend=$BACKEND_TAG  ai=$AI_TAG"

echo ">>> [3/7] DB 백업 (마이그레이션 전)"
mkdir -p backups
if docker inspect iread-mysql >/dev/null 2>&1; then
    STAMP=$(date +%Y%m%d-%H%M%S)
    DB=$(envval MYSQL_DATABASE)
    ROOT_PW=$(envval MYSQL_ROOT_PASSWORD)
    OUT="backups/${DB}-${STAMP}.sql.gz"
    # 이 스크립트는 `ssh host "bash -s" < 파일` 로 stdin 을 통해 전달된다.
    # exec 가 stdin 을 물려받으면 스크립트의 나머지 절반을 삼켜 bash 가 여기서
    # EOF(exit 0)로 끝나 버린다. 배포가 [3/7]에서 조용히 멈추던 원인.
    $COMPOSE exec -T mysql mysqldump -u root -p"$ROOT_PW" \
        --single-transaction --routines --triggers --events "$DB" </dev/null 2>/dev/null | gzip > "$OUT"
    echo "    $OUT ($(du -h "$OUT" | cut -f1))"
    # 최근 10개만 남긴다. 디스크는 299GB 남지만 무한정 쌓을 이유는 없다.
    ls -1t backups/*.sql.gz 2>/dev/null | tail -n +11 | xargs -r rm --
else
    echo "    mysql 컨테이너가 없어 최초 배포로 판단하고 건너뜁니다"
fi

echo ">>> [4/7] .env 태그 갱신"
sed -i "s|^BACKEND_TAG=.*|BACKEND_TAG=${BACKEND_TAG}|" .env
sed -i "s|^AI_TAG=.*|AI_TAG=${AI_TAG}|" .env
echo "    완료"

echo ">>> [5/7] 이미지 내려받기"
$COMPOSE pull ai backend

echo ">>> [6/7] AI 먼저 교체"
$COMPOSE up -d mysql redis ai
wait_healthy iread-ai 120 || rollback
# lexicon DB 가 없으면 /health 는 UP 이지만 이야기 어휘 기능만 죽는다. 조용히 지나가지 않도록 찍는다.
LEX=$(docker exec iread-ai python -c "import json,urllib.request;print(json.load(urllib.request.urlopen('http://127.0.0.1:8080/health'))['lexiconStatus'])" 2>/dev/null || echo UNKNOWN)
echo "    lexiconStatus=$LEX"
[ "$LEX" = "READY" ] || echo "    경고: lexicon 이 READY 가 아닙니다. /opt/iread/lexicon/story-lexicon.sqlite3 를 확인하세요."

echo ">>> [7/7] 백엔드 교체"
$COMPOSE up -d backend
wait_healthy iread-backend 240 || rollback

echo ">>> 정리"
docker image prune -f >/dev/null
echo "    사용 디스크: $(df -h / | awk 'NR==2{print $3" / "$2" ("$5")"}')"
$COMPOSE ps --format '    {{.Service}}: {{.Status}}'
echo ">>> 배포 성공"
