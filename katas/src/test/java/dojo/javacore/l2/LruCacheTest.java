package dojo.javacore.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L2")
class LruCacheTest {

	@Test
	void evictsTheLeastRecentlyUsedEntry() {
		LruCache<String, Integer> cache = new LruCache<>(2);
		cache.put("a", 1);
		cache.put("b", 2);
		cache.get("a"); // a is now more recent than b
		cache.put("c", 3); // evicts b

		assertThat(cache.get("b")).isEmpty();
		assertThat(cache.get("a")).contains(1);
		assertThat(cache.get("c")).contains(3);
		assertThat(cache.size()).isEqualTo(2);
	}

	@Test
	void putOnAnExistingKeyReplacesTheValueAndRefreshesIt() {
		LruCache<String, Integer> cache = new LruCache<>(2);
		cache.put("a", 1);
		cache.put("b", 2);
		cache.put("a", 10); // a refreshed
		cache.put("c", 3); // evicts b

		assertThat(cache.get("a")).contains(10);
		assertThat(cache.get("b")).isEmpty();
		assertThat(cache.size()).isEqualTo(2);
	}

	@Test
	void capacityOne() {
		LruCache<Integer, String> cache = new LruCache<>(1);
		cache.put(1, "one");
		cache.put(2, "two");
		assertThat(cache.get(1)).isEmpty();
		assertThat(cache.get(2)).contains("two");
	}

	@Test
	void isConstantTimePerOperation() {
		LruCache<Integer, Integer> cache = new LruCache<>(50_000);
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
			for (int i = 0; i < 1_000_000; i++) {
				cache.put(i, i);
				cache.get(i - 25_000);
			}
		});
		assertThat(cache.size()).isEqualTo(50_000);
	}

	@Test
	void theLinkedHashMapVersionBehavesTheSame() {
		Map<String, Integer> cache = LruCache.withLinkedHashMap(2);
		cache.put("a", 1);
		cache.put("b", 2);
		cache.get("a");
		cache.put("c", 3);

		assertThat(cache).containsOnlyKeys("a", "c");
	}

}
