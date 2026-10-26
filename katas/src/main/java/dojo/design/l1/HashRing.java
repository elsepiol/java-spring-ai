package dojo.design.l1;

import dojo.Kata;

/**
 * KATA design L1 "HashRing" (tracks/15-system-design/L1-basics.md, section 4).
 *
 * Consistent hashing, as used to spread keys over cache nodes or database shards. With
 * `hash(key) % n`, adding a node moves almost every key; on a ring, only about 1/n of them move.
 * - each node is placed on the ring {@code virtualNodes} times, at hash(node + "#" + i) (virtual
 *   nodes even out the load);
 * - a key belongs to the first virtual node clockwise from hash(key), wrapping around;
 * - use {@link #hash(String)} for both (given; do not change it);
 * - nodeFor on an empty ring -> IllegalStateException; adding an existing node or removing an
 *   unknown one -> IllegalArgumentException.
 * Lookups must be O(log n) (a sorted structure, not a scan).
 */
public final class HashRing {

	public HashRing(int virtualNodes) {
		Kata.todo("tracks/15-system-design/L1-basics.md section 4");
	}

	public void add(String node) {
		Kata.todo("tracks/15-system-design/L1-basics.md section 4");
	}

	public void remove(String node) {
		Kata.todo("tracks/15-system-design/L1-basics.md section 4");
	}

	public String nodeFor(String key) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 4");
	}

	/** 64-bit FNV-1a, then a finaliser so similar strings land far apart. */
	public static long hash(String s) {
		long h = 0xcbf29ce484222325L;
		for (int i = 0; i < s.length(); i++) {
			h ^= s.charAt(i);
			h *= 0x100000001b3L;
		}
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return h;
	}

}
