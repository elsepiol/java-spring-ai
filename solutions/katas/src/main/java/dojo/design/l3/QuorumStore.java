package dojo.design.l3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reference solution for the QuorumStore kata. */
public final class QuorumStore {

	public static final class QuorumException extends RuntimeException {

		public QuorumException(String message) {
			super(message);
		}

	}

	private record Versioned(String value, long version) {
	}

	private final int w;
	private final int r;
	private final List<Map<String, Versioned>> replicas = new ArrayList<>();
	private final boolean[] up;

	public QuorumStore(int n, int w, int r) {
		if (n < 1 || w < 1 || r < 1 || w > n || r > n) {
			throw new IllegalArgumentException("need 1 <= W, R <= N");
		}
		this.w = w;
		this.r = r;
		this.up = new boolean[n];
		for (int i = 0; i < n; i++) {
			replicas.add(new HashMap<>());
			up[i] = true;
		}
	}

	public void setUp(int replica, boolean isUp) {
		up[replica] = isUp;
	}

	public void put(String key, String value) {
		List<Integer> live = live();
		long version = live.stream().mapToLong(i -> versionAt(i, key)).max().orElse(0) + 1;
		for (int i : live) {
			replicas.get(i).put(key, new Versioned(value, version));
		}
		if (live.size() < w) {
			throw new QuorumException("only " + live.size() + " of " + w + " required replicas acknowledged");
		}
	}

	public Optional<String> get(String key) {
		List<Integer> live = live();
		if (live.size() < r) {
			throw new QuorumException("only " + live.size() + " of " + r + " required replicas answered");
		}
		List<Integer> asked = live.subList(0, r);
		Versioned newest = null;
		for (int i : asked) {
			Versioned v = replicas.get(i).get(key);
			if (v != null && (newest == null || v.version() > newest.version())) {
				newest = v;
			}
		}
		if (newest == null) {
			return Optional.empty();
		}
		for (int i : asked) {
			if (versionAt(i, key) < newest.version()) {
				replicas.get(i).put(key, newest); // read repair
			}
		}
		return Optional.of(newest.value());
	}

	public long versionAt(int replica, String key) {
		Versioned v = replicas.get(replica).get(key);
		return v == null ? 0 : v.version();
	}

	private List<Integer> live() {
		List<Integer> live = new ArrayList<>();
		for (int i = 0; i < up.length; i++) {
			if (up[i]) {
				live.add(i);
			}
		}
		return live;
	}

}
