package dojo.build.l2;

import dojo.Kata;
import java.util.Map;

/**
 * KATA build L2 "DependencyMediation" + DSA (tracks/03-build-tools/L2-practical.md, section 2).
 *
 * Your project depends on A and B; A wants guava 33, B wants guava 31. Only one guava can be on
 * the classpath. Maven picks with "NEAREST WINS", not "newest wins":
 * - walk the tree breadth-first from the root (your project, which is not in the result);
 * - the first time a group:artifact is met decides its version: the occurrence with the
 *   shortest path from the root wins; at equal depth, the one declared first wins;
 * - a losing occurrence is omitted TOGETHER WITH ITS SUBTREE (its dependencies are not visited
 *   from there; they may still be reached through another path).
 * - managed versions (dependencyManagement / an imported BOM) override the version of a
 *   group:artifact wherever it appears; a managed node's own dependencies are still taken from
 *   the node in the tree (we do not model re-reading poms).
 *
 * Returns group:artifact -> chosen version for every dependency on the resolved classpath.
 * Breadth-first search with a queue, plus a "seen" map. This is what `mvn dependency:tree` prints.
 */
public final class DependencyMediation {

	private DependencyMediation() {
	}

	public static Map<String, String> resolve(Dep root, Map<String, String> managedVersions) {
		return Kata.todo("tracks/03-build-tools/L2-practical.md, section 2");
	}

}
