# iRead 배포 안내

EC2 한 대(`i15b105.p.ssafy.io`, t2.xlarge)에 전체 스택을 올린다.
배포는 **사람이 main 에 머지하고 버튼을 누르는** 방식이다. 자동 트리거는 없다.

## 구성

```
i15b105.p.ssafy.io/                교사용 웹 UI (정적, nginx)
i15b105.p.ssafy.io/learner/login   아동용 웹 UI (브라우저 시연용)
i15b105.p.ssafy.io/api/            백엔드 (nginx -> 127.0.0.1:8080)
i15b105.p.ssafy.io/download/       아이용 Electron 설치 파일
```

| 구성요소 | 배치 | 비고 |
|---|---|---|
| MySQL 8.4 / Redis 7.4 | 컨테이너 | 포트 미발행. 외부에서 접근 불가 |
| AI 서비스 | 컨테이너 | 외부 노출 없음. 백엔드만 내부 네트워크로 호출 |
| 백엔드 | 컨테이너 | `127.0.0.1:8080` 만 바인딩 |
| 교사 웹 UI | 정적 파일 | `/opt/iread/web/teacher/` |
| 아동 웹 UI | 정적 파일 | `/opt/iread/web/learner/` |
| Electron 설치 파일 | 정적 파일 | `/opt/iread/download/` (수동 업로드) |

서버 파일 배치

```
/opt/iread/docker-compose.prod.yml
/opt/iread/.env             시크릿. git 에 없다. 권한 600
/opt/iread/ai.env           AI 서비스 설정. git 에 없다. 권한 600
/opt/iread/lexicon/         story-lexicon.sqlite3 (수동 업로드)
/opt/iread/web/teacher/     교사 UI (워크플로가 rsync)
/opt/iread/web/learner/     아동 UI (워크플로가 rsync)
/opt/iread/download/        iRead-Setup.exe (수동 업로드)
/opt/iread/backups/         배포 전 DB 백업, 최근 10개 보관
```

### 아동용 UI 가 두 곳으로 나가는 이유

같은 저장소(`iRead-frontend-app`)가 두 경로로 배포된다.

- **`.exe` 안** — Tobii 시선추적을 쓰는 실제 학습 환경. `electron-builder.yml` 의
  `extraResources` 가 `dist` 를 설치본에 넣고 `app://` 프로토콜로 서빙한다.
  이 경로는 CI 에서 만들 수 없어 수동 빌드·업로드다.
- **`/learner/`** — 시선추적 장치 없이 브라우저로 시연할 때 쓴다. 아이트래커가 없으면
  `cursorGazeFallbackActive` 가 자동으로 켜져 마우스로 진행된다(env 설정과 무관하다).
  대신 실제 시선 데이터는 쌓이지 않는다.

**vite `base` 는 반드시 `/` 로 빌드해야 한다.** 라우터가 `createWebHistory(import.meta.env.BASE_URL)`
를 쓰는데 라우트가 이미 `/learner/...` 로 시작하므로, `base` 를 `/learner/` 로 주면 URL 이
`/learner/learner/login` 이 된다. 그 대가로 `/assets` 와 `/images` 를 교사 앱과 공유하는데
assets 는 해시 파일명이라 충돌이 없고, 파일명이 겹치는 이미지 3개는 아동 앱이 참조하지 않는다.
nginx 가 교사 디렉터리를 먼저 찾고 없으면 아동 디렉터리에서 찾는다.

## 배포 방법

1. `iRead-backend`, `iRead-ai`, `iRead-frontend-web` 각 저장소에서 `develop` → `main` 머지
2. `iRead-backend` → Actions → **Deploy** → Run workflow

실행 로그 맨 위에 세 저장소의 `main` 과 `develop` 격차가 표로 찍힌다.
한쪽만 머지했으면 여기서 바로 보인다. 격차가 있을 때 아예 중단시키려면
`strict_merge_check` 를 켜고 실행한다.

배포 순서는 **DB 백업 → AI → 백엔드 → 교사 UI** 로 고정돼 있다.
AI 를 먼저 올리는 이유는 그 상태(구 백엔드 + 신 AI)가 무해하기 때문이다.
반대 순서면 신 백엔드가 아직 없는 AI 엔드포인트를 호출해 500 이 난다.

## 롤백

`backend_tag` / `ai_tag` 에 이전 태그(`main-abc1234`)를 넣고 다시 실행한다.
재빌드가 없어 30초쯤 걸린다. 헬스체크가 실패하면 워크플로가 **자동으로** 이전 태그로 되돌린다.

