package dojo.ops.l2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Reference solution for the CommitGraph kata: iterative graph walks over parent links. */
public final class CommitGraph {

	private final Map<String, List<String>> parents = new HashMap<>();

	public void add(String id, String... parentIds) {
		if (parents.containsKey(id)) {
			throw new IllegalArgumentException("duplicate commit " + id);
		}
		for (String p : parentIds) {
			require(p);
		}
		parents.put(id, List.of(parentIds));
	}

	public boolean isAncestor(String ancestor, String descendant) {
		require(ancestor);
		return reachable(descendant).contains(ancestor);
	}

	public Set<String> range(String from, String to) {
		Set<String> result = reachable(to);
		result.removeAll(reachable(from));
		return result;
	}

	public boolean canFastForward(String target, String source) {
		return isAncestor(target, source);
	}

	public Optional<String> mergeBase(String a, String b) {
		Set<String> common = reachable(a);
		common.retainAll(reachable(b));
		// drop every common ancestor that is a proper ancestor of another common ancestor
		Set<String> dominated = new HashSet<>();
		for (String c : common) {
			if (dominated.contains(c)) {
				continue;
			}
			Set<String> below = reachable(c);
			below.remove(c);
			dominated.addAll(below);
		}
		return common.stream().filter(c -> !dominated.contains(c)).sorted().findFirst();
	}

	private Set<String> reachable(String start) {
		require(start);
		Set<String> seen = new HashSet<>();
		Deque<String> todo = new ArrayDeque<>();
		todo.push(start);
		while (!todo.isEmpty()) {
			String id = todo.pop();
			if (seen.add(id)) {
				for (String p : parents.get(id)) {
					if (!seen.contains(p)) {
						todo.push(p);
					}
				}
			}
		}
		return seen;
	}

	private void require(String id) {
		if (!parents.containsKey(id)) {
			throw new IllegalArgumentException("unknown commit " + id);
		}
	}

}
