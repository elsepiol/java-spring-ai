package dojo.spring.l1;

import dojo.Kata;

/**
 * KATA spring L1 "MiniContainer" + DSA (tracks/06-spring-core/L1-basics.md, sections 2-3).
 *
 * Build the core of Spring yourself: an IoC container with constructor injection.
 * - register(type...): concrete classes the container may create ("beans").
 * - get(type): the instance for that type. `type` may be the class itself, or an interface or
 *   superclass that EXACTLY ONE registered class implements.
 * - Each class must have exactly one public constructor; its parameters are resolved with get()
 *   (recursively): that is constructor injection.
 * - Singletons: every bean is created once; get() always returns the same instance, and two
 *   beans that need the same dependency share it.
 * - Errors (all NoSuchBeanException / BeanCreationException with a helpful message):
 *     nothing registered for a type                    -> NoSuchBeanException naming the type
 *     two registered classes match an interface        -> BeanCreationException naming both
 *     a dependency cycle A -> B -> A                   -> BeanCreationException with "A -> B -> A"
 *       (use the simple class names; detect it with the set of beans "currently in creation": DFS)
 */
public final class MiniContainer {

	public static class NoSuchBeanException extends RuntimeException {
		public NoSuchBeanException(String message) {
			super(message);
		}
	}

	public static class BeanCreationException extends RuntimeException {
		public BeanCreationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public MiniContainer register(Class<?>... types) {
		return Kata.todo("tracks/06-spring-core/L1-basics.md, section 3");
	}

	public <T> T get(Class<T> type) {
		return Kata.todo("tracks/06-spring-core/L1-basics.md, section 3");
	}

}
