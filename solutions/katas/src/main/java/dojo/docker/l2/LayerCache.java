package dojo.docker.l2;

import java.util.List;
import java.util.Set;

/** Reference solution for the LayerCache kata. */
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
		for (int i = 0; i < current.size(); i++) {
			Step step = current.get(i);
			if (i >= previous.size() || !previous.get(i).instruction().equals(step.instruction())) {
				return i;
			}
			for (String input : step.inputs()) {
				for (String file : changedFiles) {
					if (covers(input, file)) {
						return i;
					}
				}
			}
		}
		return current.size();
	}

	private static boolean covers(String input, String file) {
		if (input.equals(".") || input.equals("./")) {
			return true;
		}
		return input.endsWith("/") ? file.startsWith(input) : file.equals(input);
	}

}