## 처음 한 번만 해야 하는 준비

### 1. GitHub Secrets (`iRead-backend` 저장소)

`Settings → Secrets and variables → Actions → New repository secret`

| 이름 | 값 |
|---|---|
| `EC2_HOST` | `i15b105.p.ssafy.io` (스킴·끝 슬래시 없이) |
| `EC2_SSH_KEY` | `I15B105T.pem` 내용 전체. `-----BEGIN` ~ `-----END` 포함 26줄 |

사람이 발급할 토큰은 없다. ghcr push 와 EC2 의 pull 은 실행마다 자동 주입되는
`GITHUB_TOKEN` 으로 처리한다(워크플로의 `permissions: packages: write`).

`REPO_PAT` 은 선택이다. 체크아웃하는 네 저장소가 모두 공개라 기본 토큰으로 충분하다.
비공개로 전환하면 그때 추가하면 되고, 워크플로가 `${{ secrets.REPO_PAT || github.token }}`
형태라 넣기만 하면 자동으로 쓴다.

### 1-1. 버튼이 보이려면 `deploy.yml` 이 기본 브랜치에 있어야 한다

`workflow_dispatch` 는 워크플로 파일이 **기본 브랜치**에 있을 때만 실행 버튼이 나타난다.
네 저장소의 기본 브랜치는 `main` 이 아니라 **`develop`** 이므로 `deploy.yml` 을 `develop` 에
올려야 한다. 파일은 develop 에서 실행되고 빌드 대상은 `main` 을 체크아웃하므로 충돌은 없다.

### 1-2. 첫 실행 전에 `develop` → `main` 머지

워크플로는 `main` 을 배포한다. main 이 develop 보다 크게 뒤져 있으면 **지금 돌아가는
서비스가 옛 코드로 되돌아간다.** 첫 실행은 `strict_merge_check` 를 켜고 돌리면
머지 안 된 커밋이 있을 때 배포 전에 중단된다.

### 2. 서버 시크릿 (완료됨)

이미 서버에 설정되어 있다. 아래는 구조 설명이다.

| 파일 | 내용 | 읽는 쪽 |
|---|---|---|
| `/opt/iread/.env` | DB·Redis 비밀번호, JWT 시크릿, `AI_INTERNAL_API_KEY`, Typecast 키, 배포 태그 | compose 변수 치환 + 백엔드 |
| `/opt/iread/ai.env` | AI 서비스 전체 설정 (`iRead-ai/.env` 사본) | AI 컨테이너 |

DB·Redis 비밀번호와 JWT 시크릿, `AI_INTERNAL_API_KEY` 는 서버에서 임의 생성했다.
로컬 개발용 값과 다르므로 로컬 `.env` 를 서버에 덮어쓰지 말 것.

AI 설정은 항목을 compose 에 나열하지 않고 `ai.env` 를 통째로 넘긴다.
나열 방식은 새 설정이 생길 때 조용히 기본값으로 떨어지는데 그 기본값이 대개 mock 이다.
실제로 `AI_PRONUNCIATION_PROVIDER` 와 `AI_SPEECH_PROVIDER` 의 기본값이 `deterministic` 이고
여기엔 `APP_ENV=production` 가드가 걸려 있지 않다.

**주의: `.env` 를 Windows 에서 만들어 올릴 때는 반드시 줄바꿈을 LF 로 바꿀 것.**
CRLF 로 올리면 `\r` 이 값 뒤에 붙어 API 키가 조용히 인증 실패한다.

```bash
ssh -i I15B105T.pem ubuntu@i15b105.p.ssafy.io
sed -i 's/\r$//' /opt/iread/ai.env      # CRLF 로 올렸을 때
```

키 유효성은 AI 컨테이너 안에서 확인할 수 있다.

```bash
docker exec iread-ai python -c "
import os,urllib.request
r=os.environ['AZURE_SPEECH_REGION']
req=urllib.request.Request(f'https://{r}.api.cognitive.microsoft.com/sts/v1.0/issueToken',
  data=b'',method='POST',headers={'Ocp-Apim-Subscription-Key':os.environ['AZURE_SPEECH_KEY'],'Content-Length':'0'})
print('Azure', urllib.request.urlopen(req,timeout=15).status)"
```

