package dojo.build.l3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reference solution. */
public final class LockFile {

	private static final Pattern LINE = Pattern.compile("(\\S+)\\s+sha256:([0-9a-f]{64})");

	private LockFile() {
	}

	public static List<String> verify(Map<String, byte[]> artifacts, String lockFile) {
		Map<String, String> expected = new HashMap<>();
		for (String raw : lockFile.split("\\R")) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			Matcher m = LINE.matcher(line);
			if (!m.matches()) {
				throw new IllegalArgumentException("malformed lock file line: " + line);
			}
			expected.put(m.group(1), m.group(2));
		}
		List<String> problems = new ArrayList<>();
		expected.forEach((path, hash) -> {
			byte[] content = artifacts.get(path);
			if (content == null) {
				problems.add("MISSING " + path);
			} else if (!MessageDigest.isEqual(hash.getBytes(), sha256(content).getBytes())) {
				problems.add("MISMATCH " + path);
			}
		});
		artifacts.keySet().stream().filter(p -> !expected.containsKey(p)).forEach(p -> problems.add("UNEXPECTED " + p));
		problems.sort(null);
		return problems;
	}

	public static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("every JVM has SHA-256", e);
		}
	}

}
