package dojo.testing.l1;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * KATA testing L1 "TrialPeriod" (tracks/10-testing/L1-basics.md, section 4).
 *
 * A free trial lasts 14 days from signup. The rules below already work, but the class reads the
 * real time (Instant.now()), so its tests would have to wait 14 days, or depend on when they run.
 * Make time a dependency: use the injected Clock everywhere, and keep the behaviour:
 * - active: now is before signup + 14 days;
 * - daysLeft: whole days left, rounded UP (13.1 days -> 14), 0 once expired;
 * - inGracePeriod: expired, but by less than 3 days (still allowed to export their data).
 */
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
		return Instant.now().isBefore(signedUpAt.plus(LENGTH));
	}

	public long daysLeft() {
		Duration left = Duration.between(Instant.now(), signedUpAt.plus(LENGTH));
		if (left.isNegative() || left.isZero()) {
			return 0;
		}
		long days = left.toDays();
		return left.equals(Duration.ofDays(days)) ? days : days + 1;
	}

	public boolean inGracePeriod() {
		Instant end = signedUpAt.plus(LENGTH);
		Instant now = Instant.now();
		return !now.isBefore(end) && now.isBefore(end.plus(GRACE));
	}

}
