FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle/
COPY build.gradle settings.gradle ./
COPY src src/
COPY src/main/resources src/main/resources

RUN chmod +x gradlew \
 && ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/BitGroundSpring-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]