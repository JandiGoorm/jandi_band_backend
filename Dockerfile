# 1. 빌드(Build) 스테이지: 소스 코드를 JAR 파일로 빌드
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace/app

# Gradle 의존성을 먼저 받아 별도의 레이어로 캐싱합니다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew build -x test --no-daemon

# 소스 코드를 복사합니다.
COPY src src

# 다시 빌드하여 최종 JAR 파일을 생성합니다.
RUN ./gradlew bootJar --no-daemon

# -----------------------------------------------------

# 2. 실행(Final) 스테이지: 실제 운영 환경에서 사용될 이미지
# GraalVM JDK를 사용하여 성능 최적화 및 향후 Native Image 전환 준비
FROM ghcr.io/graalvm/jdk-community:21
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
# JAR 파일 이름이 다를 경우, `band_backend-0.0.1-SNAPSHOT.jar` 부분을 실제 파일 이름으로 수정하세요.
COPY --from=builder /workspace/app/build/libs/band_backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# 컨테이너 실행 시, 외부(/app/config/application.properties)에 있는 설정 파일을 사용하도록 지정
# 이 경로는 EC2 서버의 docker-compose.yml에 설정한 volumes 경로와 반드시 일치해야 합니다.
ENTRYPOINT ["java", "-Dspring.config.location=file:/app/config/application.properties", "-jar", "app.jar"]
