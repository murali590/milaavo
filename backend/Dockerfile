# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle wrapper and build files first. This layer (and the dependency download)
# stays cached until build.gradle changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

# Source code last, because it changes most often.
COPY src src
# Tests run in CI, not in every image build.
RUN ./gradlew --no-daemon bootJar -x test

# ---------- Stage 2: the image we actually run ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a normal user, not root.
RUN useradd --system --uid 1001 milaavo
USER milaavo

COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8091

# The JVM reads this variable by itself. Use 75% of the container's memory limit for the heap.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Exec form (a JSON array): java is PID 1 and gets the stop signal for a graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]