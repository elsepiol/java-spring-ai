package dojo.testing.l1;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Reference solution for the TrialPeriod kata: every read of the time goes through the Clock. */
public final class TrialPeriod {

	public static final Duration LENGTH = Duration.ofDays(14);
	public static final Duration GRACE = Duration.ofDays(3);

	private final Instant signedUpAt;
	private final Clock clock;

	public TrialPeriod(Instant signedUpAt, Clock clock) {
		this.signedUpAt = signedUpAt;
		this.clock = clock;
	}

	public boolean isActive() {
		return clock.instant().isBefore(signedUpAt.plus(LENGTH));
	}

	public long daysLeft() {
		Duration left = Duration.between(clock.instant(), signedUpAt.plus(LENGTH));
		if (left.isNegative() || left.isZero()) {
			return 0;
		}
		long days = left.toDays();
		return left.equals(Duration.ofDays(days)) ? days : days + 1;
	}

	public boolean inGracePeriod() {
		Instant end = signedUpAt.plus(LENGTH);
		Instant now = clock.instant();
		return !now.isBefore(end) && now.isBefore(end.plus(GRACE));
	}

}
