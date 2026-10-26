package dojo.security.l1;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Reference solution for the PasswordHasher kata. */
public final class PasswordHasher {

	public static final String PREFIX = "pbkdf2-sha256";
	public static final int ITERATIONS = 600_000;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final int iterations;

	public PasswordHasher() {
		this(ITERATIONS);
	}

	public PasswordHasher(int iterations) {
		this.iterations = iterations;
	}

	public String hash(char[] password) {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		byte[] hash = derive(password, salt, iterations);
		Base64.Encoder b64 = Base64.getEncoder();
		return PREFIX + "$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
	}

	public boolean verify(char[] password, String stored) {
		Parsed p = parse(stored);
		if (p == null) {
			return false;
		}
		return MessageDigest.isEqual(derive(password, p.salt, p.iterations), p.hash);
	}

	public boolean needsRehash(String stored) {
		Parsed p = parse(stored);
		return p == null || p.iterations < iterations;
	}

	int iterations() {
		return iterations;
	}

	private record Parsed(int iterations, byte[] salt, byte[] hash) {
	}

	private static Parsed parse(String stored) {
		if (stored == null) {
			return null;
		}
		String[] parts = stored.split("\\$");
		if (parts.length != 4 || !parts[0].equals(PREFIX)) {
			return null;
		}
		try {
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] hash = Base64.getDecoder().decode(parts[3]);
			if (iterations < 1 || salt.length == 0 || hash.length == 0) {
				return null;
			}
			return new Parsed(iterations, salt, hash);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static byte[] derive(char[] password, byte[] salt, int iterations) {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
		finally {
			spec.clearPassword();
		}
	}

}
