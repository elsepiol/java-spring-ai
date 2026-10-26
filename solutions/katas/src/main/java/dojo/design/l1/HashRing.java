package dojo.design.l1;

import java.util.Map;
import java.util.TreeMap;

/** Reference solution for the HashRing kata: a TreeMap from ring position to node. */
public final class HashRing {

	private final int virtualNodes;
	private final TreeMap<Long, String> ring = new TreeMap<>();
	private final java.util.Set<String> nodes = new java.util.HashSet<>();

	public HashRing(int virtualNodes) {
		this.virtualNodes = virtualNodes;
	}

	public void add(String node) {
		if (!nodes.add(node)) {
			throw new IllegalArgumentException("already on the ring: " + node);
		}
		for (int i = 0; i < virtualNodes; i++) {
			ring.put(hash(node + "#" + i), node);
		}
	}

	public void remove(String node) {
		if (!nodes.remove(node)) {
			throw new IllegalArgumentException("not on the ring: " + node);
		}
		for (int i = 0; i < virtualNodes; i++) {
			ring.remove(hash(node + "#" + i), node);
		}
	}

	public String nodeFor(String key) {
		if (ring.isEmpty()) {
			throw new IllegalStateException("no nodes");
		}
		Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));
		return (e != null ? e : ring.firstEntry()).getValue();
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
