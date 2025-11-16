FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle 래퍼와 빌드 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY test.gradle .

# 소스 코드 복사
COPY src src

# 빌드 실행 (테스트 제외)
RUN chmod +x ./gradlew && \
    ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 설정 (한국 시간)
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 보안: non-root 사용자 생성 및 로그 디렉토리 생성
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*SNAPSHOT.jar app.jar

# non-root 사용자로 전환
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms128m", \
    "-Xmx512m", \
    "-XX:MaxMetaspaceSize=96m", \
    "-XX:ReservedCodeCacheSize=64m", \
    "-XX:+UseSerialGC", \
    "-XX:MinHeapFreeRatio=20", \
    "-XX:MaxHeapFreeRatio=40", \
    "-XX:GCTimeRatio=4", \
    "-XX:AdaptiveSizePolicyWeight=90", \
    "-Xss256k", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.backgroundpreinitializer.ignore=true", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]