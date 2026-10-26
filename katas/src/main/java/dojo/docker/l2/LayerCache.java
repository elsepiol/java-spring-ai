package dojo.docker.l2;

import java.util.List;
import java.util.Set;

import dojo.Kata;

/**
 * KATA docker L2 "LayerCache" (tracks/11-docker-deploy/L2-practical.md, section 1).
 *
 * Model Docker's build cache. Each step has its instruction text and the files it copies in.
 * A step's cached layer can be reused only if
 * - the step at the same position in the previous build had the same instruction text, and
 * - none of its input files changed (an input "src/" or "." covers every file below it; any
 *   other input is one exact file), and
 * - every step before it was reused too (a layer sits on top of its parent: one miss rebuilds
 *   everything after it).
 * firstRebuiltStep returns the index of the first step that must run again, or current.size()
 * when the whole build comes from the cache.
 */
public final class LayerCache {

	public record Step(String instruction, Set<String> inputs) {

		public static Step run(String command) {
			return new Step("RUN " + command, Set.of());
		}

		public static Step copy(String... inputs) {
			return new Step("COPY " + String.join(" ", inputs), Set.of(inputs));
		}

	}

	private LayerCache() {
	}

	public static int firstRebuiltStep(List<Step> previous, List<Step> current, Set<String> changedFiles) {
		return Kata.todo("tracks/11-docker-deploy/L2-practical.md section 1");
	}

}
