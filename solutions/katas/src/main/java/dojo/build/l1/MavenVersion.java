package dojo.build.l1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reference solution. */
public final class MavenVersion implements Comparable<MavenVersion> {

	private static final Pattern FORMAT = Pattern.compile("(\\d+(?:\\.\\d+)*)(?:-([A-Za-z]+)-?(\\d*))?");
	private static final List<String> QUALIFIERS = List.of("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");

	private final String text;
	private final List<Long> numbers; // trailing zeros removed, so 1 == 1.0 == 1.0.0
	private final int qualifier; // index in QUALIFIERS
	private final long qualifierNumber;

	private MavenVersion(String text, List<Long> numbers, int qualifier, long qualifierNumber) {
		this.text = text;
		this.numbers = numbers;
		this.qualifier = qualifier;
		this.qualifierNumber = qualifierNumber;
	}

	public static MavenVersion parse(String text) {
		Matcher m = FORMAT.matcher(text.strip());
		if (!m.matches()) {
			throw new IllegalArgumentException("not a version: " + text);
		}
		List<Long> numbers = new ArrayList<>();
		for (String part : m.group(1).split("\\.")) {
			numbers.add(Long.parseLong(part));
		}
		while (!numbers.isEmpty() && numbers.getLast() == 0) {
			numbers.removeLast();
		}
		String q = m.group(2) == null ? "" : normalise(m.group(2).toLowerCase(Locale.ROOT));
		int index = QUALIFIERS.indexOf(q);
		if (index < 0) {
			throw new IllegalArgumentException("unknown qualifier '" + m.group(2) + "' in " + text);
		}
		long qn = m.group(3) == null || m.group(3).isEmpty() ? 0 : Long.parseLong(m.group(3));
		return new MavenVersion(text, List.copyOf(numbers), index, qn);
	}

	private static String normalise(String q) {
		return switch (q) {
			case "a" -> "alpha";
			case "b" -> "beta";
			case "m" -> "milestone";
			case "cr" -> "rc";
			default -> q;
		};
	}

	public boolean isSnapshot() {
		return QUALIFIERS.get(qualifier).equals("snapshot");
	}

	@Override
	public int compareTo(MavenVersion other) {
		int n = Math.max(numbers.size(), other.numbers.size());
		for (int i = 0; i < n; i++) {
			long a = i < numbers.size() ? numbers.get(i) : 0;
			long b = i < other.numbers.size() ? other.numbers.get(i) : 0;
			if (a != b) {
				return Long.compare(a, b);
			}
		}
		if (qualifier != other.qualifier) {
			return Integer.compare(qualifier, other.qualifier);
		}
		return Long.compare(qualifierNumber, other.qualifierNumber);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof MavenVersion v && compareTo(v) == 0;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(numbers, qualifier, qualifierNumber); // consistent with compareTo
	}

	@Override
	public String toString() {
		return text;
	}

}
