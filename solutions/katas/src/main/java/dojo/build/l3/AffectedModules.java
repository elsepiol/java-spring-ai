package dojo.build.l3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/** Reference solution: reverse graph + BFS for the affected set, then Kahn's algorithm restricted to it. */
public final class AffectedModules {

	private AffectedModules() {
	}

	public static List<String> toRebuild(Map<String, List<String>> dependsOn, Set<String> changed) {
		Set<String> all = new HashSet<>(dependsOn.keySet());
		dependsOn.values().forEach(all::addAll);
		Map<String, List<String>> dependents = new HashMap<>();
		dependsOn.forEach((m, ds) -> ds.forEach(d -> dependents.computeIfAbsent(d, k -> new ArrayList<>()).add(m)));

		Set<String> affected = new HashSet<>();
		Queue<String> queue = new ArrayDeque<>();
		for (String c : changed) {
			if (all.contains(c) && affected.add(c)) {
				queue.add(c);
			}
		}
		while (!queue.isEmpty()) {
			for (String dependent : dependents.getOrDefault(queue.poll(), List.of())) {
				if (affected.add(dependent)) {
					queue.add(dependent);
				}
			}
		}

		Map<String, Integer> missing = new HashMap<>();
		for (String m : affected) {
			missing.put(m, (int) dependsOn.getOrDefault(m, List.of()).stream().filter(affected::contains).count());
		}
		PriorityQueue<String> ready = new PriorityQueue<>();
		missing.forEach((m, n) -> {
			if (n == 0) {
				ready.add(m);
			}
		});
		List<String> order = new ArrayList<>();
		while (!ready.isEmpty()) {
			String m = ready.poll();
			order.add(m);
			for (String dependent : dependents.getOrDefault(m, List.of())) {
				if (affected.contains(dependent) && missing.merge(dependent, -1, Integer::sum) == 0) {
					ready.add(dependent);
				}
			}
		}
		return order;
	}

}
