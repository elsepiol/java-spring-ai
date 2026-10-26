package dojo.security.l1;

import dojo.Kata;

/**
 * KATA security L1 "PasswordHasher" (tracks/09-security/L1-basics.md, section 3).
 *
 * Store passwords so that a stolen database does not reveal them:
 * - a slow, salted key-derivation function: PBKDF2WithHmacSHA256 from javax.crypto
 *   (SecretKeyFactory + PBEKeySpec), {@link #ITERATIONS} iterations, a 256-bit result;
 * - a fresh random 16-byte salt per hash from a SecureRandom (never Random);
 * - the stored format is self-describing so parameters can change later:
 *   {@code pbkdf2-sha256$<iterations>$<base64 salt>$<base64 hash>} (standard Base64);
 * - verify recomputes with the stored salt and iterations and compares in constant time
 *   (MessageDigest.isEqual, not Arrays.equals or String.equals);
 * - needsRehash tells the login code to upgrade old hashes (fewer iterations) after a
 *   successful login.
 * A malformed stored value never verifies (return false, do not throw).
 */
public final class PasswordHasher {

	public static final String PREFIX = "pbkdf2-sha256";
	public static final int ITERATIONS = 600_000;

	private final int iterations;

	public PasswordHasher() {
		this(ITERATIONS);
	}

	/** For tests and migrations: hash with a different work factor. */
	public PasswordHasher(int iterations) {
		this.iterations = iterations;
	}

	public String hash(char[] password) {
		return Kata.todo("tracks/09-security/L1-basics.md section 3");
	}

	public boolean verify(char[] password, String stored) {
		return Kata.todo("tracks/09-security/L1-basics.md section 3");
	}

	public boolean needsRehash(String stored) {
		return Kata.todo("tracks/09-security/L1-basics.md section 3");
	}

	int iterations() {
		return iterations;
	}

}
