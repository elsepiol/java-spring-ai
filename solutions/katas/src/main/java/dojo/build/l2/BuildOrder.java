package dojo.build.l2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Reference solution: Kahn's algorithm with a priority queue (alphabetical), DFS to name a cycle. */
public final class BuildOrder {

	public static final class CycleException extends RuntimeException {
		public CycleException(String message) {
			super(message);
		}
	}

	private BuildOrder() {
	}

	public static List<String> of(Map<String, List<String>> dependsOn) {
		Map<String, Set<String>> deps = new TreeMap<>();
		dependsOn.forEach((module, ds) -> {
			deps.computeIfAbsent(module, k -> new TreeSet<>()).addAll(ds);
			ds.forEach(d -> deps.computeIfAbsent(d, k -> new TreeSet<>()));
		});
		Map<String, Integer> missing = new HashMap<>(); // in-degree: dependencies not built yet
		Map<String, List<String>> dependents = new HashMap<>();
		deps.forEach((m, ds) -> {
			missing.put(m, ds.size());
			ds.forEach(d -> dependents.computeIfAbsent(d, k -> new ArrayList<>()).add(m));
		});
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
				if (missing.merge(dependent, -1, Integer::sum) == 0) {
					ready.add(dependent);
				}
			}
		}
		if (order.size() < deps.size()) {
			throw new CycleException(findCycle(deps, new HashSet<>(order)));
		}
		return order;
	}

	/** Only modules that were never built can be on a cycle. Walk dependencies until a module repeats. */
	private static String findCycle(Map<String, Set<String>> deps, Set<String> built) {
		String start = deps.keySet().stream().filter(m -> !built.contains(m)).findFirst().orElseThrow();
		List<String> path = new ArrayList<>();
		String current = start;
		while (!path.contains(current)) {
			path.add(current);
			current = deps.get(current).stream().filter(d -> !built.contains(d)).findFirst().orElseThrow();
		}
		List<String> cycle = new ArrayList<>(path.subList(path.indexOf(current), path.size()));
		cycle.add(current);
		return String.join(" -> ", cycle);
	}

}
