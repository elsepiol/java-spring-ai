package dojo.javacore.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L3")
class ExpiringCacheTest {

	/** A clock the test moves by hand. */
	static final class TestClock extends Clock {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration d) {
			now = now.plus(d);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}

	private final TestClock clock = new TestClock();
	private final AtomicInteger loads = new AtomicInteger();

	private String load(String key) {
		loads.incrementAndGet();
		return key.toUpperCase();
	}

	@Test
	void loadsOnceThenServesFromTheCache() {
		ExpiringCache<String, String> cache = new ExpiringCache<>(10, Duration.ofMinutes(5), clock);
		assertThat(cache.get("mug", this::load)).isEqualTo("MUG");
		assertThat(cache.get("mug", this::load)).isEqualTo("MUG");
		assertThat(loads).hasValue(1);
		assertThat(cache.stats()).isEqualTo(new ExpiringCache.Stats(1, 1, 0));
		assertThat(cache.stats().hitRatio()).isEqualTo(0.5);
	}

	@Test
	void staleEntriesAreLoadedAgain() {
		ExpiringCache<String, String> cache = new ExpiringCache<>(10, Duration.ofMinutes(5), clock);
		cache.get("mug", this::load);
		clock.advance(Duration.ofMinutes(4).plusSeconds(59));
		cache.get("mug", this::load);
		assertThat(loads).as("still fresh").hasValue(1);

		clock.advance(Duration.ofSeconds(1)); // exactly 5 minutes old: stale
		assertThat(cache.peek("mug")).isEmpty();
		cache.get("mug", this::load);
		assertThat(loads).hasValue(2);
		assertThat(cache.stats()).isEqualTo(new ExpiringCache.Stats(1, 2, 0));
	}

	@Test
	void isBoundedAndEvictsTheLeastRecentlyUsed() {
		ExpiringCache<String, String> cache = new ExpiringCache<>(2, Duration.ofHours(1), clock);
		cache.get("a", this::load);
		cache.get("b", this::load);
		cache.get("a", this::load); // a used more recently than b
		cache.get("c", this::load); // evicts b

		assertThat(cache.size()).isEqualTo(2);
		assertThat(cache.peek("b")).isEmpty();
		assertThat(cache.peek("a")).contains("A");
		assertThat(cache.stats().evictions()).isEqualTo(1);
	}

	@Test
	void neverGrowsPastItsBound() {
		ExpiringCache<Integer, String> cache = new ExpiringCache<>(1_000, Duration.ofHours(1), clock);
		for (int i = 0; i < 100_000; i++) {
			cache.get(i, String::valueOf);
		}
		assertThat(cache.size()).isEqualTo(1_000);
		assertThat(cache.stats().evictions()).isEqualTo(99_000);
	}

}
