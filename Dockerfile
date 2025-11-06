# 1. JDK 17 기반
FROM openjdk:17-jdk-slim

# 2. 컨테이너 내부 작업 디렉토리
WORKDIR /app

# 3. jar 복사
COPY build/libs/geulda-app-0.0.1-SNAPSHOT.jar app.jar

# 4. 컨테이너가 사용할 포트
EXPOSE 8080

# 5. 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]