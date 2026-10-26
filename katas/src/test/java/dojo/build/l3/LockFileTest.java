package dojo.build.l3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L3")
class LockFileTest {

	private static final byte[] GUAVA = "pretend this is guava-33.4.jar".getBytes(UTF_8);
	private static final byte[] SLF4J = "pretend this is slf4j-api-2.0.17.jar".getBytes(UTF_8);

	private static String lock() {
		return """
				# reviewed 2026-10-01
				com/google/guava/guava/33.4/guava-33.4.jar sha256:%s

				org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar sha256:%s
				""".formatted(LockFile.sha256(GUAVA), LockFile.sha256(SLF4J));
	}

	@Test
	void computesSha256AsLowercaseHex() {
		assertThat(LockFile.sha256("abc".getBytes(UTF_8))).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void theReviewedFilesPass() {
		Map<String, byte[]> artifacts = Map.of(
				"com/google/guava/guava/33.4/guava-33.4.jar", GUAVA,
				"org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar", SLF4J);
		assertThat(LockFile.verify(artifacts, lock())).isEmpty();
	}

	@Test
	void aSwappedJarIsCaught() {
		Map<String, byte[]> artifacts = Map.of(
				"com/google/guava/guava/33.4/guava-33.4.jar", "guava with a backdoor".getBytes(UTF_8),
				"org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar", SLF4J);
		assertThat(LockFile.verify(artifacts, lock())).containsExactly("MISMATCH com/google/guava/guava/33.4/guava-33.4.jar");
	}

	@Test
	void missingAndUnexpectedFilesAreReported() {
		Map<String, byte[]> artifacts = Map.of(
				"org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar", SLF4J,
				"evil/left-pad/1.0/left-pad-1.0.jar", "new".getBytes(UTF_8));
		assertThat(LockFile.verify(artifacts, lock())).containsExactly(
				"MISSING com/google/guava/guava/33.4/guava-33.4.jar",
				"UNEXPECTED evil/left-pad/1.0/left-pad-1.0.jar");
	}

	@Test
	void aMalformedLockFileIsRejected() {
		assertThatThrownBy(() -> LockFile.verify(Map.of(), "guava.jar md5:1234")).isInstanceOf(IllegalArgumentException.class);
	}

}
