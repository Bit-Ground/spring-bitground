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

ENV PINPOINT_AGENT_HOME=/usr/local/pinpoint-agent
# 로컬에서 다운로드한 Pinpoint Agent 디렉터리를 이미지로 복사
# Dockerfile과 같은 디렉터리에 'pinpoint-agent' 폴더가 있다고 가정합니다.
# 이 폴더 안에 'pinpoint-bootstrap-YOUR_PINPOINT_VERSION.jar' 파일이 있어야 합니다.
COPY pinpoint-agent /usr/local/pinpoint-agent

# builder 단계에서 만들어진 JAR 복사
ARG JAR_FILE=build/libs/*.jar
COPY --from=builder /app/${JAR_FILE} app.jar

# (선택) 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8090/actuator/health || exit 1

EXPOSE 8090

# 애플리케이션 실행 명령어
# -javaagent JVM 인자를 사용하여 Pinpoint Agent 연결
# -Dpinpoint.applicationname과 -Dpinpoint.agentid는 반드시 설정해야 합니다.
ENTRYPOINT ["sh","-c", "\
  java \
    -javaagent:${PINPOINT_AGENT_HOME}/pinpoint-bootstrap-2.3.3-NCP-RC3.jar \
    -Dpinpoint.applicationName=bitground-spring \
    -Dpinpoint.agentId=bitground-spring \
    -jar app.jar \
"]