package dojo.docker.l3;

import java.util.ArrayList;
import java.util.List;

/** Reference solution for the RollingUpdate kata. */
public final class RollingUpdate {

	public record State(int oldPods, int newPods) {
	}

	private RollingUpdate() {
	}

	public static List<State> plan(int replicas, int maxSurge, int maxUnavailable) {
		if (replicas < 0 || maxSurge < 0 || maxUnavailable < 0 || (maxSurge == 0 && maxUnavailable == 0)) {
			throw new IllegalArgumentException("maxSurge and maxUnavailable must not both be 0");
		}
		int oldPods = replicas;
		int newPods = 0;
		int minAvailable = replicas - maxUnavailable;
		List<State> steps = new ArrayList<>();
		while (oldPods > 0 || newPods < replicas) {
			int create = Math.max(0, Math.min(replicas - newPods, replicas + maxSurge - (oldPods + newPods)));
			newPods += create;
			int delete = Math.max(0, Math.min(oldPods, oldPods + newPods - minAvailable));
			oldPods -= delete;
			steps.add(new State(oldPods, newPods));
		}
		return steps;
	}

	public static int[] resolve(int replicas, String maxSurge, String maxUnavailable) {
		return new int[] { amount(replicas, maxSurge, true), amount(replicas, maxUnavailable, false) };
	}

	private static int amount(int replicas, String value, boolean roundUp) {
		if (value.endsWith("%")) {
			int percent = Integer.parseInt(value.substring(0, value.length() - 1));
			int scaled = replicas * percent;
			return roundUp ? (scaled + 99) / 100 : scaled / 100;
		}
		return Integer.parseInt(value);
	}

}
