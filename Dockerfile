FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system iread \
    && useradd --system --gid iread --home-dir /app --create-home iread

WORKDIR /app

COPY --from=builder --chown=iread:iread /workspace/build/libs/*.jar app.jar

USER iread

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
