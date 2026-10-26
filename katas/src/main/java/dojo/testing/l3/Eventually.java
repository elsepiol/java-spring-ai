package dojo.testing.l3;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * KATA testing L3 "Eventually" (tracks/10-testing/L3-production.md, section 3).
 *
 * Tests of asynchronous code (a message consumer, an @Async method, a cache refresh) often
 * "sleep 2 seconds, then assert". That is slow when the work is fast and flaky when the CI machine
 * is slow. This helper does exactly that; rewrite it to POLL instead (what Awaitility does):
 * - check the condition immediately, then every {@code pollInterval}, and return as soon as it
 *   holds;
 * - give up after {@code timeout}: throw AssertionError("condition not met within <timeout>");
 *   never wait much longer than timeout (at most one extra poll interval);
 * - a condition that THROWS counts as "not yet" (the data may not exist yet); if it never
 *   succeeds, attach the last exception as the AssertionError's cause, so the failure explains
 *   itself;
 * - when the waiting thread is interrupted, restore the interrupt flag and fail at once.
 */
public final class Eventually {

	private Eventually() {
	}

	public static void await(Duration timeout, Duration pollInterval, BooleanSupplier condition) {
		try {
			Thread.sleep(timeout);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (!condition.getAsBoolean()) {
			throw new AssertionError("condition not met within " + timeout);
		}
	}

}
