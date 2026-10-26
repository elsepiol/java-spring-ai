package dojo.web.l3;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleSupplier;

/** Reference solution. */
public final class RetryPolicy {

	public record Failure(int status) {
		public static Failure connectionFailed() {
			return new Failure(0);
		}
	}

	private static final Set<String> IDEMPOTENT = Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE");
	private static final Set<Integer> TRANSIENT = Set.of(0, 429, 502, 503, 504);

	private final int maxAttempts;
	private final Duration baseDelay;
	private final Duration maxDelay;
	private final DoubleSupplier random;

	public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay, DoubleSupplier random) {
		this.maxAttempts = maxAttempts;
		this.baseDelay = baseDelay;
		this.maxDelay = maxDelay;
		this.random = random;
	}

	public boolean shouldRetry(int attempt, String method, boolean hasIdempotencyKey, Failure failure) {
		boolean repeatable = IDEMPOTENT.contains(method) || hasIdempotencyKey;
		return attempt < maxAttempts && repeatable && TRANSIENT.contains(failure.status());
	}

	public Duration delay(int attempt, Optional<Duration> retryAfter) {
		if (retryAfter.isPresent()) {
			return min(retryAfter.get(), maxDelay);
		}
		long exp = baseDelay.toMillis() << Math.min(attempt - 1, 30); // cap the shift: no overflow
		long ceiling = Math.min(maxDelay.toMillis(), exp);
		// Full jitter: clients that failed at the same moment spread out instead of retrying in waves.
		return Duration.ofMillis((long) (random.getAsDouble() * ceiling));
	}

	private static Duration min(Duration a, Duration b) {
		return a.compareTo(b) <= 0 ? a : b;
	}

}
