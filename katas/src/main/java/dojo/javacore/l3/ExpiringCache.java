package dojo.javacore.l3;

import dojo.Kata;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * KATA javacore L3 "ExpiringCache" (tracks/01-java-core/L3-production.md, sections 4 and 6).
 *
 * "Just put it in a static HashMap" is the most common memory leak in Java services. A cache that
 * is fit for production is BOUNDED (max entries, least recently used evicted first), EXPIRES
 * entries (time to live), and is MEASURABLE (hits, misses, evictions: a cache you cannot measure
 * you cannot tune). This is a small version of what Caffeine does for the shop's product cache.
 *
 * - get(key, loader): a fresh cached value is a hit; otherwise a miss, and loader.apply(key) is
 *   called, stored and returned. An entry older than ttl is not fresh (stale entries count as a miss).
 * - At most maxEntries entries; storing one more evicts the least recently USED (read or written).
 *   Removing a stale entry is not an eviction.
 * - Time comes from the Clock, so tests can move it. Not thread-safe (concurrency track, L3).
 */
public class ExpiringCache<K, V> {

	public record Stats(long hits, long misses, long evictions) {

		public double hitRatio() {
			long total = hits + misses;
			return total == 0 ? 0 : (double) hits / total;
		}

	}

	public ExpiringCache(int maxEntries, Duration ttl, Clock clock) {
	}

	public V get(K key, Function<? super K, ? extends V> loader) {
		return Kata.todo("tracks/01-java-core/L3-production.md, section 6");
	}

	/** The cached value without loading, if present and fresh. Counts as neither hit nor miss. */
	public Optional<V> peek(K key) {
		return Kata.todo("tracks/01-java-core/L3-production.md, section 6");
	}

	public int size() {
		return Kata.todo("tracks/01-java-core/L3-production.md, section 6");
	}

	public Stats stats() {
		return Kata.todo("tracks/01-java-core/L3-production.md, section 6");
	}

}
