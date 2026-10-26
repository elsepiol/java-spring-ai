package dojo.ops.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.ops.l2.AccessLogStats.Endpoint;

@Tag("ops")
@Tag("L2")
class AccessLogStatsTest {

	private static final List<String> LOG = List.of(
			"2026-01-01T10:00:00Z GET /api/orders/42 200 120ms",
			"2026-01-01T10:00:01Z GET /api/orders/43 200 80ms",
			"2026-01-01T10:00:01Z POST /api/orders 201 300ms",
			"2026-01-01T10:00:02Z GET /api/products 200 15ms",
			"2026-01-01T10:00:02Z GET /api/orders/42 503 2000ms",
			"garbage line",
			"2026-01-01T10:00:03Z GET /api/orders/7/items/3 200 40ms",
			"2026-01-01T10:00:03Z GET /api/products 500 900ms",
			"2026-01-01T10:00:04Z GET /api/products 200 slow",
			"2026-01-01T10:00:04Z GET /api/products 2xx 10ms",
			"2026-01-01T10:00:05Z GET /api/products 404 5ms");

	private final AccessLogStats stats = new AccessLogStats(LOG);

	@Test
	void skipsMalformedLines() {
		assertThat(stats.skipped()).isEqualTo(3);
	}

	@Test
	void countsStatusesInOrder() {
		assertThat(stats.statusCounts()).containsExactly(
				Map.entry(200, 4L), Map.entry(201, 1L), Map.entry(404, 1L), Map.entry(500, 1L), Map.entry(503, 1L));
	}

	@Test
	void ranksEndpointsWithIdsCollapsed() {
		assertThat(stats.topEndpoints(3)).containsExactly(
				new Endpoint("GET /api/orders/{id}", 3),
				new Endpoint("GET /api/products", 3),
				new Endpoint("GET /api/orders/{id}/items/{id}", 1));
		assertThat(stats.topEndpoints(10)).hasSize(4);
	}

	@Test
	void computesTheErrorRate() {
		assertThat(stats.errorRate()).isCloseTo(2.0 / 8, within(1e-9));
		assertThat(new AccessLogStats(List.of()).errorRate()).isZero();
	}

	@Test
	void computesNearestRankPercentiles() {
		// latencies sorted: 5 15 40 80 120 300 900 2000
		assertThat(stats.latencyPercentile(50)).isEqualTo(80);
		assertThat(stats.latencyPercentile(90)).isEqualTo(2000);
		assertThat(stats.latencyPercentile(75)).isEqualTo(300);
		assertThat(stats.latencyPercentile(100)).isEqualTo(2000);
		assertThat(stats.latencyPercentile(1)).isEqualTo(5);
		assertThatIllegalStateException().isThrownBy(() -> new AccessLogStats(List.of()).latencyPercentile(50));
	}

	@Test
	void handlesALargeLog() {
		List<String> lines = new ArrayList<>();
		for (int i = 1; i <= 100_000; i++) {
			lines.add("2026-01-01T10:00:00Z GET /api/items/" + i + " 200 " + i + "ms");
		}
		AccessLogStats big = new AccessLogStats(lines);
		assertThat(big.latencyPercentile(99)).isEqualTo(99_000);
		assertThat(big.topEndpoints(1)).containsExactly(new Endpoint("GET /api/items/{id}", 100_000));
	}

}
