import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The dojo's question drill. No build needed, no dependencies:
 *
 *   java quiz/Quiz.java                          10 questions: due ones first, then new ones
 *   java quiz/Quiz.java --track concurrency --level 2 --count 15
 *   java quiz/Quiz.java --kind bug,predict       only "spot the bug" and "what does it print"
 *   java quiz/Quiz.java --stats                  what you know, per track and level
 *   java quiz/Quiz.java --check                  validate every question file (CI runs this)
 *
 * It reads tracks/(track)/questions.md. Your answers are remembered in .dojo/progress.tsv (not
 * committed) with a Leitner box per question: knew it -> next box, asked again after 1, 3, 7, 14,
 * then 30 days; did not know it -> back to box 1 and asked again before this session ends.
 */
public class Quiz {

	static final Pattern HEADER = Pattern.compile("^### ([A-Z]+-L([123])-\\d{3}) · ([a-z]+)\\s*$");
	static final Set<String> KINDS = Set.of("recall", "why", "predict", "bug", "scenario", "design", "interview", "dsa");
	static final int[] INTERVAL_DAYS = { 0, 1, 3, 7, 14, 30 }; // index = box

	record Question(String id, String track, int level, String kind, String text, String answer) {
	}

	record Card(int box, LocalDate due, int seen, int missed) {
		static final Card NEW = new Card(0, LocalDate.MIN, 0, 0);
	}

	public static void main(String[] args) throws IOException {
		Map<String, String> opts = parseArgs(args);
		Path root = findRoot();
		List<String> problems = new ArrayList<>();
		List<Question> all = load(root, problems);

		if (opts.containsKey("check")) {
			check(all, problems);
			return;
		}
		if (!problems.isEmpty()) {
			problems.forEach(p -> System.err.println("warning: " + p));
		}
		Path progressFile = root.resolve(".dojo/progress.tsv");
		Map<String, Card> progress = loadProgress(progressFile);
		List<Question> chosen = filter(all, opts);

		if (opts.containsKey("stats")) {
			stats(chosen, progress);
			return;
		}
		if (opts.containsKey("list")) {
			chosen.forEach(q -> System.out.println(q.id() + "  " + q.kind() + "  " + firstLine(q.text())));
			return;
		}
		if (chosen.isEmpty()) {
			System.out.println("No questions match " + opts + ". Try --stats to see what exists.");
			return;
		}
		int count = Integer.parseInt(opts.getOrDefault("count", "10"));
		drill(pick(chosen, progress, count, opts.containsKey("new")), progress, progressFile);
	}

	// ---- loading ----

	static Path findRoot() {
		Path dir = Path.of("").toAbsolutePath();
		for (Path p = dir; p != null; p = p.getParent()) {
			if (Files.isDirectory(p.resolve("tracks"))) {
				return p;
			}
		}
		throw new IllegalStateException("Run this from inside the dojo repository (no tracks/ folder found above " + dir + ")");
	}

	static List<Question> load(Path root, List<String> problems) throws IOException {
		List<Question> out = new ArrayList<>();
		List<Path> files;
		try (Stream<Path> s = Files.list(root.resolve("tracks"))) {
			files = s.map(d -> d.resolve("questions.md")).filter(Files::exists).sorted().toList();
		}
		for (Path file : files) {
			String track = file.getParent().getFileName().toString().replaceFirst("^\\d+-", "");
			out.addAll(parse(track, Files.readAllLines(file, StandardCharsets.UTF_8), root.relativize(file).toString(), problems));
		}
		return out;
	}

