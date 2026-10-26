package dojo.build.l3;

import dojo.Kata;
import dojo.build.l2.Dep;
import java.util.List;

/**
 * KATA build L3 "UpperBounds" (tracks/03-build-tools/L3-production.md, section 2).
 * Uses your L1 MavenVersion and L2 DependencyMediation: do those first.
 *
 * Nearest-wins can pick a version OLDER than what some library in the tree asked for. That
 * library was compiled against the newer API and fails at runtime with NoSuchMethodError or
 * ClassNotFoundException, often only on one code path, days later. The maven-enforcer rule
 * "requireUpperBoundDeps" fails the build instead. Implement it:
 * for every group:artifact in the tree, if the resolved version (DependencyMediation, no managed
 * versions) is LOWER than the highest version requested anywhere in the tree, report
 *   "group:artifact resolved to R but X requires H"
 * where H is the highest requested version and X the group:artifact:version of the node that
 * declared it (if several do, the first in breadth-first order). Sorted by group:artifact.
 * Requests inside the subtree of an omitted node count too: the library was compiled that way.
 */
public final class UpperBounds {

	private UpperBounds() {
	}

	public static List<String> violations(Dep root) {
		return Kata.todo("tracks/03-build-tools/L3-production.md, section 2");
	}

}
