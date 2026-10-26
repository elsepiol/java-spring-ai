package dojo.security.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("security")
@Tag("L3")
class LoginThrottleTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
	private final LoginThrottle throttle = new LoginThrottle(clock, 5, Duration.ofMinutes(15), Duration.ofMinutes(10));

	@Test
	void locksAfterTooManyFailures() {
		for (int i = 0; i < 4; i++) {
			throttle.recordFailure("alice");
		}
		assertThat(throttle.isLocked("alice")).isFalse();
		throttle.recordFailure("alice");
		assertThat(throttle.isLocked("alice")).isTrue();
		assertThat(throttle.retryAfter("alice")).isEqualTo(Duration.ofMinutes(10));
		assertThat(throttle.isLocked("bob")).isFalse();
		assertThat(throttle.retryAfter("bob")).isEqualTo(Duration.ZERO);
	}

	@Test
	void unlocksWhenTheLockoutEnds() {
		for (int i = 0; i < 5; i++) {
			throttle.recordFailure("alice");
		}
		clock.advance(Duration.ofMinutes(9));
		assertThat(throttle.retryAfter("alice")).isEqualTo(Duration.ofMinutes(1));
		clock.advance(Duration.ofMinutes(1));
		assertThat(throttle.isLocked("alice")).isFalse();
		throttle.recordFailure("alice"); // a fresh start, not "still at 5"
		assertThat(throttle.isLocked("alice")).isFalse();
	}

	@Test
	void forgetsFailuresOlderThanTheWindow() {
		for (int i = 0; i < 4; i++) {
			throttle.recordFailure("alice");
		}
		clock.advance(Duration.ofMinutes(16));
		throttle.recordFailure("alice");
		assertThat(throttle.isLocked("alice")).isFalse();
	}

	@Test
	void slowGuessingWithinTheWindowStillLocks() {
		for (int i = 0; i < 5; i++) {
			throttle.recordFailure("alice");
			clock.advance(Duration.ofMinutes(3)); // 5 failures spread over 12 minutes
		}
		assertThat(throttle.isLocked("alice")).isTrue();
	}

	@Test
	void successClearsFailures() {
		for (int i = 0; i < 4; i++) {
			throttle.recordFailure("alice");
		}
		throttle.recordSuccess("alice");
		throttle.recordFailure("alice");
		assertThat(throttle.isLocked("alice")).isFalse();
	}

	@Test
	void successDoesNotUnlockALockedAccount() {
		for (int i = 0; i < 5; i++) {
			throttle.recordFailure("alice");
		}
		throttle.recordSuccess("alice");
		assertThat(throttle.isLocked("alice")).isTrue();
	}

	@Test
	void spellingsOfOneNameShareTheirFailures() {
		throttle.recordFailure("alice");
		throttle.recordFailure("Alice");
		throttle.recordFailure("ALICE");
		throttle.recordFailure(" alice");
		throttle.recordFailure("alice ");
		assertThat(throttle.isLocked("aLiCe")).isTrue();
	}

	@Test
	void isThreadSafe() throws Exception {
		LoginThrottle big = new LoginThrottle(clock, 4_000, Duration.ofMinutes(15), Duration.ofMinutes(10));
		CountDownLatch start = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < 8; t++) {
			threads.add(Thread.ofPlatform().start(() -> {
				try {
					start.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				for (int i = 0; i < 500; i++) {
					big.recordFailure("alice");
				}
			}));
		}
		start.countDown();
		for (Thread t : threads) {
			t.join();
		}
		assertThat(big.isLocked("alice")).as("8 x 500 = 4000 failures must reach the limit exactly").isTrue();
	}

	static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration d) {
			now = now.plus(d);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
