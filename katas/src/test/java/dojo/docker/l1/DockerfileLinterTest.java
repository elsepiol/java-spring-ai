package dojo.docker.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.docker.l1.DockerfileLinter.Finding;

@Tag("docker")
@Tag("L1")
class DockerfileLinterTest {

	private final DockerfileLinter linter = new DockerfileLinter();

	@Test
	void aGoodMultiStageDockerfileIsClean() {
		String dockerfile = """
				# build stage: may run as root, it is thrown away
				FROM maven:3.9-eclipse-temurin-21 AS build
				WORKDIR /src
				COPY pom.xml .
				RUN mvn -B dependency:go-offline
				COPY src ./src
				RUN mvn -B package -DskipTests

				FROM eclipse-temurin:21-jre@sha256:4f6c0b1d2e3a4f5b6c7d8e9f0a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d
				RUN apt-get update && apt-get install -y --no-install-recommends curl \\
				    && rm -rf /var/lib/apt/lists/*
				RUN useradd --system --uid 10001 app
				COPY --from=build /src/target/app.jar /app/app.jar
				USER app
				ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
				ENTRYPOINT ["java", "-jar", "/app/app.jar"]
				""";
		assertThat(linter.lint(dockerfile)).isEmpty();
	}

	@Test
	void flagsUnpinnedBaseImages() {
		assertThat(linter.lint("FROM openjdk\nUSER app\n")).containsExactly(new Finding(1, "pin-base-image"));
		assertThat(linter.lint("FROM node:latest\nUSER app\n")).containsExactly(new Finding(1, "pin-base-image"));
		assertThat(linter.lint("FROM localhost:5000/base\nUSER app\n")).containsExactly(new Finding(1, "pin-base-image"));
		assertThat(linter.lint("FROM localhost:5000/base:1.2\nUSER app\n")).isEmpty();
		assertThat(linter.lint("FROM scratch\nUSER 10001\n")).isEmpty();
	}

	@Test
	void earlierStagesCanBeUsedByName() {
		String dockerfile = """
				FROM golang:1.23 AS Builder
				FROM builder
				USER app
				""";
		assertThat(linter.lint(dockerfile)).isEmpty();
	}

	@Test
	void theFinalStageMustNotRunAsRoot() {
		assertThat(linter.lint("FROM alpine:3.20\nRUN echo hi\n")).containsExactly(new Finding(1, "non-root-user"));
		assertThat(linter.lint("FROM alpine:3.20\nUSER app\nUSER root\n")).containsExactly(new Finding(3, "non-root-user"));
		assertThat(linter.lint("FROM alpine:3.20\nUSER 0:0\n")).containsExactly(new Finding(2, "non-root-user"));
		String userOnlyInBuildStage = """
				FROM alpine:3.20 AS build
				USER builder
				FROM alpine:3.20
				""";
		assertThat(linter.lint(userOnlyInBuildStage)).containsExactly(new Finding(3, "non-root-user"));
	}

	@Test
	void flagsAddSecretsAndAptLists() {
		String dockerfile = """
				FROM debian:12
				add app.tar.gz /opt/
				ENV DB_PASSWORD=hunter2 LOG_LEVEL=info
				ARG npm_token
				ENV LOG_LEVEL debug
				RUN apt-get update && \\
				    apt-get install -y curl
				USER app
				""";
		assertThat(linter.lint(dockerfile)).containsExactly(
				new Finding(2, "copy-not-add"),
				new Finding(3, "secret-in-env"),
				new Finding(4, "secret-in-env"),
				new Finding(6, "apt-lists"));
	}

	@Test
	void flagsShellFormEntrypoints() {
		String dockerfile = """
				FROM eclipse-temurin:21-jre
				USER app
				CMD java -jar /app.jar
				ENTRYPOINT ["java", "-jar", "/app.jar"]
				""";
		assertThat(linter.lint(dockerfile)).containsExactly(new Finding(3, "exec-form"));
	}

	@Test
	void findingsAreSortedByLineThenRule() {
		List<Finding> findings = linter.lint("FROM ubuntu\nADD x /x\nENTRYPOINT run.sh\n");
		assertThat(findings).containsExactly(
				new Finding(1, "non-root-user"),
				new Finding(1, "pin-base-image"),
				new Finding(2, "copy-not-add"),
				new Finding(3, "exec-form"));
	}

}
