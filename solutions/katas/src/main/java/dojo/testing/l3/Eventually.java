package dojo.testing.l3;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Reference solution for the Eventually kata: poll with a deadline instead of one long sleep. */
public final class Eventually {

	private Eventually() {
	}

	public static void await(Duration timeout, Duration pollInterval, BooleanSupplier condition) {
		long deadline = System.nanoTime() + timeout.toNanos();
		RuntimeException last = null;
		while (true) {
			try {
				if (condition.getAsBoolean()) {
					return;
				}
			}
			catch (RuntimeException e) {
				last = e;
			}
			long left = deadline - System.nanoTime();
			if (left <= 0) {
				AssertionError error = new AssertionError("condition not met within " + timeout);
				if (last != null) {
					error.initCause(last);
				}
				throw error;
			}
			try {
				Thread.sleep(Duration.ofNanos(Math.min(left, pollInterval.toNanos())));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("interrupted while waiting", e);
			}
		}
	}

}
