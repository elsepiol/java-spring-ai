package dojo.security.l3;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Reference solution for the InputGuards kata. */
public final class InputGuards {

	private final Set<String> allowedHosts;

	public InputGuards(Set<String> allowedHosts) {
		this.allowedHosts = allowedHosts.stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
	}

	public static String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder out = new StringBuilder(text.length() + 16);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				case '\'' -> out.append("&#39;");
				default -> out.append(c);
			}
		}
		return out.toString();
	}

	public static Path resolveUpload(Path baseDir, String fileName) {
		if (fileName == null || fileName.isBlank() || fileName.indexOf('\0') >= 0) {
			throw new IllegalArgumentException("invalid file name");
		}
		Path base = baseDir.toAbsolutePath().normalize();
		Path requested;
		try {
			requested = Path.of(fileName);
		}
		catch (InvalidPathException e) {
			throw new IllegalArgumentException("invalid file name");
		}
		if (requested.isAbsolute() || fileName.startsWith("/") || fileName.startsWith("\\")) {
			throw new IllegalArgumentException("absolute paths are not allowed");
		}
		Path resolved = base.resolve(requested).normalize();
		if (!resolved.startsWith(base) || resolved.equals(base)) {
			throw new IllegalArgumentException("path escapes the upload directory");
		}
		return resolved;
	}

	public boolean isSafeRedirect(String target) {
		if (target == null || target.isEmpty() || target.chars().anyMatch(c -> c < 0x20 || c == 0x7f || c == '\\')) {
			return false;
		}
		if (target.startsWith("/")) {
			return !target.startsWith("//");
		}
		try {
			URI uri = new URI(target);
			return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
					&& allowedHosts.contains(uri.getHost().toLowerCase(Locale.ROOT));
		}
		catch (URISyntaxException e) {
			return false;
		}
	}

}
