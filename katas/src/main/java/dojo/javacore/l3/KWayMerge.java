package dojo.javacore.l3;

import dojo.Kata;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * KATA javacore L3 "KWayMerge" + DSA (tracks/01-java-core/L3-production.md, section 7).
 *
 * Ten servers each write a log file sorted by time; you need ONE timeline. The files are too big
 * for memory. Merge k already-sorted iterators into one sorted iterator, LAZILY: it reads an input
 * only when the next output element is requested, so memory use is O(k), not O(total).
 * This is the merge step of external sorting, of LSM-tree databases (Cassandra, RocksDB) and of
 * Elasticsearch shard results.
 *
 * Use a PriorityQueue holding the current head of each input: O(log k) per element.
 */
public final class KWayMerge {

	private KWayMerge() {
	}

	public static <T> Iterator<T> merge(List<? extends Iterator<? extends T>> inputs, Comparator<? super T> order) {
		return Kata.todo("tracks/01-java-core/L3-production.md, section 7");
	}

}
