FROM eclipse-temurin:17-jdk AS builder

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

FROM eclipse-temurin:17-jre

WORKDIR /app

# 타임존 설정 (한국 시간)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]