`APP_ENV=production` 이라서 AI 서비스는 mock provider 나 기본 내부키로는 **기동을 거부**한다
(`iread_ai/config.py` 의 `validate_runtime`). 설정이 빠지면 조용히 가짜 데이터를 내보내는 대신
컨테이너가 뜨지 않으므로, 배포 실패로 즉시 드러난다.

### 3. lexicon DB 업로드 (선택)

없어도 나머지 기능은 정상이고 `/health` 도 `UP` 이지만 `lexiconStatus=UNAVAILABLE` 이 되어
이야기 어휘 팔레트 기능만 죽는다. 배포 스크립트가 이 상태를 로그에 경고로 찍는다.

```bash
scp -i I15B105T.pem story-lexicon.sqlite3 \
    ubuntu@i15b105.p.ssafy.io:/opt/iread/lexicon/
```

원본은 `python -m iread_ai.lexicon.install --source-db <원본>` 으로 만든다.
원본 DB 가 저장소에 없으므로 CI 에서는 생성할 수 없다.

### 4. Electron 설치 파일 업로드

CI 에서 빌드할 수 없다. Tobii 런타임(`tobii_gameintegration_x64`)이 라이선스 때문에
git 에 없고, `--win nsis` 라서 Windows 러너도 필요하다. 로컬 Windows 에서 만들어 올린다.

```bash
cd iRead-frontend-app && npm run build          # 학습자 UI 를 먼저 빌드
cd ../iRead-electron  && npm run dist           # release/iRead Setup x.y.z.exe

scp -i I15B105T.pem "release/iRead Setup 0.1.0.exe" \
    ubuntu@i15b105.p.ssafy.io:/opt/iread/download/iRead-Setup.exe
```

**파일명은 `iRead-Setup.exe` 로 고정한다.** 교사 UI 의 다운로드 링크가 이 이름을 가리키므로
버전이 올라가도 링크를 고칠 필요가 없다.

## 시연용으로 완화한 보안 설정

`SecurityConfig` 에서 `GET /uploads/images/**` 을 `permitAll` 로 열어 두었다.

`<img>` 태그는 `Authorization` 헤더를 보낼 수 없고 `JwtAuthenticationFilter` 는 헤더만 읽으므로
(쿠키에서 토큰을 읽지 않는다) 교사가 업로드한 아동 프로필 사진이 401 로 깨지기 때문이다.

- 조회(GET)만 열려 있고 업로드 경로는 그대로 인증이 필요하다.
- `/gaze/**` 는 열지 않았다.
- **URL 을 아는 사람은 누구나 업로드된 이미지를 볼 수 있다.** 시연 데이터 전제의 설정이므로
  실제 아동 사진을 다루게 되면 되돌려야 한다.

근본 해결은 둘 중 하나다. `JwtAuthenticationFilter` 가 쿠키에서도 토큰을 읽게 하거나,
프론트가 `fetch` + `Authorization` 헤더로 받아 blob URL 로 그리는 것이다. 후자는 백엔드 변경이 없다.

## 서버에서 하면 안 되는 것

SSAFY 안내의 위험 명령 외에 이 구성에서 특히 주의할 것.

- **`sudo iptables` 금지.** Docker 가 자기 규칙을 관리한다. 손대면 22번 포트가 닫혀 접속 불능이 될 수 있다.
- **Gerrit / Apache 를 건드리지 말 것.** 8988·8989·29418 을 쓰는 별개 서비스가 돌고 있다. nginx 는 80·443 만 쓴다.
- **compose 에 포트를 추가하지 말 것.** Docker 는 ufw 를 우회하므로 `ports:` 에 적으면 `ufw status` 에 안 보여도 인터넷에 열린다. MySQL·Redis 는 절대 발행하지 않는다.
- **EC2 에서 gradle 로 빌드하지 말 것.** t2.xlarge 는 버스트 인스턴스라 지속 성능이 0.9 vCPU 다. 빌드는 GitHub 러너에서 한다.

## 자주 쓰는 확인 명령

```bash
cd /opt/iread
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f --tail 100 backend
docker compose -f docker-compose.prod.yml exec -T mysql \
    mysql -u root -p"$(grep ^MYSQL_ROOT_PASSWORD= .env | cut -d= -f2-)" -e "show databases"

# 메일 확인 (mailpit UI 는 루프백 전용이라 터널이 필요하다)
ssh -i I15B105T.pem -L 8025:127.0.0.1:8025 ubuntu@i15b105.p.ssafy.io
# 그 다음 브라우저에서 http://127.0.0.1:8025
```
