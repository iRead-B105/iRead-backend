# Backend 데모 실행

데모 프로필은 실제 사용자가 아닌 고정된 시연 데이터와 결정적 AI fixture를 사용한다.

## 준비

1. 빈 MySQL 데이터베이스 `iread_demo`를 만든다.
2. `src/main/resources/application-local.properties`에 로컬 접속 정보를 둔다.

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/iread_demo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=<local-user>
spring.datasource.password=<local-password>
auth.jwt.secret=<32-byte-or-longer-local-secret>
```

## 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

데모 교수자 계정은 `demo@iread.local` / `demo1234`이고, 학습자 App 시연용 비식별 아동은
`샛별`, `한결`, `박서아`이다.
`demo` 프로필은 Story·훈련 생성, 훈련 평가, STT와 TTS를 외부 AI 서버 없이 재현한다.

`한결`과 `박서아`의 일일 커리큘럼은 마이크 없이 완료할 수 있는 훈련 5개로 구성한다.
`샛별`의 오늘의 커리큘럼은 프론트 전체 훈련 점검용으로 34개 훈련 템플릿을 한 번씩
포함하며, 각 훈련에는 Mock AI가 생성한 문항이 정확히 1개씩 저장된다.
`샛별` 커리큘럼에는 `그림과 문장 연결하기`도 포함되며, 저장된 `imagePrompt`를 Mock AI가
이미지 URL로 변환하므로 학습자 App에서 이미지 선택 화면까지 확인할 수 있다.
`샛별`의 이야기는 완성된 대사를 seed에 저장하지 않는다. 이야기 템플릿을 선택하면 Mock AI가
1~5번째 대사를 생성하고 5번째 대사만 음성 분기 입력을 요구한다. 분기 음성을 제출하면
Mock STT의 선택 내용을 반영한 6~10번째 대사를 생성하고 이야기를 완료한다.
`demo` 프로필로 Backend를 재시작하면 세 데모 학습자의 현재 훈련 진행 상태와 음성·시선
시도 데이터가 초기화되며, 각 커리큘럼의 첫 훈련부터 다시 테스트할 수 있다.
교수자 App 시연용 학습 이력과 분석 데이터는 초기화 대상에 포함하지 않는다.

학습자 App에서 실력도전을 처음 열면 기존 데모 검사 커리큘럼을 분류별 3문항, 총 9문항으로
보충한다. 각 문항의 훈련 템플릿은 분류 안에서 한 번만 무작위 선택하고, Mock AI가 생성한
문항을 `test_datas`에 저장하므로 새로 고치거나 재접속해도 다시 추첨하지 않는다.

## 초기화

데모 데이터베이스를 비운 뒤 다시 실행하면 Flyway의 `V1__baseline_schema.sql`과
`V2__demo_seed.sql`이 같은 상태를 만든다. 기존 데모 migration `V3`부터 `V10`까지의 최종
데이터 상태는 `V2`에 통합했고, 기존 `V11`의 시선 분석 상세 컬럼은 빈 DB 기준선인 `V1`에
반영했다. 데모 데이터만 조정하는 동안에는 `V2`를 갱신하고 빈 DB를 다시 생성한다.

다음 신규 migration 버전은 `V3`부터 순차적으로 사용한다. 다음 조건 중 하나라도 해당하면
이미 적용된 `V1`이나 `V2`를 덮어쓰지 않고 새 버전을 추가한다.

- 공유·보존 DB에서 기존 데이터를 변환해야 하는 변경
- 테이블·컬럼·제약조건 등 스키마 변경
- 배포 또는 릴리스 이후 기존 Flyway 이력을 유지해야 하는 변경

기존 `V3`부터 `V11`까지 적용한 데모 DB는 기준선 체크섬과 migration 이력이 달라지므로 한 번
삭제 후 재생성해야 한다. 해당 이력을 `repair`로 강제로 맞추거나 운영 데이터베이스에 통합
기준선을 덮어쓰지 않는다.

업로드된 음성까지 초기화할 때는 Backend가 종료된 상태에서 저장소의 `audio/` 디렉터리만 삭제한다.

실제 사용자 데이터나 연구·분석용 음성은 이 데모 데이터베이스에 넣지 않는다.
