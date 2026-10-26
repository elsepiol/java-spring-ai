package dojo.testing.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("testing")
@Tag("L1")
class TrialPeriodTest {

	private static final Instant SIGNUP = Instant.parse("2026-03-01T12:00:00Z");

	private static TrialPeriod at(String now) {
		return new TrialPeriod(SIGNUP, Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
	}

	@Test
	void isActiveRightAfterSignup() {
		assertThat(at("2026-03-01T12:00:00Z").isActive()).isTrue();
		assertThat(at("2026-03-01T12:00:00Z").daysLeft()).isEqualTo(14);
	}

	@Test
	void expiresExactly14DaysLater() {
		assertThat(at("2026-03-15T11:59:59Z").isActive()).isTrue();
		assertThat(at("2026-03-15T12:00:00Z").isActive()).isFalse();
	}

	@ParameterizedTest(name = "at {0} -> {1} days left")
	@CsvSource({
			"2026-03-01T12:00:01Z, 14",
			"2026-03-02T12:00:00Z, 13",
			"2026-03-14T12:00:01Z, 1",
			"2026-03-15T12:00:00Z, 0",
			"2026-04-01T00:00:00Z, 0" })
	void countsDaysLeftRoundingUp(String now, long expected) {
		assertThat(at(now).daysLeft()).isEqualTo(expected);
	}

	@Test
	void hasAThreeDayGracePeriod() {
		assertThat(at("2026-03-15T11:00:00Z").inGracePeriod()).as("still active").isFalse();
		assertThat(at("2026-03-15T12:00:00Z").inGracePeriod()).isTrue();
		assertThat(at("2026-03-18T11:59:59Z").inGracePeriod()).isTrue();
		assertThat(at("2026-03-18T12:00:00Z").inGracePeriod()).isFalse();
	}

	@Test
	void aTickingClockMovesTheTrialForward() {
		Clock start = Clock.fixed(SIGNUP, ZoneOffset.UTC);
		TrialPeriod trial = new TrialPeriod(SIGNUP, Clock.offset(start, Duration.ofDays(20)));
		assertThat(trial.isActive()).isFalse();
		assertThat(trial.inGracePeriod()).isFalse();
	}

}
