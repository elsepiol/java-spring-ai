package dojo.testing.l3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KATA testing L3 "FeatureFlags" (find the bug) (tracks/10-testing/L3-production.md, section 4).
 *
 * Each test creates its own FeatureFlags, yet FeatureFlagsTest passes or fails depending on the
 * ORDER in which its tests run (it fixes the order to make the failure reproducible). Find the
 * shared state that leaks from one test into the next, and remove it without changing the API.
 */
public final class FeatureFlags {

	private static final Map<String, Boolean> FLAGS = new ConcurrentHashMap<>();

	public FeatureFlags(Map<String, Boolean> defaults) {
		defaults.forEach(FLAGS::putIfAbsent);
	}

	public boolean isEnabled(String flag) {
		return FLAGS.getOrDefault(flag, false);
	}

	public void enable(String flag) {
		FLAGS.put(flag, true);
	}

	public void disable(String flag) {
		FLAGS.put(flag, false);
	}

}
