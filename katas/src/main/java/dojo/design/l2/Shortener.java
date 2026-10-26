package dojo.design.l2;

import java.util.Optional;

import dojo.Kata;

/**
 * KATA design L2 "Shortener" (tracks/15-system-design/L2-practical.md, section 1).
 *
 * The core of a URL shortener (the most common design interview warm-up):
 * - codes are the Base62 encoding (0-9, a-z, A-Z, in that order, most significant digit first)
 *   of a numeric id from a counter starting at 1; encode(0) = "0", encode(61) = "Z",
 *   encode(62) = "10"; decode is its inverse (IllegalArgumentException for other characters);
 * - shorten(url) is idempotent: the same URL always gets the same code (no duplicate rows when
 *   a client retries);
 * - shorten(url, alias) uses a custom alias (3 to 30 characters of [A-Za-z0-9-]); an alias
 *   taken by ANOTHER url -> AliasTakenException; aliases must never collide with generated codes,
 *   so generated codes skip any value already used as an alias;
 * - only http and https URLs are accepted (IllegalArgumentException), or the service becomes a
 *   javascript: / data: phishing tool;
 * - resolve(code) -> the URL, or empty.
 */
public final class Shortener {

	public static final class AliasTakenException extends RuntimeException {

		public AliasTakenException(String alias) {
			super("alias taken: " + alias);
		}

	}

	public static String encode(long id) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 1");
	}

	public static long decode(String code) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 1");
	}

	public String shorten(String url) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 1");
	}

	public String shorten(String url, String alias) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 1");
	}

	public Optional<String> resolve(String code) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 1");
	}

}
