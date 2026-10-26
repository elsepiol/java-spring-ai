package dojo.ops.l2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Reference solution for the AccessLogStats kata. */
public final class AccessLogStats {

	public record Endpoint(String name, long requests) {
	}

	private record Request(String method, String path, int status, long latencyMs) {
	}

	private static final Pattern NUMERIC = Pattern.compile("\\d+");

	private final List<Request> requests = new ArrayList<>();
	private int skipped;

	public AccessLogStats(List<String> lines) {
		for (String line : lines) {
			Request r = parse(line);
			if (r == null) {
				skipped++;
			}
			else {
				requests.add(r);
			}
		}
	}

	public int skipped() {
		return skipped;
	}

	public Map<Integer, Long> statusCounts() {
		Map<Integer, Long> counts = new TreeMap<>();
		for (Request r : requests) {
			counts.merge(r.status, 1L, Long::sum);
		}
		return counts;
	}

	public List<Endpoint> topEndpoints(int n) {
		Map<String, Long> counts = new HashMap<>();
		for (Request r : requests) {
			counts.merge(r.method + " " + normalise(r.path), 1L, Long::sum);
		}
		return counts.entrySet().stream()
			.map(e -> new Endpoint(e.getKey(), e.getValue()))
			.sorted(Comparator.comparingLong(Endpoint::requests).reversed().thenComparing(Endpoint::name))
			.limit(n)
			.toList();
	}

	public double errorRate() {
		if (requests.isEmpty()) {
			return 0.0;
		}
		return (double) requests.stream().filter(r -> r.status >= 500 && r.status <= 599).count() / requests.size();
	}

	public long latencyPercentile(double p) {
		if (requests.isEmpty()) {
			throw new IllegalStateException("no requests");
		}
		if (!(p > 0 && p <= 100)) {
			throw new IllegalArgumentException("p must be in (0, 100]");
		}
		long[] sorted = requests.stream().mapToLong(Request::latencyMs).sorted().toArray();
		int rank = (int) Math.ceil(p / 100.0 * sorted.length);
		return sorted[Math.max(rank, 1) - 1];
	}

	private static String normalise(String path) {
		String[] segments = path.split("/", -1);
		for (int i = 0; i < segments.length; i++) {
			if (NUMERIC.matcher(segments[i]).matches()) {
				segments[i] = "{id}";
			}
		}
		return String.join("/", segments);
	}

	private static Request parse(String line) {
		String[] f = line.strip().split("\\s+");
		if (f.length != 5 || !f[4].endsWith("ms")) {
			return null;
		}
		try {
			int status = Integer.parseInt(f[3]);
			long latency = Long.parseLong(f[4].substring(0, f[4].length() - 2));
			if (status < 100 || status > 599 || latency < 0) {
				return null;
			}
			return new Request(f[1], f[2], status, latency);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
