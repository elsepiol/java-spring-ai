package dojo.build.l1;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Reference solution: the two tables from the Maven documentation ("Introduction to the Dependency Mechanism"). */
public final class Scopes {

	public enum Scope { COMPILE, PROVIDED, RUNTIME, TEST }

	public enum Classpath { MAIN_COMPILE, MAIN_RUNTIME, TEST_COMPILE, TEST_RUNTIME }

	private Scopes() {
	}

	public static Set<Classpath> classpaths(Scope scope) {
		return switch (scope) {
			case COMPILE -> EnumSet.allOf(Classpath.class);
			// the container (Tomcat, the JDK) supplies it at runtime: compile against it, do not package it
			case PROVIDED -> EnumSet.of(Classpath.MAIN_COMPILE, Classpath.TEST_COMPILE, Classpath.TEST_RUNTIME);
			// needed to run, never referenced in code (a JDBC driver): your code cannot accidentally depend on it
			case RUNTIME -> EnumSet.of(Classpath.MAIN_RUNTIME, Classpath.TEST_RUNTIME);
			case TEST -> EnumSet.of(Classpath.TEST_COMPILE, Classpath.TEST_RUNTIME);
		};
	}

	public static Optional<Scope> transitive(Scope direct, Scope inDependencysPom) {
		// provided and test dependencies of your dependencies are never inherited
		if (inDependencysPom == Scope.PROVIDED || inDependencysPom == Scope.TEST) {
			return Optional.empty();
		}
		return Optional.of(switch (direct) {
			case COMPILE -> inDependencysPom; // compile stays compile, runtime stays runtime
			case PROVIDED -> Scope.PROVIDED;
			case RUNTIME -> Scope.RUNTIME;
			case TEST -> Scope.TEST;
		});
	}

}
