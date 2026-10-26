package dojo.ops.l1;

import java.util.Collection;

import dojo.Kata;

/**
 * KATA ops L1 "GitObjects" (tracks/14-git-linux-networking/L1-basics.md, section 2).
 *
 * Git is a content-addressed store: every object's id is the SHA-1 of a header plus its content.
 * - blobId: for a file's bytes, SHA-1 of ("blob " + content length in BYTES + "\0" + content), as
 *   40 lowercase hex characters. Must equal `git hash-object <file>`.
 * - shortId: the shortest prefix (at least 4 characters, like `git log --oneline` with
 *   core.abbrev) that identifies {@code id} uniquely among {@code allIds}; {@code id} is in allIds.
 */
public final class GitObjects {

	private GitObjects() {
	}

	public static String blobId(byte[] content) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 2");
	}

	public static String shortId(String id, Collection<String> allIds) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 2");
	}

}
