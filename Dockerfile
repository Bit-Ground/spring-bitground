# 1) Build stage: Gradle Wrapper 이용
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 캐시 활용을 위해 먼저 wrapper와 설정만 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
# Kotlin DSL
COPY settings.gradle.kts settings.gradle.kts
# 만약 Groovy DSL 사용 중이면 build.gradle / settings.gradle 로 변경

# 소스 복사 후 빌드
COPY src src
RUN chmod +x gradlew \
 && ./gradlew clean bootJar -x test --no-daemon

# 2) Run stage: JRE만 포함
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 아티팩트 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]