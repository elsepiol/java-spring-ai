package dojo.build.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L2")
class BuildOrderTest {

	@Test
	void buildsDependenciesFirst() {
		List<String> order = BuildOrder.of(Map.of(
				"app", List.of("web", "persistence"),
				"web", List.of("core"),
				"persistence", List.of("core"),
				"core", List.of()));
		assertThat(order).containsExactly("core", "persistence", "web", "app");
	}

	@Test
	void modulesOnlyMentionedAsDependenciesAreIncluded() {
		assertThat(BuildOrder.of(Map.of("app", List.of("lib")))).containsExactly("lib", "app");
	}

	@Test
	void independentModulesAreAlphabetical() {
		assertThat(BuildOrder.of(Map.of("zeta", List.of(), "alpha", List.of(), "mid", List.of()))).containsExactly("alpha", "mid", "zeta");
	}

	@Test
	void aCycleIsReportedAsAPath() {
		Map<String, List<String>> modules = Map.of(
				"app", List.of("a"),
				"a", List.of("b"),
				"b", List.of("c"),
				"c", List.of("a"),
				"util", List.of());
		assertThatThrownBy(() -> BuildOrder.of(modules))
				.isInstanceOf(BuildOrder.CycleException.class)
				.hasMessageContainingAll("a -> b", "b -> c", "c -> a");
	}

	@Test
	void aModuleDependingOnItselfIsACycle() {
		assertThatThrownBy(() -> BuildOrder.of(Map.of("a", List.of("a")))).isInstanceOf(BuildOrder.CycleException.class).hasMessage("a -> a");
	}

	@Test
	void scalesToALargeGraph() {
		Map<String, List<String>> chain = new java.util.HashMap<>();
		for (int i = 1; i < 20_000; i++) {
			chain.put(String.format("m%05d", i), List.of(String.format("m%05d", i - 1)));
		}
		List<String> order = BuildOrder.of(chain);
		assertThat(order).hasSize(20_000).startsWith("m00000", "m00001").endsWith("m19999");
	}

}
