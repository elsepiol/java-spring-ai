package dojo.javacore.l2;

import dojo.Kata;
import java.util.Map;
import java.util.Optional;

/**
 * KATA javacore L2 "LruCache" + DSA (tracks/01-java-core/L2-practical.md, section 3).
 *
 * A Least-Recently-Used cache holding at most `capacity` entries. When a new key would exceed
 * the capacity, the entry that was used (get or put) longest ago is evicted.
 *
 * Part 1: implement it FROM SCRATCH with a HashMap plus your own doubly linked list, so that
 *         get and put are O(1). (The classic interview question: no LinkedHashMap here.)
 * Part 2: implement withLinkedHashMap(): the same behaviour in about 10 lines, the way you would
 *         do it at work when a library like Caffeine is not available.
 */
public class LruCache<K, V> {

	public LruCache(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be at least 1");
		}
	}

	/** The value, and the entry becomes the most recently used. Empty if absent. */
	public Optional<V> get(K key) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 3");
	}

	/** Adds or replaces; either way the entry becomes the most recently used. May evict one entry. */
	public void put(K key, V value) {
		Kata.todo("tracks/01-java-core/L2-practical.md, section 3");
	}

	public int size() {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 3");
	}

	/** Part 2: a java.util.Map that behaves as an LRU cache of the given capacity. */
	public static <K, V> Map<K, V> withLinkedHashMap(int capacity) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 3 (LinkedHashMap's accessOrder and removeEldestEntry)");
	}

}
