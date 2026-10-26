package dojo.security.l2;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import dojo.Kata;

/**
 * KATA security L2 "JwtCodec" (tracks/09-security/L2-practical.md, section 2).
 *
 * Issue and verify HS256 JSON Web Tokens by hand, to know exactly what a JWT library checks for you.
 * A token is {@code base64url(header) + "." + base64url(payload) + "." + base64url(signature)}:
 * - base64url WITHOUT padding (Base64.getUrlEncoder().withoutPadding());
 * - header: {"alg":"HS256","typ":"JWT"};
 * - payload: {"sub":..., "roles":[...], "iat":<epoch seconds>, "exp":<epoch seconds>};
 * - signature: HmacSHA256(secret, the first two parts joined by ".") (javax.crypto.Mac).
 *
 * verify must throw InvalidTokenException unless ALL of these hold:
 * - three parts, each valid base64url, header and payload valid JSON objects;
 * - the header's alg is exactly HS256 (never trust "none" or anything else the token claims);
 * - the signature matches, compared in constant time (MessageDigest.isEqual);
 * - exp is present and now is before exp + {@link #CLOCK_SKEW};
 * - sub is present and not blank.
 * The secret must be at least 32 bytes (256 bits): shorter secrets are rejected in the constructor
 * with IllegalArgumentException. JSON: use Jackson (tools.jackson.databind.json.JsonMapper).
 */
public final class JwtCodec {

	public static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

	public record Claims(String subject, List<String> roles, Instant expiresAt) {
	}

	public JwtCodec(byte[] secret, Clock clock) {
		Kata.todo("tracks/09-security/L2-practical.md section 2");
	}

	public String issue(String subject, List<String> roles, Duration ttl) {
		return Kata.todo("tracks/09-security/L2-practical.md section 2");
	}

	public Claims verify(String token) {
		return Kata.todo("tracks/09-security/L2-practical.md section 2");
	}

}
