# iRead Backend

iRead 서비스의 백엔드 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.7
- Gradle Groovy DSL
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- MyBatis
- MySQL 8.4
- Redis 7.4
- Lombok

## Docker Compose 사용법

Docker Compose는 로컬 개발용 MySQL, Redis 컨테이너를 실행합니다.

실행 전에 프로젝트 루트에 `.env` 파일을 생성합니다. 필요한 값은 `.env.example`을 참고합니다.

### 실행

```bash
docker compose up -d
```

실행되는 컨테이너:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`

데이터베이스 이름과 계정 정보는 커밋하지 않는 `.env` 또는 로컬 환경 설정에서 관리합니다.

### 상태 확인

```bash
docker compose ps
```

### 로그 확인

```bash
docker compose logs -f
```

특정 서비스만 확인하려면:

```bash
docker compose logs -f mysql
docker compose logs -f redis
```

### 중지

```bash
docker compose down
```

### 데이터까지 삭제

MySQL, Redis 볼륨 데이터까지 삭제하려면 아래 명령을 사용합니다.

```bash
docker compose down -v
```

## 애플리케이션 실행

로컬 설정은 `src/main/resources/application-local.properties`에 작성합니다.
이 파일은 `application.properties`에서 optional로 import됩니다.

예시:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/iread?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=<DB_USERNAME>
spring.datasource.password=<DB_PASSWORD>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

애플리케이션 실행:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

## 테스트

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```
