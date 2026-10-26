# syntax=docker/dockerfile:1
# A production Dockerfile for a Spring Boot app, annotated. Build: docker build -t shop:1.0 .
# (The syntax line must stay the very first line: it selects the BuildKit frontend.)

# ---- Stage 1: build. Has Maven and a JDK; thrown away afterwards.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Dependencies first: this layer is reused until pom.xml changes (L2 section 1).
COPY pom.xml .
# A BuildKit cache mount keeps ~/.m2 between builds without putting it in a layer.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# ---- Stage 2: run. Only a JRE, the app, and a non-root user.
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
WORKDIR /app
# Spring Boot's layers, least to most frequently changing: a code change ships one small layer.
COPY --from=build /src/target/extracted/dependencies/ ./
COPY --from=build /src/target/extracted/spring-boot-loader/ ./
COPY --from=build /src/target/extracted/snapshot-dependencies/ ./
COPY --from=build /src/target/extracted/application/ ./
USER app
EXPOSE 8080
# Size the heap from the container's memory limit, not the host's (L3 section 1).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Exec form: java is PID 1 and receives SIGTERM for graceful shutdown.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
