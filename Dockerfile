FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 1) wrapper 및 설정 복사
COPY gradlew .
COPY gradle gradle

# 2) 실제 Gradle 스크립트 복사 (Groovy DSL)
COPY build.gradle settings.gradle ./

# 3) 소스 복사 및 빌드
COPY src src
RUN chmod +x gradlew \
 && ./gradlew clean bootJar -x test --no-daemon

# 런타임 스테이지 (이전과 동일)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
