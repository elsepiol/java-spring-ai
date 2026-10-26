package dojo.web.l3;

import dojo.Kata;
import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * KATA web L3 "CircuitBreaker" (tracks/05-web-http/L3-production.md, section 3).
 *
 * When a dependency is down, calling it anyway wastes threads on timeouts and keeps the dependency
 * from recovering. A circuit breaker stops calling it for a while:
 *
 *   CLOSED    calls go through. After `failureThreshold` CONSECUTIVE failures (exceptions) -> OPEN.
 *             A success resets the count.
 *   OPEN      calls are not made: throw CallNotPermittedException immediately (fail fast, use a
 *             fallback). After `openFor` has passed -> HALF_OPEN.
 *   HALF_OPEN ONE trial call goes through (others arriving meanwhile are rejected): success ->
 *             CLOSED; failure -> OPEN again, for another `openFor`.
 *
 * Exceptions thrown by the call are rethrown unchanged. Time comes from nanoClock. Thread-safe.
 * (Resilience4j is the library; this is its core idea.)
 */
public final class CircuitBreaker {

	public enum State { CLOSED, OPEN, HALF_OPEN }

	public static final class CallNotPermittedException extends RuntimeException {
		public CallNotPermittedException() {
			super("circuit is open");
		}
	}

	public CircuitBreaker(int failureThreshold, Duration openFor, LongSupplier nanoClock) {
	}

	public <T> T call(Supplier<T> call) {
		return Kata.todo("tracks/05-web-http/L3-production.md, section 3");
	}

	public State state() {
		return Kata.todo("tracks/05-web-http/L3-production.md, section 3");
	}

}
