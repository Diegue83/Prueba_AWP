
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app

EXPOSE 10000

CMD ["java", "-jar", "app.jar"]
