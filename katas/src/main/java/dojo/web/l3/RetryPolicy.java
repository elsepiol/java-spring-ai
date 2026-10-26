package dojo.web.l3;

import dojo.Kata;
import java.time.Duration;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * KATA web L3 "RetryPolicy" (tracks/05-web-http/L3-production.md, section 2).
 *
 * Retries turn short blips into successes, and badly designed retries turn a slow service into
 * a dead one (every client tripling its traffic exactly when the server is struggling).
 *
 * shouldRetry(attempt, method, hasIdempotencyKey, failure): attempt is the number of the attempt
 * that just failed (1 = the first call). Retry only if ALL hold:
 * - attempt < maxAttempts;
 * - the request is safe to repeat: an idempotent method (GET, HEAD, OPTIONS, PUT, DELETE), or a
 *   POST/PATCH that carries an Idempotency-Key;
 * - the failure is transient: a connection failure or timeout (status 0 here), 429, 502, 503, 504.
 *   Never 4xx (the same request will fail the same way) and never 500 (a bug, not a blip).
 *
 * delay(attempt, retryAfter): if the server sent Retry-After, wait exactly that (capped at
 * maxDelay). Otherwise exponential backoff with FULL JITTER: a random value in
 * [0, min(maxDelay, baseDelay * 2^(attempt-1))), using the injected random in [0, 1).
 */
public final class RetryPolicy {

	public record Failure(int status) {
		public static Failure connectionFailed() {
			return new Failure(0);
		}
	}

	public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay, DoubleSupplier random) {
	}

	public boolean shouldRetry(int attempt, String method, boolean hasIdempotencyKey, Failure failure) {
		return Kata.todo("tracks/05-web-http/L3-production.md, section 2");
	}

	public Duration delay(int attempt, Optional<Duration> retryAfter) {
		return Kata.todo("tracks/05-web-http/L3-production.md, section 2");
	}

}
