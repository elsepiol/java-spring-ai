package dojo.design.l2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reference solution for the Shortener kata. */
public final class Shortener {

	public static final class AliasTakenException extends RuntimeException {

		public AliasTakenException(String alias) {
			super("alias taken: " + alias);
		}

	}

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9-]{3,30}");

	private final Map<String, String> urlByCode = new HashMap<>();
	private final Map<String, String> codeByUrl = new HashMap<>();
	private long nextId = 1;

	public static String encode(long id) {
		if (id < 0) {
			throw new IllegalArgumentException("negative id");
		}
		if (id == 0) {
			return "0";
		}
		StringBuilder out = new StringBuilder();
		while (id > 0) {
			out.append(ALPHABET.charAt((int) (id % 62)));
			id /= 62;
		}
		return out.reverse().toString();
	}

	public static long decode(String code) {
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException("empty code");
		}
		long id = 0;
		for (char c : code.toCharArray()) {
			int digit = ALPHABET.indexOf(c);
			if (digit < 0) {
				throw new IllegalArgumentException("not base62: " + code);
			}
			id = Math.addExact(Math.multiplyExact(id, 62), digit);
		}
		return id;
	}

	public synchronized String shorten(String url) {
		validate(url);
		String existing = codeByUrl.get(url);
		if (existing != null) {
			return existing;
		}
		String code;
		do {
			code = encode(nextId++);
		} while (urlByCode.containsKey(code)); // skip values someone took as an alias
		store(code, url);
		return code;
	}

	public synchronized String shorten(String url, String alias) {
		validate(url);
		if (alias == null || !ALIAS.matcher(alias).matches()) {
			throw new IllegalArgumentException("invalid alias");
		}
		String owner = urlByCode.get(alias);
		if (owner != null) {
			if (owner.equals(url)) {
				return alias;
			}
			throw new AliasTakenException(alias);
		}
		urlByCode.put(alias, url);
		codeByUrl.putIfAbsent(url, alias);
		return alias;
	}

	public synchronized Optional<String> resolve(String code) {
		return Optional.ofNullable(urlByCode.get(code));
	}

	private void store(String code, String url) {
		urlByCode.put(code, url);
		codeByUrl.put(url, code);
	}

	private static void validate(String url) {
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || uri.getHost() == null) {
				throw new IllegalArgumentException("only http(s) URLs: " + url);
			}
		}
		catch (URISyntaxException | NullPointerException e) {
			throw new IllegalArgumentException("invalid URL: " + url);
		}
	}

}
