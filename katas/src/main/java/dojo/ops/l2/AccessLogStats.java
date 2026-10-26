package dojo.ops.l2;

import java.util.List;
import java.util.Map;

import dojo.Kata;

/**
 * KATA ops L2 "AccessLogStats" (tracks/14-git-linux-networking/L2-practical.md, section 3).
 *
 * What you would do with `grep | awk | sort | uniq -c | sort -rn | head` during an incident,
 * as code. Each line looks like
 *     2026-01-01T10:00:00Z GET /api/orders/42 200 123ms
 * (timestamp, method, path, status, latency). Lines that do not match exactly (wrong number of
 * fields, non-numeric status, latency without "ms") are skipped and counted.
 * - statusCounts: status -> number of requests, iterated in ascending status order;
 * - topEndpoints(n): the n most requested "METHOD path" with numeric path segments replaced by
 *   {id} ("GET /api/orders/{id}"), most requests first, ties alphabetically;
 * - errorRate: share of 5xx responses among parsed lines (0.0 when there are none);
 * - latencyPercentile(p): nearest-rank percentile of latency in ms: sort, take the value at
 *   position ceil(p/100 * count) (1-based). p in (0, 100]; no data -> IllegalStateException.
 */
public final class AccessLogStats {

	public record Endpoint(String name, long requests) {
	}

	public AccessLogStats(List<String> lines) {
		Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

	public int skipped() {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

	public Map<Integer, Long> statusCounts() {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

	public List<Endpoint> topEndpoints(int n) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

	public double errorRate() {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

	public long latencyPercentile(double p) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 3");
	}

}
