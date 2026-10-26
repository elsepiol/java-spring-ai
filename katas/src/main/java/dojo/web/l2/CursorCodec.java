package dojo.web.l2;

import dojo.Kata;

/**
 * KATA web L2 "CursorCodec" (tracks/05-web-http/L2-practical.md, section 5: pagination cursors).
 *
 * A keyset-paginated API returns "nextCursor": an OPAQUE string the client sends back to get the
 * next page. Inside, it holds the last row's sort key (price and id). Opaque means clients cannot
 * build or edit cursors, so you are free to change their content later, and nobody can craft one
 * to skip filters.
 *
 * encode: Base64URL (no padding) of the text "<priceCents>:<id>", then "." and a Base64URL
 * HMAC-SHA256 signature of that payload with the server's secret key.
 * decode: verify the signature (constant-time comparison) and parse; anything wrong (tampered,
 * signed with another key, garbage, missing parts) -> InvalidCursorException (the API answers 400).
 */
public final class CursorCodec {

	public record Position(long priceCents, long id) {
	}

	public static final class InvalidCursorException extends RuntimeException {
		public InvalidCursorException(String message) {
			super(message);
		}
	}

	public CursorCodec(byte[] secretKey) {
	}

	public String encode(Position position) {
		return Kata.todo("tracks/05-web-http/L2-practical.md, section 5");
	}

	public Position decode(String cursor) {
		return Kata.todo("tracks/05-web-http/L2-practical.md, section 5");
	}

}
