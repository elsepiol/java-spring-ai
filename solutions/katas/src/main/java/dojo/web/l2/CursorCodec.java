package dojo.web.l2;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Reference solution. */
public final class CursorCodec {

	public record Position(long priceCents, long id) {
	}

	public static final class InvalidCursorException extends RuntimeException {
		public InvalidCursorException(String message) {
			super(message);
		}
	}

	private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DEC = Base64.getUrlDecoder();
	private final SecretKeySpec key;

	public CursorCodec(byte[] secretKey) {
		this.key = new SecretKeySpec(secretKey.clone(), "HmacSHA256");
	}

	public String encode(Position position) {
		String payload = ENC.encodeToString((position.priceCents() + ":" + position.id()).getBytes(StandardCharsets.UTF_8));
		return payload + "." + ENC.encodeToString(sign(payload));
	}

	public Position decode(String cursor) {
		int dot = cursor.indexOf('.');
		if (dot <= 0) {
			throw new InvalidCursorException("malformed cursor");
		}
		String payload = cursor.substring(0, dot);
		byte[] signature;
		String text;
		try {
			signature = DEC.decode(cursor.substring(dot + 1));
			text = new String(DEC.decode(payload), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new InvalidCursorException("malformed cursor");
		}
		if (!MessageDigest.isEqual(sign(payload), signature)) { // constant time: no timing side channel
			throw new InvalidCursorException("cursor signature does not match");
		}
		String[] parts = text.split(":");
		try {
			return new Position(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
		} catch (RuntimeException e) {
			throw new InvalidCursorException("malformed cursor content");
		}
	}

	private byte[] sign(String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

}
