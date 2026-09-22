# ---- build stage -------------------------------------------------------------
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between source changes.
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle --no-daemon dependencies --configuration runtimeClasspath > /dev/null || true

COPY src ./src
RUN gradle --no-daemon bootJar -x test

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl is only used by the compose healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
