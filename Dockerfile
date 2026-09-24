# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Сначала только pom.xml: слой с зависимостями кешируется, пока pom не меняется
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
ARG SKIP_TESTS=false
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests=${SKIP_TESTS}

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S bot && adduser -S -G bot bot
WORKDIR /app
COPY --from=build /build/target/outfix-market-bot.jar app.jar
USER bot
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    METRICS_PORT=8081
# /metrics для Prometheus и /health для проверки ниже
EXPOSE 8081
# start-period покрывает проверку токена и миграции БД при старте
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${METRICS_PORT}/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