	static List<Question> parse(String track, List<String> lines, String fileName, List<String> problems) {
		List<Question> out = new ArrayList<>();
		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);
			if (!line.startsWith("### ")) {
				i++;
				continue;
			}
			Matcher m = HEADER.matcher(line);
			if (!m.matches()) {
				problems.add(fileName + ":" + (i + 1) + ": heading is not '### ID-L1-001 · kind': " + line);
				i++;
				continue;
			}
			int start = i + 1;
			StringBuilder text = new StringBuilder();
			StringBuilder answer = new StringBuilder();
			boolean inAnswer = false;
			boolean closed = false;
			for (i = start; i < lines.size() && !lines.get(i).startsWith("### ") && !lines.get(i).startsWith("## "); i++) {
				String l = lines.get(i);
				if (l.trim().startsWith("<details>")) {
					inAnswer = true;
				} else if (l.trim().equals("</details>")) {
					closed = true;
					inAnswer = false;
				} else if (inAnswer) {
					answer.append(l).append('\n');
				} else if (!closed) {
					text.append(l).append('\n');
				}
			}
			String id = m.group(1);
			String kind = m.group(3);
			if (!KINDS.contains(kind)) {
				problems.add(fileName + ": " + id + ": unknown kind '" + kind + "', use one of " + new TreeMap<>(Map.of("k", KINDS)).get("k"));
			}
			if (!closed || answer.toString().isBlank()) {
				problems.add(fileName + ": " + id + ": needs an answer inside <details>...</details>");
			}
			if (text.toString().isBlank()) {
				problems.add(fileName + ": " + id + ": the question is empty");
			}
			out.add(new Question(id, track, Integer.parseInt(m.group(2)), kind, text.toString().strip(), answer.toString().strip()));
		}
		return out;
	}

	static void check(List<Question> all, List<String> problems) {
		Set<String> seen = new HashSet<>();
		for (Question q : all) {
			if (!seen.add(q.id())) {
				problems.add("duplicate id " + q.id());
			}
		}
		Map<String, int[]> perTrack = new TreeMap<>();
		all.forEach(q -> perTrack.computeIfAbsent(q.track(), t -> new int[4])[q.level()]++);
		perTrack.forEach((t, n) -> System.out.printf("%-14s L1 %3d   L2 %3d   L3 %3d%n", t, n[1], n[2], n[3]));
		System.out.println("total " + all.size() + " questions");
		if (!problems.isEmpty()) {
			problems.forEach(p -> System.out.println("::error::" + p));
			System.exit(1);
		}
	}

	// ---- choosing ----

	static List<Question> filter(List<Question> all, Map<String, String> opts) {
		List<String> tracks = csv(opts.get("track"));
		List<String> levels = csv(opts.get("level"));
		List<String> kinds = csv(opts.get("kind"));
		return all.stream()
				.filter(q -> tracks.isEmpty() || tracks.stream().anyMatch(t -> q.track().contains(t)))
				.filter(q -> levels.isEmpty() || levels.contains(String.valueOf(q.level())) || levels.contains("l" + q.level()))
				.filter(q -> kinds.isEmpty() || kinds.contains(q.kind()))
				.toList();
	}

	static List<Question> pick(List<Question> candidates, Map<String, Card> progress, int count, boolean onlyNew) {
		LocalDate today = LocalDate.now();
		List<Question> due = new ArrayList<>();
		List<Question> fresh = new ArrayList<>();
		for (Question q : candidates) {
			Card c = progress.getOrDefault(q.id(), Card.NEW);
			if (c.seen() == 0) {
				fresh.add(q);
			} else if (!onlyNew && !c.due().isAfter(today)) {
				due.add(q);
			}
		}
		Random random = new Random();
		Collections.shuffle(due, random);
		// Lowest box first: the things you know least come back first.
		due.sort((a, b) -> Integer.compare(progress.get(a.id()).box(), progress.get(b.id()).box()));
		// New questions in course order (L1 before L3), shuffled within a level.
		Collections.shuffle(fresh, random);
		fresh.sort((a, b) -> Integer.compare(a.level(), b.level()));
		List<Question> out = new ArrayList<>(due);
		out.addAll(fresh);
		return out.subList(0, Math.min(count, out.size()));
	}

	// ---- the drill ----

	static void drill(List<Question> questions, Map<String, Card> progress, Path progressFile) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		Deque<Question> queue = new ArrayDeque<>(questions);
		Set<String> missedThisSession = new HashSet<>();
		int knew = 0;
		int total = 0;
		System.out.println(questions.size() + " questions. Answer out loud or on paper BEFORE you reveal: recall is the exercise.\n");
		while (!queue.isEmpty()) {
			Question q = queue.poll();
			Card card = progress.getOrDefault(q.id(), Card.NEW);
			System.out.println("=".repeat(72));
			System.out.printf("%s   [%s · L%d · %s]   box %d%n%n", q.id(), q.track(), q.level(), q.kind(), card.box());
			System.out.println(q.text());
			System.out.print("\n(Enter to reveal the answer) ");
			if (in.readLine() == null) {
				break;
			}
			System.out.println("\n--- answer " + "-".repeat(61));
			System.out.println(q.answer());
			System.out.println("-".repeat(72));
			String verdict = ask(in, "Did you know it?  [y]es  [p]artly  [n]o  [q]uit: ", "ypnq");
			if (verdict == null || verdict.equals("q")) {
				break;
			}
			total++;
			LocalDate today = LocalDate.now();
			Card next = switch (verdict) {
				case "y" -> {
					knew++;
					// A question missed earlier this session does not jump boxes on the retry.
					int box = missedThisSession.contains(q.id()) ? 1 : Math.min(card.box() + 1, 5);
					yield new Card(box, today.plusDays(INTERVAL_DAYS[box]), card.seen() + 1, card.missed());
				}
				case "p" -> {
					int box = Math.max(card.box(), 1);
					yield new Card(box, today.plusDays(INTERVAL_DAYS[box]), card.seen() + 1, card.missed());
				}
				default -> {
					if (missedThisSession.add(q.id())) {
						queue.addLast(q); // once more before the session ends
					}
					yield new Card(1, today.plusDays(1), card.seen() + 1, card.missed() + 1);
				}
			};
			progress.put(q.id(), next);
			saveProgress(progressFile, progress);
			System.out.println();
		}
		System.out.printf("%nDone: knew %d of %d. Progress saved in %s%n", knew, total, progressFile);
		if (!missedThisSession.isEmpty()) {
			System.out.println("Re-read the lesson sections for: " + String.join(", ", new java.util.TreeSet<>(missedThisSession)));
		}
	}

	static String ask(BufferedReader in, String prompt, String allowed) throws IOException {
		while (true) {
			System.out.print(prompt);
			String line = in.readLine();
			if (line == null) {
				return null;
			}
			String s = line.trim().toLowerCase();
			if (s.length() == 1 && allowed.contains(s)) {
				return s;
			}
		}
	}

	static void stats(List<Question> questions, Map<String, Card> progress) {
		LocalDate today = LocalDate.now();
		Map<String, int[]> rows = new LinkedHashMap<>(); // total, new, due, mastered(box>=4)
		for (Question q : questions) {
			int[] r = rows.computeIfAbsent(q.track() + " L" + q.level(), k -> new int[4]);
			Card c = progress.getOrDefault(q.id(), Card.NEW);
			r[0]++;
			if (c.seen() == 0) {
				r[1]++;
			} else if (!c.due().isAfter(today)) {
				r[2]++;
			}
			if (c.box() >= 4) {
				r[3]++;
			}
		}
		System.out.printf("%-20s %6s %6s %6s %9s%n", "track/level", "total", "new", "due", "mastered");
		rows.forEach((k, r) -> System.out.printf("%-20s %6d %6d %6d %9d%n", k, r[0], r[1], r[2], r[3]));
		List<Map.Entry<String, Card>> weakest = progress.entrySet().stream()
				.filter(e -> e.getValue().missed() > 0)
				.sorted((a, b) -> Integer.compare(b.getValue().missed(), a.getValue().missed()))
				.limit(5).toList();
		if (!weakest.isEmpty()) {
			System.out.println("\nMissed most often: " + weakest.stream().map(e -> e.getKey() + " (" + e.getValue().missed() + "x)").toList());
		}
	}

	// ---- progress file ----

	static Map<String, Card> loadProgress(Path file) throws IOException {
		Map<String, Card> out = new HashMap<>();
		if (!Files.exists(file)) {
			return out;
		}
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			String[] f = line.split("\t");
			if (f.length == 5 && !f[0].equals("id")) {
				out.put(f[0], new Card(Integer.parseInt(f[1]), LocalDate.parse(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4])));
			}
		}
		return out;
	}

	static void saveProgress(Path file, Map<String, Card> progress) throws IOException {
		Files.createDirectories(file.getParent());
		StringBuilder sb = new StringBuilder("id\tbox\tdue\tseen\tmissed\n");
		new TreeMap<>(progress).forEach((id, c) -> sb.append(id).append('\t').append(c.box()).append('\t').append(c.due())
				.append('\t').append(c.seen()).append('\t').append(c.missed()).append('\n'));
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, sb, StandardCharsets.UTF_8);
		Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
	}

	// ---- small helpers ----

	static Map<String, String> parseArgs(String[] args) {
		Map<String, String> out = new LinkedHashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("--")) {
				throw new IllegalArgumentException("Unexpected argument " + args[i] + ". See the comment at the top of quiz/Quiz.java.");
			}
			String key = args[i].substring(2);
			boolean flag = Set.of("check", "stats", "list", "new").contains(key);
			if (flag) {
				out.put(key, "true");
			} else if (i + 1 < args.length) {
				out.put(key, args[++i]);
			} else {
				throw new IllegalArgumentException("--" + key + " needs a value");
			}
		}
		return out;
	}

	static List<String> csv(String value) {
		return value == null ? List.of() : Stream.of(value.toLowerCase().split(",")).map(String::trim).toList();
	}

	static String firstLine(String text) {
		String first = text.lines().findFirst().orElse("");
		return first.length() > 90 ? first.substring(0, 87) + "..." : first;
	}
}
