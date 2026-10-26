package dojo.build.l2;

import dojo.Kata;
import java.util.List;
import java.util.Map;

/**
 * KATA build L2 "BuildOrder" + DSA (tracks/03-build-tools/L2-practical.md, section 4: multi-module builds).
 *
 * In a multi-module project ("the reactor"), a module must be built after every module it
 * depends on. Given module -> the modules it depends on, return an order in which to build them.
 * - Topological sort (Kahn's algorithm: repeatedly take a module whose dependencies are all built).
 * - Deterministic: when several modules are ready, take them in alphabetical order.
 * - Every module that appears only as a dependency (no entry of its own) has no dependencies.
 * - A cycle makes a build impossible: throw CycleException whose message shows ONE cycle as a
 *   path that starts and ends at the same module, e.g. "a -> b -> c -> a". (Maven says: "The
 *   projects in the reactor contain a cyclic reference".)
 */
public final class BuildOrder {

	public static final class CycleException extends RuntimeException {
		public CycleException(String message) {
			super(message);
		}
	}

	private BuildOrder() {
	}

	public static List<String> of(Map<String, List<String>> dependsOn) {
		return Kata.todo("tracks/03-build-tools/L2-practical.md, section 4");
	}

}
