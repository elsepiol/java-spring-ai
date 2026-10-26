package dojo.build.l2;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

/** Reference solution: BFS; the first visit of a group:artifact wins, and only winners are expanded. */
public final class DependencyMediation {

	private DependencyMediation() {
	}

	public static Map<String, String> resolve(Dep root, Map<String, String> managedVersions) {
		Map<String, String> chosen = new LinkedHashMap<>();
		Queue<Dep> queue = new ArrayDeque<>(root.dependencies()); // BFS = nearest first; list order = declaration order
		while (!queue.isEmpty()) {
			Dep dep = queue.poll();
			if (chosen.containsKey(dep.ga())) {
				continue; // a nearer (or earlier) occurrence already won: omit this one and its subtree
			}
			chosen.put(dep.ga(), managedVersions.getOrDefault(dep.ga(), dep.version()));
			queue.addAll(dep.dependencies());
		}
		return chosen;
	}

}
