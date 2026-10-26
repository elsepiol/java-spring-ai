package dojo.build.l3;

import dojo.Kata;
import java.util.List;
import java.util.Map;

/**
 * KATA build L3 "LockFile" (tracks/03-build-tools/L3-production.md, section 5: supply chain).
 *
 * A build should use exactly the dependency files that were reviewed, byte for byte. A lock file
 * lists each artifact with its SHA-256 checksum (like Gradle's verification-metadata.xml or npm's
 * package-lock.json "integrity"). If a repository, a proxy or an attacker swaps a jar, the
 * checksum no longer matches and the build must stop.
 *
 * Lock file format, one artifact per line: "<path> sha256:<64 lowercase hex chars>"; blank lines
 * and lines starting with # are ignored.
 *
 * verify(artifacts, lockFile) returns the problems, sorted:
 *   "MISMATCH <path>"    the file's SHA-256 differs from the lock file
 *   "MISSING <path>"     in the lock file but not among the artifacts
 *   "UNEXPECTED <path>"  among the artifacts but not in the lock file (a new, unreviewed dependency)
 * An empty list means the build may proceed. A malformed line is an IllegalArgumentException.
 * Use java.security.MessageDigest and java.util.HexFormat.
 */
public final class LockFile {

	private LockFile() {
	}

	public static List<String> verify(Map<String, byte[]> artifacts, String lockFile) {
		return Kata.todo("tracks/03-build-tools/L3-production.md, section 5");
	}

	public static String sha256(byte[] content) {
		return Kata.todo("tracks/03-build-tools/L3-production.md, section 5");
	}

}
