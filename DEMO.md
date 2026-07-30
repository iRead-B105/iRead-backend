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
`demo` 프로필로 Backend를 재시작하면 세 데모 학습자의 현재 훈련 진행 상태와 음성·시선
시도 데이터가 초기화되며, 각 커리큘럼의 첫 훈련부터 다시 테스트할 수 있다.
교수자 App 시연용 학습 이력과 분석 데이터는 초기화 대상에 포함하지 않는다.

학습자 App에서 실력도전을 처음 열면 기존 데모 검사 커리큘럼을 분류별 3문항, 총 9문항으로
보충한다. 각 문항의 훈련 템플릿은 분류 안에서 한 번만 무작위 선택하고, Mock AI가 생성한
문항을 `test_datas`에 저장하므로 새로 고치거나 재접속해도 다시 추첨하지 않는다.

## 초기화

데모 데이터베이스를 비운 뒤 다시 실행하면 Flyway의 `V1__baseline_schema.sql`과
`V2__demo_seed.sql`이 같은 상태를 만든다. 데모 마이그레이션 통합 전에 `V3`부터 `V6`까지
적용한 이력이 있는 데모 데이터베이스는 Flyway 이력 정합성을 위해 한 번 재생성해야 한다.
업로드된 음성까지 초기화할 때는 Backend가 종료된 상태에서 저장소의 `audio/` 디렉터리만 삭제한다.

실제 사용자 데이터나 연구·분석용 음성은 이 데모 데이터베이스에 넣지 않는다.
