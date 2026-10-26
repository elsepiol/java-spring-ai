package dojo.javacore.l3;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Reference solution: an access-ordered LinkedHashMap plus a write timestamp per entry. */
public class ExpiringCache<K, V> {

	public record Stats(long hits, long misses, long evictions) {

		public double hitRatio() {
			long total = hits + misses;
			return total == 0 ? 0 : (double) hits / total;
		}

	}

	private record Entry<V>(V value, Instant writtenAt) {
	}

	private final int maxEntries;
	private final Duration ttl;
	private final Clock clock;
	private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);
	private long hits;
	private long misses;
	private long evictions;

	public ExpiringCache(int maxEntries, Duration ttl, Clock clock) {
		if (maxEntries < 1) {
			throw new IllegalArgumentException("maxEntries must be at least 1");
		}
		this.maxEntries = maxEntries;
		this.ttl = ttl;
		this.clock = clock;
	}

	public V get(K key, Function<? super K, ? extends V> loader) {
		Entry<V> e = entries.get(key); // also marks it recently used
		if (e != null && isFresh(e)) {
			hits++;
			return e.value();
		}
		misses++;
		if (e != null) {
			entries.remove(key); // stale: expiry, not eviction
		}
		V value = loader.apply(key);
		entries.put(key, new Entry<>(value, clock.instant()));
		evictIfNeeded();
		return value;
	}

	public Optional<V> peek(K key) {
		Entry<V> e = entries.get(key);
		return e != null && isFresh(e) ? Optional.of(e.value()) : Optional.empty();
	}

	public int size() {
		return entries.size();
	}

	public Stats stats() {
		return new Stats(hits, misses, evictions);
	}

	private boolean isFresh(Entry<V> e) {
		return clock.instant().isBefore(e.writtenAt().plus(ttl));
	}

	private void evictIfNeeded() {
		Iterator<Map.Entry<K, Entry<V>>> eldestFirst = entries.entrySet().iterator();
		while (entries.size() > maxEntries && eldestFirst.hasNext()) {
			eldestFirst.next();
			eldestFirst.remove();
			evictions++;
		}
	}

}
