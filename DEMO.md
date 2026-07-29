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

### Docker Compose

`.env.example`을 `.env`로 복사하고 `AUTH_JWT_SECRET`에 32바이트 이상의
로컬 전용 임의 문자열을 지정한 뒤 실행한다.

```powershell
Copy-Item .env.example .env
docker compose --profile demo up -d --build
docker compose --profile demo ps
```

Docker에서는 `iread` 데이터베이스에 Flyway demo seed가 자동 적용된다.

### Spring 로컬 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

데모 교수자 계정은 `demo@iread.local` / `demo1234`이고, 연결된 비식별 아동 이름은 `샛별`이다.
`demo` 프로필은 Story·훈련 생성, 훈련 평가, STT와 TTS를 외부 AI 서버 없이 재현한다.

## 초기화

데모 데이터베이스를 비운 뒤 다시 실행하면 Flyway V1과 demo V2 seed가 같은 상태를 만든다.
업로드된 음성까지 초기화할 때는 Backend가 종료된 상태에서 저장소의 `audio/` 디렉터리만 삭제한다.

Docker의 데모 데이터와 업로드 파일을 모두 초기화할 때만 다음 명령을 사용한다.
이 명령은 Compose volume을 삭제하므로 저장된 로컬 데모 데이터는 복구되지 않는다.

```powershell
docker compose --profile demo down -v
```

실제 사용자 데이터나 연구·분석용 음성은 이 데모 데이터베이스에 넣지 않는다.
