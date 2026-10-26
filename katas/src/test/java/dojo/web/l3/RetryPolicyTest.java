package dojo.web.l3;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.web.l3.RetryPolicy.Failure;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L3")
class RetryPolicyTest {

	private double nextRandom = 0.5;
	private final RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(2), () -> nextRandom);

	@Test
	void retriesTransientFailuresOfIdempotentRequests() {
		for (int status : new int[] { 0, 429, 502, 503, 504 }) {
			assertThat(policy.shouldRetry(1, "GET", false, new Failure(status))).as("status %d", status).isTrue();
		}
		assertThat(policy.shouldRetry(1, "PUT", false, Failure.connectionFailed())).isTrue();
		assertThat(policy.shouldRetry(1, "DELETE", false, new Failure(503))).isTrue();
	}

	@Test
	void neverRetriesClientErrorsOrBugs() {
		for (int status : new int[] { 400, 401, 403, 404, 409, 422, 500 }) {
			assertThat(policy.shouldRetry(1, "GET", false, new Failure(status))).as("status %d", status).isFalse();
		}
	}

	@Test
	void postIsRetriedOnlyWithAnIdempotencyKey() {
		assertThat(policy.shouldRetry(1, "POST", false, Failure.connectionFailed())).as("could create a second order").isFalse();
		assertThat(policy.shouldRetry(1, "POST", true, Failure.connectionFailed())).isTrue();
		assertThat(policy.shouldRetry(1, "PATCH", false, new Failure(503))).isFalse();
	}

	@Test
	void stopsAfterMaxAttempts() {
		assertThat(policy.shouldRetry(2, "GET", false, new Failure(503))).isTrue();
		assertThat(policy.shouldRetry(3, "GET", false, new Failure(503))).isFalse();
	}

	@Test
	void exponentialBackoffWithFullJitter() {
		nextRandom = 0.5;
		assertThat(policy.delay(1, Optional.empty())).isEqualTo(Duration.ofMillis(50));   // half of 100
		assertThat(policy.delay(2, Optional.empty())).isEqualTo(Duration.ofMillis(100));  // half of 200
		assertThat(policy.delay(3, Optional.empty())).isEqualTo(Duration.ofMillis(200));  // half of 400
		nextRandom = 0.0;
		assertThat(policy.delay(3, Optional.empty())).isEqualTo(Duration.ZERO);
		nextRandom = 0.999;
		assertThat(policy.delay(3, Optional.empty())).isEqualTo(Duration.ofMillis(399));
	}

	@Test
	void backoffIsCapped() {
		nextRandom = 0.5;
		assertThat(policy.delay(10, Optional.empty())).isEqualTo(Duration.ofSeconds(1)); // half of the 2 s cap
		assertThat(policy.delay(64, Optional.empty())).isEqualTo(Duration.ofSeconds(1)); // no overflow
	}

	@Test
	void retryAfterFromTheServerWins() {
		assertThat(policy.delay(1, Optional.of(Duration.ofMillis(1500)))).isEqualTo(Duration.ofMillis(1500));
		assertThat(policy.delay(1, Optional.of(Duration.ofMinutes(5)))).as("capped").isEqualTo(Duration.ofSeconds(2));
	}

}
