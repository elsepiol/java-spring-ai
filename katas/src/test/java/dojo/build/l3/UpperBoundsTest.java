package dojo.build.l3;

import static dojo.build.l2.Dep.of;
import static org.assertj.core.api.Assertions.assertThat;

import dojo.build.l2.Dep;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L3")
class UpperBoundsTest {

	@Test
	void aConsistentTreeHasNoViolations() {
		Dep app = of("shop:app:1.0",
				of("x:a:1", of("org.slf4j:slf4j-api:2.0.17")),
				of("org.slf4j:slf4j-api:2.0.17"));
		assertThat(UpperBounds.violations(app)).isEmpty();
	}

	@Test
	void reportsAnOlderVersionWinningByNearness() {
		Dep app = of("shop:app:1.0",
				of("com.google.guava:guava:31.0"),
				of("x:cache-lib:2.3", of("com.google.guava:guava:33.4")));
		assertThat(UpperBounds.violations(app))
				.containsExactly("com.google.guava:guava resolved to 31.0 but x:cache-lib:2.3 requires 33.4");
	}

	@Test
	void comparesVersionsLikeMavenNotLikeText() {
		Dep app = of("shop:app:1.0",
				of("x:jackson:2.9"),
				of("x:lib:1", of("x:jackson:2.10")));
		assertThat(UpperBounds.violations(app)).containsExactly("x:jackson resolved to 2.9 but x:lib:1 requires 2.10");
	}

	@Test
	void anOlderRequestIsFine() {
		Dep app = of("shop:app:1.0",
				of("x:jackson:2.19"),
				of("x:lib:1", of("x:jackson:2.15")));
		assertThat(UpperBounds.violations(app)).isEmpty();
	}

	@Test
	void requestsInsideOmittedSubtreesCount() {
		Dep app = of("shop:app:1.0",
				of("x:a:1"),
				of("y:b:1", of("x:a:2", of("x:c:5"))),     // x:a:2 loses to x:a:1
				of("x:c:4"));                               // x:c 4 wins (depth 1) but x:a:2 asked for 5
		assertThat(UpperBounds.violations(app)).containsExactly(
				"x:a resolved to 1 but y:b:1 requires 2",
				"x:c resolved to 4 but x:a:2 requires 5");
	}

}
