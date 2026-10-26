package dojo.testing.l3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reference solution for the FeatureFlags kata: the state belongs to the instance, not the class. */
public final class FeatureFlags {

	private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

	public FeatureFlags(Map<String, Boolean> defaults) {
		flags.putAll(defaults);
	}

	public boolean isEnabled(String flag) {
		return flags.getOrDefault(flag, false);
	}

	public void enable(String flag) {
		flags.put(flag, true);
	}

	public void disable(String flag) {
		flags.put(flag, false);
	}

}
