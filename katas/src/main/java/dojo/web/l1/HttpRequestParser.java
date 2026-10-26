package dojo.web.l1;

import dojo.Kata;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KATA web L1 "HttpRequestParser" (tracks/05-web-http/L1-basics.md, section 2).
 *
 * Under every framework, an HTTP/1.1 request is text. Parse one:
 *
 *   POST /api/v1/orders?coupon=SPRING%2026&tag=a&tag=b HTTP/1.1\r\n
 *   Host: shop.example.com\r\n
 *   Content-Type: application/json\r\n
 *   Content-Length: 13\r\n
 *   \r\n
 *   {"items":[1]}
 *
 * Rules:
 * - Lines end with CRLF. The first line is METHOD SP TARGET SP VERSION; only "HTTP/1.1" is accepted.
 * - The target is a path plus an optional query. Query parameters are percent-decoded as UTF-8
 *   ("+" means space, "%2F" is "/"); a name can repeat (tag=a&tag=b); a name without "=" has value "".
 * - Header names are case-insensitive (header("content-type") finds "Content-Type"); values are trimmed.
 *   A header that appears twice: the values are joined with ", ".
 * - HTTP/1.1 requires a Host header.
 * - The body is exactly Content-Length characters after the empty line (no Content-Length: empty body);
 *   fewer characters than announced is malformed. (Treat characters as bytes: ASCII bodies only.)
 * - Anything malformed: MalformedRequestException (a server answers 400 Bad Request).
 */
public final class HttpRequestParser {

	public record HttpRequest(String method, String path, Map<String, List<String>> query, Map<String, String> headers, String body) {

		/** Case-insensitive header lookup. */
		public Optional<String> header(String name) {
			return Kata.todo("tracks/05-web-http/L1-basics.md, section 2");
		}
	}

	public static final class MalformedRequestException extends RuntimeException {
		public MalformedRequestException(String message) {
			super(message);
		}
	}

	private HttpRequestParser() {
	}

	public static HttpRequest parse(String raw) {
		return Kata.todo("tracks/05-web-http/L1-basics.md, section 2");
	}

}
