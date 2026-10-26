package dojo.build.l1;

import static dojo.build.l1.Scopes.Classpath.MAIN_COMPILE;
import static dojo.build.l1.Scopes.Classpath.MAIN_RUNTIME;
import static dojo.build.l1.Scopes.Classpath.TEST_COMPILE;
import static dojo.build.l1.Scopes.Classpath.TEST_RUNTIME;
import static dojo.build.l1.Scopes.Scope.COMPILE;
import static dojo.build.l1.Scopes.Scope.PROVIDED;
import static dojo.build.l1.Scopes.Scope.RUNTIME;
import static dojo.build.l1.Scopes.Scope.TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L1")
class ScopesTest {

	@Test
	void compileIsEverywhere() {
		assertThat(Scopes.classpaths(COMPILE)).containsExactlyInAnyOrder(MAIN_COMPILE, MAIN_RUNTIME, TEST_COMPILE, TEST_RUNTIME);
	}

	@Test
	void providedIsNotPackaged() {
		// e.g. jakarta.servlet-api in a war: Tomcat has it
		assertThat(Scopes.classpaths(PROVIDED)).containsExactlyInAnyOrder(MAIN_COMPILE, TEST_COMPILE, TEST_RUNTIME);
	}

	@Test
	void runtimeIsNotVisibleToTheCompiler() {
		// e.g. the PostgreSQL JDBC driver: your code talks to java.sql, not to org.postgresql
		assertThat(Scopes.classpaths(RUNTIME)).containsExactlyInAnyOrder(MAIN_RUNTIME, TEST_RUNTIME);
	}

	@Test
	void testIsOnlyForTests() {
		assertThat(Scopes.classpaths(TEST)).containsExactlyInAnyOrder(TEST_COMPILE, TEST_RUNTIME);
	}

	@Test
	void transitiveScopes() {
		assertThat(Scopes.transitive(COMPILE, COMPILE)).contains(COMPILE);
		assertThat(Scopes.transitive(COMPILE, RUNTIME)).contains(RUNTIME);
		assertThat(Scopes.transitive(TEST, COMPILE)).contains(TEST);
		assertThat(Scopes.transitive(TEST, RUNTIME)).contains(TEST);
		assertThat(Scopes.transitive(PROVIDED, COMPILE)).contains(PROVIDED);
		assertThat(Scopes.transitive(RUNTIME, COMPILE)).contains(RUNTIME);
	}

	@Test
	void testAndProvidedDependenciesOfYourDependenciesAreNotInherited() {
		for (Scopes.Scope direct : Scopes.Scope.values()) {
			assertThat(Scopes.transitive(direct, TEST)).as("%s -> test", direct).isEmpty();
			assertThat(Scopes.transitive(direct, PROVIDED)).as("%s -> provided", direct).isEmpty();
		}
	}

}
