# 런타임 전용 이미지. jar는 먼저 `./gradlew bootJar` 로 만들어 둔다.
FROM eclipse-temurin:21-jre

# 헬스체크와 장애 조사에 쓴다.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# application.properties가 uploads/images, audio, gaze를 상대 경로로 쓰므로
# WORKDIR 기준으로 미리 만들어 두고 볼륨을 여기에 마운트한다.
RUN useradd --create-home --uid 10001 iread \
    && mkdir -p /app/uploads/images /app/audio /app/gaze \
    && chown -R iread:iread /app

# *-SNAPSHOT-plain.jar 은 이 패턴에 걸리지 않으므로 부트 jar만 복사된다.
COPY --chown=iread:iread build/libs/*-SNAPSHOT.jar /app/app.jar

USER iread
EXPOSE 8080

# Flyway 마이그레이션 때문에 첫 기동이 길어질 수 있어 start-period를 넉넉히 준다.
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=12 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=60", \
    "-Duser.timezone=Asia/Seoul", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "/app/app.jar"]
