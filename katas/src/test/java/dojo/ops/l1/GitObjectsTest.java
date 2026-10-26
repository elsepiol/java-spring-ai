package dojo.ops.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ops")
@Tag("L1")
class GitObjectsTest {

	@Test
	void matchesGitHashObject() {
		// printf '' | git hash-object --stdin ; echo 'hello world' | git hash-object --stdin
		assertThat(GitObjects.blobId(new byte[0])).isEqualTo("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
		assertThat(GitObjects.blobId("hello world\n".getBytes(StandardCharsets.UTF_8)))
			.isEqualTo("3b18e512dba79e4c8300dd08aeb37f8e728b8dad");
	}

	@Test
	void countsBytesNotCharacters() {
		// printf 'é' | git hash-object --stdin   (2 bytes in UTF-8)
		assertThat(GitObjects.blobId("é".getBytes(StandardCharsets.UTF_8))).isEqualTo("4b04fff51468d8ab5201ab02b725dc477bc7cb45");
	}

	@Test
	void sameContentSameId() {
		byte[] a = "x".getBytes(StandardCharsets.UTF_8);
		assertThat(GitObjects.blobId(a)).isEqualTo(GitObjects.blobId("x".getBytes(StandardCharsets.UTF_8)));
		assertThat(GitObjects.blobId(a)).isNotEqualTo(GitObjects.blobId("y".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void abbreviatesToTheShortestUniquePrefix() {
		List<String> ids = List.of(
				"3b18e512dba79e4c8300dd08aeb37f8e728b8dad",
				"3b18e7aa00000000000000000000000000000000",
				"e69de29bb2d1d6434b8b29ae775ad8c2e48c5391");
		assertThat(GitObjects.shortId(ids.get(2), ids)).isEqualTo("e69d");
		assertThat(GitObjects.shortId(ids.get(0), ids)).isEqualTo("3b18e5");
		assertThat(GitObjects.shortId(ids.get(1), ids)).isEqualTo("3b18e7");
	}

}
