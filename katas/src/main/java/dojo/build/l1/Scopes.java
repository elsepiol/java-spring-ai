package dojo.build.l1;

import dojo.Kata;
import java.util.Optional;
import java.util.Set;

/**
 * KATA build L1 "Scopes" (tracks/03-build-tools/L1-basics.md, section 5: dependency scopes).
 *
 * Maven puts each dependency on some classpaths and not others, depending on its scope; and a
 * dependency of a dependency gets a scope computed from both. Get these tables right and most
 * "it compiles but fails at runtime" (or "it is in my jar but should not be") puzzles disappear.
 *
 * classpaths(): on which classpaths a DIRECT dependency with this scope is.
 * transitive(): the scope a transitive dependency ends up with, given the scope of the direct
 *   dependency that brought it in (rows) and the scope it has in that dependency's own pom
 *   (columns); empty when it is not inherited at all.
 */
public final class Scopes {

	public enum Scope { COMPILE, PROVIDED, RUNTIME, TEST }

	public enum Classpath {
		/** compiling src/main/java */
		MAIN_COMPILE,
		/** running the application: what is packaged into the jar/war (or must be on the server) */
		MAIN_RUNTIME,
		/** compiling src/test/java */
		TEST_COMPILE,
		/** running the tests */
		TEST_RUNTIME
	}

	private Scopes() {
	}

	public static Set<Classpath> classpaths(Scope scope) {
		return Kata.todo("tracks/03-build-tools/L1-basics.md, section 5");
	}

	public static Optional<Scope> transitive(Scope direct, Scope inDependencysPom) {
		return Kata.todo("tracks/03-build-tools/L1-basics.md, section 5 (the transitive scope table)");
	}

}
