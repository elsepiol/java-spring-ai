package dojo.docker.l1;

import java.util.List;

import dojo.Kata;

/**
 * KATA docker L1 "DockerfileLinter" (tracks/11-docker-deploy/L1-basics.md, section 4).
 *
 * Review a Dockerfile like a careful colleague (or hadolint). Return the findings sorted by line,
 * then rule. Instructions are case-insensitive; "#" lines are comments; a line ending in "\" continues
 * on the next one (the finding's line is the instruction's FIRST line, counting from 1).
 *
 * Rules:
 * - "pin-base-image": FROM an image with no tag or with ":latest". Exceptions: "scratch", and a
 *   FROM naming an earlier build stage ("FROM build" after "FROM maven:3.9 AS build"). A digest
 *   (@sha256:...) counts as pinned.
 * - "non-root-user": the FINAL stage never switches to a non-root user: report it on the final
 *   FROM line when it has no USER instruction, or on the USER line when that is "root" or "0".
 *   (Earlier build stages may run as root: they are thrown away.)
 * - "copy-not-add": any ADD (COPY is explicit; ADD also downloads URLs and unpacks archives).
 * - "secret-in-env": ENV or ARG setting a variable whose name contains PASSWORD, SECRET, TOKEN or
 *   API_KEY (any case): it is baked into the image and visible in `docker history`.
 * - "apt-lists": a RUN with "apt-get install" that does not also remove /var/lib/apt/lists in the
 *   same RUN (the layer keeps the package index forever).
 * - "exec-form": ENTRYPOINT or CMD in shell form (not a JSON array): the shell becomes PID 1 and
 *   the JVM never receives SIGTERM.
 */
public final class DockerfileLinter {

	public record Finding(int line, String rule) {
	}

	public List<Finding> lint(String dockerfile) {
		return Kata.todo("tracks/11-docker-deploy/L1-basics.md section 4");
	}

}
