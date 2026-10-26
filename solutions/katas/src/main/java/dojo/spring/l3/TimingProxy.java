package dojo.spring.l3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

/** Reference solution: a JDK dynamic proxy, which is what Spring AOP uses for interface-based beans. */
public final class TimingProxy {

	private static final Set<String> OBJECT_METHODS = Set.of("toString", "equals", "hashCode");

	private TimingProxy() {
	}

	public static <T> T wrap(T target, Class<T> type, List<String> log) {
		Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (self, method, args) -> {
			boolean record = !(OBJECT_METHODS.contains(method.getName()) && method.getDeclaringClass() == Object.class);
			try {
				Object result = method.invoke(target, args);
				if (record) {
					log.add(method.getName() + " ok");
				}
				return result;
			} catch (InvocationTargetException e) { // reflection wraps whatever the target threw
				if (record) {
					log.add(method.getName() + " failed: " + e.getCause().getClass().getSimpleName());
				}
				throw e.getCause(); // unwrap: callers see exactly what they would without the proxy
			}
		});
		return type.cast(proxy);
	}

}
