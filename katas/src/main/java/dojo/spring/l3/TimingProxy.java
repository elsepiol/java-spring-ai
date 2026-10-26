package dojo.spring.l3;

import dojo.Kata;
import java.util.List;

/**
 * KATA spring L3 "TimingProxy" (tracks/06-spring-core/L3-production.md, section 1: proxies).
 *
 * @Transactional, @Cacheable, @Async, @PreAuthorize and @Retryable all work the same way: Spring
 * hands out a PROXY that implements the same interface, runs extra code around each call, and
 * delegates to your object. Build one with java.lang.reflect.Proxy:
 *
 * wrap(target, type, log) returns an object implementing `type` that, for every interface method:
 * - calls the same method on target with the same arguments and returns its result;
 * - records "<methodName> ok" or "<methodName> failed: <ExceptionSimpleName>" in log;
 * - rethrows the ORIGINAL exception the target threw (not an InvocationTargetException, and not
 *   an UndeclaredThrowableException for checked exceptions the method declares).
 * toString/equals/hashCode of the proxy are not recorded (delegate them silently).
 */
public final class TimingProxy {

	private TimingProxy() {
	}

	public static <T> T wrap(T target, Class<T> type, List<String> log) {
		return Kata.todo("tracks/06-spring-core/L3-production.md, section 1");
	}

}
