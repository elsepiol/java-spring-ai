package dojo.build.l3;

import dojo.build.l1.MavenVersion;
import dojo.build.l2.Dep;
import dojo.build.l2.DependencyMediation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/** Reference solution: resolve with nearest-wins, then BFS over the FULL tree for the highest request. */
public final class UpperBounds {

	private UpperBounds() {
	}

	private record Request(MavenVersion version, String requester) {
	}

	public static List<String> violations(Dep root) {
		Map<String, String> resolved = DependencyMediation.resolve(root, Map.of());
		Map<String, Request> highest = new TreeMap<>();
		Queue<Dep> parents = new ArrayDeque<>(List.of(root));
		while (!parents.isEmpty()) {
			Dep parent = parents.poll();
			for (Dep child : parent.dependencies()) {
				MavenVersion v = MavenVersion.parse(child.version());
				Request best = highest.get(child.ga());
				if (best == null || v.compareTo(best.version()) > 0) {
					highest.put(child.ga(), new Request(v, parent.ga() + ":" + parent.version()));
				}
				parents.add(child);
			}
		}
		List<String> out = new ArrayList<>();
		highest.forEach((ga, request) -> {
			String chosen = resolved.get(ga);
			if (MavenVersion.parse(chosen).compareTo(request.version()) < 0) {
				out.add(ga + " resolved to " + chosen + " but " + request.requester() + " requires " + request.version());
			}
		});
		return out;
	}

}
