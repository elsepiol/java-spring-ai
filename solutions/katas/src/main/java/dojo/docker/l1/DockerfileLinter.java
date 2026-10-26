package dojo.docker.l1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Reference solution for the DockerfileLinter kata. */
public final class DockerfileLinter {

	public record Finding(int line, String rule) {
	}

	private record Instruction(int line, String keyword, String args) {
	}

	private static final Pattern SECRET = Pattern.compile("(?i).*(PASSWORD|SECRET|TOKEN|API_KEY).*");

	public List<Finding> lint(String dockerfile) {
		List<Instruction> instructions = parse(dockerfile);
		List<Finding> findings = new ArrayList<>();
		Set<String> stages = new HashSet<>();
		int finalFrom = -1;
		Instruction finalUser = null;
		for (Instruction in : instructions) {
			switch (in.keyword) {
				case "FROM" -> {
					String[] parts = in.args.split("\\s+");
					String image = parts[0];
					if (!pinned(image, stages)) {
						findings.add(new Finding(in.line, "pin-base-image"));
					}
					if (parts.length >= 3 && parts[1].equalsIgnoreCase("AS")) {
						stages.add(parts[2].toLowerCase(Locale.ROOT));
					}
					finalFrom = in.line;
					finalUser = null;
				}
				case "USER" -> finalUser = in;
				case "ADD" -> findings.add(new Finding(in.line, "copy-not-add"));
				case "ENV", "ARG" -> {
					if (setsSecret(in.args)) {
						findings.add(new Finding(in.line, "secret-in-env"));
					}
				}
				case "RUN" -> {
					if (in.args.contains("apt-get install") && !in.args.contains("/var/lib/apt/lists")) {
						findings.add(new Finding(in.line, "apt-lists"));
					}
				}
				case "ENTRYPOINT", "CMD" -> {
					if (!in.args.startsWith("[")) {
						findings.add(new Finding(in.line, "exec-form"));
					}
				}
				default -> {
				}
			}
		}
		if (finalFrom > 0) {
			if (finalUser == null) {
				findings.add(new Finding(finalFrom, "non-root-user"));
			}
			else {
				String user = finalUser.args.split("[:\\s]")[0];
				if (user.equals("root") || user.equals("0")) {
					findings.add(new Finding(finalUser.line, "non-root-user"));
				}
			}
		}
		findings.sort(Comparator.comparingInt(Finding::line).thenComparing(Finding::rule));
		return findings;
	}

	private static boolean pinned(String image, Set<String> stages) {
		if (image.equalsIgnoreCase("scratch") || stages.contains(image.toLowerCase(Locale.ROOT)) || image.contains("@sha256:")) {
			return true;
		}
		int slash = image.lastIndexOf('/');
		int colon = image.lastIndexOf(':');
		if (colon <= slash) {
			return false;
		}
		return !image.substring(colon + 1).equals("latest");
	}

	private static boolean setsSecret(String args) {
		// ENV KEY=value KEY2=value, ENV KEY value, ARG KEY[=default]
		for (String token : args.split("\\s+")) {
			String name = token.contains("=") ? token.substring(0, token.indexOf('=')) : token;
			if (SECRET.matcher(name).matches()) {
				return true;
			}
			if (!token.contains("=")) {
				break; // "ENV KEY value": only the first token is a name
			}
		}
		return false;
	}

	private static List<Instruction> parse(String dockerfile) {
		List<Instruction> result = new ArrayList<>();
		String[] lines = dockerfile.split("\\R", -1);
		StringBuilder current = null;
		int start = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].strip();
			if (current == null) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				current = new StringBuilder();
				start = i + 1;
			}
			else if (line.startsWith("#")) {
				continue;
			}
			boolean continues = line.endsWith("\\");
			current.append(continues ? line.substring(0, line.length() - 1) : line).append(' ');
			if (!continues) {
				String text = current.toString().strip();
				int space = text.indexOf(' ');
				String keyword = (space < 0 ? text : text.substring(0, space)).toUpperCase(Locale.ROOT);
				String args = space < 0 ? "" : text.substring(space + 1).strip().replaceAll("\\s+", " ");
				result.add(new Instruction(start, keyword, args));
				current = null;
			}
		}
		return result;
	}

}
