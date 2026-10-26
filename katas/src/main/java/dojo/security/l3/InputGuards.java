package dojo.security.l3;

import java.nio.file.Path;
import java.util.Set;

import dojo.Kata;

/**
 * KATA security L3 "InputGuards" (tracks/09-security/L3-production.md, section 2).
 *
 * Three classic injection-style holes, each closed in one method:
 *
 * escapeHtml (XSS): make untrusted text safe to place inside HTML text or a quoted attribute:
 * replace & < > " ' with &amp; &lt; &gt; &quot; &#39; (the & first, or you double-escape). null -> "".
 *
 * resolveUpload (path traversal): turn a user-supplied relative file name into a path INSIDE baseDir,
 * or throw IllegalArgumentException. Reject: absolute paths, anything that normalises to outside
 * baseDir ("../../etc/passwd", "a/../../x"), the base directory itself, and NUL characters. Decide
 * on the normalised path (Path.normalize, startsWith on Path, not on String: "/data/up" is a string
 * prefix of "/data/uploads-old").
 *
 * isSafeRedirect (open redirect): allow "?next=" targets that are either
 * - a local path: starts with exactly one "/" (not "//evil.com", which browsers treat as a host,
 *   and not "/\evil.com"), or
 * - an absolute https URL whose host is one of {@code allowedHosts} (exact match, case-insensitive:
 *   "shop.example.com.evil.net" and "evil.net/?shop.example.com" are not allowed).
 * Anything else (http:, javascript:, data:, unparsable, null, control characters) is unsafe.
 */
public final class InputGuards {

	private final Set<String> allowedHosts;

	public InputGuards(Set<String> allowedHosts) {
		this.allowedHosts = Set.copyOf(allowedHosts);
	}

	public static String escapeHtml(String text) {
		return Kata.todo("tracks/09-security/L3-production.md section 2");
	}

	public static Path resolveUpload(Path baseDir, String fileName) {
		return Kata.todo("tracks/09-security/L3-production.md section 2");
	}

	public boolean isSafeRedirect(String target) {
		return Kata.todo("tracks/09-security/L3-production.md section 2");
	}

}
