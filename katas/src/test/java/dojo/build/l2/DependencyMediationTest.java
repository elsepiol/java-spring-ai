package dojo.build.l2;

import static dojo.build.l2.Dep.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L2")
class DependencyMediationTest {

	@Test
	void collectsTheWholeTree() {
		Dep app = of("shop:app:1.0",
				of("org.springframework:spring-web:7.0.1", of("org.springframework:spring-core:7.0.1")),
				of("org.postgresql:postgresql:42.7.14"));
		assertThat(DependencyMediation.resolve(app, Map.of())).containsOnly(
				entry("org.springframework:spring-web", "7.0.1"),
				entry("org.springframework:spring-core", "7.0.1"),
				entry("org.postgresql:postgresql", "42.7.14"));
	}

	@Test
	void theNearestVersionWinsEvenIfItIsOlder() {
		// app -> a -> b -> guava 33   (depth 3)
		// app -> c -> guava 31        (depth 2)  wins, although older: a likely NoSuchMethodError in b
		Dep app = of("shop:app:1.0",
				of("x:a:1", of("x:b:1", of("com.google.guava:guava:33.0"))),
				of("x:c:1", of("com.google.guava:guava:31.0")));
		assertThat(DependencyMediation.resolve(app, Map.of())).containsEntry("com.google.guava:guava", "31.0");
	}

	@Test
	void atEqualDepthTheFirstDeclaredWins() {
		Dep app = of("shop:app:1.0",
				of("x:a:1", of("org.slf4j:slf4j-api:2.0.17")),
				of("x:b:1", of("org.slf4j:slf4j-api:1.7.36")));
		assertThat(DependencyMediation.resolve(app, Map.of())).containsEntry("org.slf4j:slf4j-api", "2.0.17");
	}

	@Test
	void aDirectDependencyAlwaysWins() {
		Dep app = of("shop:app:1.0",
				of("x:a:1", of("com.fasterxml:jackson:2.15")),
				of("com.fasterxml:jackson:2.19")); // declare it yourself to choose the version
		assertThat(DependencyMediation.resolve(app, Map.of())).containsEntry("com.fasterxml:jackson", "2.19");
	}

	@Test
	void theSubtreeOfALosingVersionIsOmitted() {
		// netty 4.2 (the loser, deeper) would bring in netty-buffer; netty 4.1 (the winner) does not
		Dep app = of("shop:app:1.0",
				of("io.netty:netty:4.1"),
				of("x:a:1", of("io.netty:netty:4.2", of("io.netty:netty-buffer:4.2"))));
		Map<String, String> resolved = DependencyMediation.resolve(app, Map.of());
		assertThat(resolved).containsEntry("io.netty:netty", "4.1").doesNotContainKey("io.netty:netty-buffer");
	}

	@Test
	void managedVersionsOverrideMediation() {
		Dep app = of("shop:app:1.0",
				of("x:a:1", of("com.google.guava:guava:31.0")),
				of("x:b:1", of("x:c:1", of("com.google.guava:guava:33.0"))));
		Map<String, String> bom = Map.of("com.google.guava:guava", "33.4");
		assertThat(DependencyMediation.resolve(app, bom)).containsEntry("com.google.guava:guava", "33.4").containsEntry("x:a", "1");
	}

}
