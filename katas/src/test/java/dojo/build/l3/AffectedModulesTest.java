package dojo.build.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L3")
class AffectedModulesTest {

	// core <- money <- orders <- shop-app
	// core <- users <- shop-app
	// core <- users <- admin-app
	// reports (independent), docs (independent)
	private static final Map<String, List<String>> MONOREPO = Map.of(
			"core", List.of(),
			"money", List.of("core"),
			"users", List.of("core"),
			"orders", List.of("money", "users"),
			"shop-app", List.of("orders", "users"),
			"admin-app", List.of("users"),
			"reports", List.of(),
			"docs", List.of());

	@Test
	void aLeafChangeRebuildsOnlyItself() {
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("shop-app"))).containsExactly("shop-app");
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("docs"))).containsExactly("docs");
	}

	@Test
	void aChangeRebuildsEveryDependentInBuildOrder() {
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("money"))).containsExactly("money", "orders", "shop-app");
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("users"))).containsExactly("users", "admin-app", "orders", "shop-app");
	}

	@Test
	void aCoreChangeRebuildsEverythingThatUsesCoreButNotTheRest() {
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("core")))
				.containsExactly("core", "money", "users", "admin-app", "orders", "shop-app")
				.doesNotContain("reports", "docs");
	}

	@Test
	void severalChangesAndUnknownModules() {
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of("reports", "money", "deleted-module")))
				.containsExactly("money", "orders", "reports", "shop-app");
	}

	@Test
	void nothingChangedNothingToBuild() {
		assertThat(AffectedModules.toRebuild(MONOREPO, Set.of())).isEmpty();
	}

}
