package dojo.spring.l1;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Reference solution: reflection + a DFS that remembers what is being created. */
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

	private final List<Class<?>> registered = new ArrayList<>();
	private final Map<Class<?>, Object> singletons = new HashMap<>();
	private final LinkedHashSet<Class<?>> inCreation = new LinkedHashSet<>(); // insertion order = the path

	public MiniContainer register(Class<?>... types) {
		registered.addAll(List.of(types));
		return this;
	}

	public <T> T get(Class<T> type) {
		Class<?> impl = implementationOf(type);
		Object bean = singletons.get(impl);
		if (bean == null) {
			bean = create(impl);
			singletons.put(impl, bean);
		}
		return type.cast(bean);
	}

	private Class<?> implementationOf(Class<?> type) {
		List<Class<?>> matches = registered.stream().filter(type::isAssignableFrom).toList();
		if (matches.isEmpty()) {
			throw new NoSuchBeanException("No bean of type " + type.getSimpleName() + " is registered");
		}
		if (matches.size() > 1) {
			throw new BeanCreationException("Expected one bean of type " + type.getSimpleName() + " but found "
					+ matches.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")), null);
		}
		return matches.getFirst();
	}

	private Object create(Class<?> impl) {
		if (!inCreation.add(impl)) {
			List<String> path = new ArrayList<>();
			boolean onCycle = false;
			for (Class<?> c : inCreation) {
				onCycle |= c == impl;
				if (onCycle) {
					path.add(c.getSimpleName());
				}
			}
			path.add(impl.getSimpleName());
			throw new BeanCreationException("Circular dependency: " + String.join(" -> ", path), null);
		}
		try {
			Constructor<?>[] constructors = impl.getConstructors();
			if (constructors.length != 1) {
				throw new BeanCreationException(impl.getSimpleName() + " needs exactly one public constructor", null);
			}
			Class<?>[] parameterTypes = constructors[0].getParameterTypes();
			Object[] args = new Object[parameterTypes.length];
			for (int i = 0; i < args.length; i++) {
				args[i] = get(parameterTypes[i]); // recursion: the dependency graph, depth first
			}
			return constructors[0].newInstance(args);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new BeanCreationException("Cannot create " + impl.getSimpleName(), e);
		} finally {
			inCreation.remove(impl);
		}
	}

}
