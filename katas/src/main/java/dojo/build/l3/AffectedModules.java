package dojo.build.l3;

import dojo.Kata;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KATA build L3 "AffectedModules" + DSA (tracks/03-build-tools/L3-production.md, section 3).
 *
 * A monorepo with 300 modules; a pull request touches 2 of them. Building and testing all 300
 * takes 40 minutes; building only what can be affected takes 4. Which modules can be affected?
 * The changed ones, plus every module that depends on a changed one, directly or transitively
 * (what `mvn -pl core -amd` = "also make dependents" computes, and what Gradle, Bazel and Nx do).
 *
 * Return them in a valid build order (dependencies first; alphabetical among ready modules),
 * WITHOUT modules that cannot be affected. Changed modules that do not exist in the graph are
 * ignored. The graph has no cycles. Hint: reverse the edges, then a BFS from the changed modules.
 */
public final class AffectedModules {

	private AffectedModules() {
	}

	public static List<String> toRebuild(Map<String, List<String>> dependsOn, Set<String> changed) {
		return Kata.todo("tracks/03-build-tools/L3-production.md, section 3");
	}

}
