package dojo.docker.l2;

import static dojo.docker.l2.LayerCache.Step.copy;
import static dojo.docker.l2.LayerCache.Step.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.docker.l2.LayerCache.Step;

@Tag("docker")
@Tag("L2")
class LayerCacheTest {

	// COPY everything, then download dependencies: any source change re-downloads the internet.
	private static final List<Step> NAIVE = List.of(
			copy("."),
			run("mvn -B dependency:go-offline"),
			run("mvn -B package"));

	// Dependencies first: only a pom.xml change re-downloads them.
	private static final List<Step> ORDERED = List.of(
			copy("pom.xml"),
			run("mvn -B dependency:go-offline"),
			copy("src/"),
			run("mvn -B package"));

	@Test
	void nothingChangedMeansEverythingIsCached() {
		assertThat(LayerCache.firstRebuiltStep(ORDERED, ORDERED, Set.of())).isEqualTo(4);
	}

	@Test
	void theNaiveOrderRebuildsEverythingOnASourceChange() {
		assertThat(LayerCache.firstRebuiltStep(NAIVE, NAIVE, Set.of("src/main/java/App.java"))).isZero();
	}

	@Test
	void dependenciesFirstKeepsTheDownloadCached() {
		assertThat(LayerCache.firstRebuiltStep(ORDERED, ORDERED, Set.of("src/main/java/App.java"))).isEqualTo(2);
		assertThat(LayerCache.firstRebuiltStep(ORDERED, ORDERED, Set.of("pom.xml"))).isZero();
		assertThat(LayerCache.firstRebuiltStep(ORDERED, ORDERED, Set.of("README.md"))).as("not copied at all").isEqualTo(4);
	}

	@Test
	void directoriesCoverOnlyFilesBelowThem() {
		List<Step> steps = List.of(copy("src/"), run("build"));
		assertThat(LayerCache.firstRebuiltStep(steps, steps, Set.of("srcgen/Foo.java"))).isEqualTo(2);
		assertThat(LayerCache.firstRebuiltStep(steps, steps, Set.of("src/a/b/C.java"))).isZero();
	}

	@Test
	void anEditedInstructionInvalidatesItselfAndEverythingAfter() {
		List<Step> edited = List.of(
				copy("pom.xml"),
				run("mvn -B -U dependency:go-offline"),
				copy("src/"),
				run("mvn -B package"));
		assertThat(LayerCache.firstRebuiltStep(ORDERED, edited, Set.of())).isEqualTo(1);
	}

	@Test
	void newStepsAreBuilt() {
		List<Step> longer = List.of(copy("pom.xml"), run("mvn -B dependency:go-offline"), copy("src/"), run("mvn -B package"),
				run("jdeps --print-module-deps app.jar"));
		assertThat(LayerCache.firstRebuiltStep(ORDERED, longer, Set.of())).isEqualTo(4);
	}

}
