package dojo.security.l2;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Reference solution for the JwtCodec kata. */
public final class JwtCodec {

	public static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

	public record Claims(String subject, List<String> roles, Instant expiresAt) {
	}

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final byte[] secret;
	private final Clock clock;

	public JwtCodec(byte[] secret, Clock clock) {
		if (secret == null || secret.length < 32) {
			throw new IllegalArgumentException("HS256 needs a secret of at least 256 bits");
		}
		this.secret = secret.clone();
		this.clock = clock;
	}

	public String issue(String subject, List<String> roles, Duration ttl) {
		Instant now = clock.instant();
		ObjectNode header = JSON.createObjectNode().put("alg", "HS256").put("typ", "JWT");
		ObjectNode payload = JSON.createObjectNode().put("sub", subject);
		roles.forEach(payload.putArray("roles")::add);
		payload.put("iat", now.getEpochSecond()).put("exp", now.plus(ttl).getEpochSecond());
		String signingInput = encode(header) + "." + encode(payload);
		return signingInput + "." + ENCODER.encodeToString(sign(signingInput));
	}

	public Claims verify(String token) {
		if (token == null) {
			throw new InvalidTokenException("missing token");
		}
		String[] parts = token.split("\\.", -1);
		if (parts.length != 3) {
			throw new InvalidTokenException("not three parts");
		}
		JsonNode header = decode(parts[0]);
		if (!"HS256".equals(header.path("alg").asString(null))) {
			throw new InvalidTokenException("unexpected alg");
		}
		byte[] signature;
		try {
			signature = DECODER.decode(parts[2]);
		}
		catch (IllegalArgumentException e) {
			throw new InvalidTokenException("signature is not base64url");
		}
		if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), signature)) {
			throw new InvalidTokenException("bad signature");
		}
		JsonNode payload = decode(parts[1]);
		JsonNode exp = payload.get("exp");
		if (exp == null || !exp.canConvertToLong()) {
			throw new InvalidTokenException("no exp");
		}
		Instant expiresAt = Instant.ofEpochSecond(exp.asLong());
		if (!clock.instant().isBefore(expiresAt.plus(CLOCK_SKEW))) {
			throw new InvalidTokenException("expired");
		}
		String subject = payload.path("sub").asString(null);
		if (subject == null || subject.isBlank()) {
			throw new InvalidTokenException("no subject");
		}
		List<String> roles = new ArrayList<>();
		payload.path("roles").forEach(r -> roles.add(r.asString()));
		return new Claims(subject, List.copyOf(roles), expiresAt);
	}

	private static String encode(JsonNode node) {
		return ENCODER.encodeToString(JSON.writeValueAsBytes(node));
	}

	private static JsonNode decode(String part) {
		try {
			JsonNode node = JSON.readTree(new String(DECODER.decode(part), StandardCharsets.UTF_8));
			if (!node.isObject()) {
				throw new InvalidTokenException("not a JSON object");
			}
			return node;
		}
		catch (IllegalArgumentException | JacksonException e) {
			throw new InvalidTokenException("malformed part");
		}
	}

	private byte[] sign(String signingInput) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

}
