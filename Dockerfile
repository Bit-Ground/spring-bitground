# 1) Build 단계: Gradle Wrapper 기반
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# gradlew, gradle 설정 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle/

# 실제 빌드 스크립트 복사
# Groovy DSL 쓰신다면 build.gradle/settings.gradle,
# Kotlin DSL 쓰신다면 .kts 파일 이름으로 바꿔주세요.
COPY build.gradle settings.gradle ./

# 소스 코드 복사
COPY src src

# 권한 부여 후 빌드
RUN chmod +x gradlew \
 && ./gradlew clean bootJar -x test --no-daemon

# 2) Run 단계: JRE만 포함해 경량화
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# builder 단계에서 만들어진 JAR 복사
ARG JAR_FILE=build/libs/*.jar
COPY --from=builder /app/${JAR_FILE} app.jar

# (선택) 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8090/actuator/health || exit 1

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
