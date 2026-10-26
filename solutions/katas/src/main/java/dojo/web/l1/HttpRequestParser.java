package dojo.web.l1;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Reference solution. Headers are stored with lower-case names, which makes lookups case-insensitive. */
public final class HttpRequestParser {

	public record HttpRequest(String method, String path, Map<String, List<String>> query, Map<String, String> headers, String body) {

		public Optional<String> header(String name) {
			return Optional.ofNullable(headers.get(name.toLowerCase(Locale.ROOT)));
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
		int headEnd = raw.indexOf("\r\n\r\n");
		if (headEnd < 0) {
			throw new MalformedRequestException("no empty line after the headers");
		}
		String[] lines = raw.substring(0, headEnd).split("\r\n");
		String[] start = lines[0].split(" ");
		if (start.length != 3 || !start[2].equals("HTTP/1.1") || start[0].isEmpty() || !start[1].startsWith("/")) {
			throw new MalformedRequestException("bad request line: " + lines[0]);
		}
		Map<String, String> headers = new LinkedHashMap<>();
		for (int i = 1; i < lines.length; i++) {
			int colon = lines[i].indexOf(':');
			if (colon <= 0) {
				throw new MalformedRequestException("bad header line: " + lines[i]);
			}
			String name = lines[i].substring(0, colon).strip().toLowerCase(Locale.ROOT);
			String value = lines[i].substring(colon + 1).strip();
			headers.merge(name, value, (a, b) -> a + ", " + b);
		}
		if (!headers.containsKey("host")) {
			throw new MalformedRequestException("HTTP/1.1 requires a Host header");
		}
		String rest = raw.substring(headEnd + 4);
		int length;
		try {
			length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
		} catch (NumberFormatException e) {
			throw new MalformedRequestException("bad Content-Length");
		}
		if (length < 0 || rest.length() < length) {
			throw new MalformedRequestException("body shorter than Content-Length");
		}
		String target = start[1];
		int q = target.indexOf('?');
		String path = q < 0 ? target : target.substring(0, q);
		Map<String, List<String>> query = new LinkedHashMap<>();
		if (q >= 0 && q < target.length() - 1) {
			for (String pair : target.substring(q + 1).split("&")) {
				int eq = pair.indexOf('=');
				String name = decode(eq < 0 ? pair : pair.substring(0, eq));
				String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
				query.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
			}
		}
		return new HttpRequest(start[0], path, query, headers, rest.substring(0, length));
	}

	private static String decode(String s) {
		try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new MalformedRequestException("bad percent-encoding: " + s);
		}
	}

}
