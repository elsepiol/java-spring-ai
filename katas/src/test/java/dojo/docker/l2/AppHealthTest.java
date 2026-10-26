package dojo.docker.l2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("docker")
@Tag("L2")
class AppHealthTest {

	private final MutableClock clock = new MutableClock();
	private final AppHealth health = new AppHealth(clock, Duration.ofSeconds(30));

	@Test
	void notReadyUntilStarted() {
		assertThat(health.startup()).isEqualTo(Status.DOWN);
		assertThat(health.readiness()).isEqualTo(Status.DOWN);
		assertThat(health.liveness()).as("starting is not broken").isEqualTo(Status.UP);
		health.startupComplete();
		assertThat(health.startup()).isEqualTo(Status.UP);
		assertThat(health.readiness()).isEqualTo(Status.UP);
	}

	@Test
	void aDatabaseOutageTakesThePodOutOfRotationButDoesNotRestartIt() {
		health.startupComplete();
		health.dependencyDown("postgres");
		assertThat(health.readiness()).isEqualTo(Status.DOWN);
		assertThat(health.liveness()).isEqualTo(Status.UP);
		health.dependencyDown("redis");
		health.dependencyUp("postgres");
		assertThat(health.readiness()).as("redis still down").isEqualTo(Status.DOWN);
		health.dependencyUp("redis");
		assertThat(health.readiness()).isEqualTo(Status.UP);
	}

	@Test
	void aStuckWorkerFailsLiveness() {
		health.startupComplete();
		clock.advance(Duration.ofSeconds(30));
		assertThat(health.liveness()).isEqualTo(Status.UP);
		clock.advance(Duration.ofSeconds(1));
		assertThat(health.liveness()).isEqualTo(Status.DOWN);
		health.heartbeat();
		assertThat(health.liveness()).isEqualTo(Status.UP);
	}

	@Test
	void shutdownStopsTrafficButKeepsThePodAlive() {
		health.startupComplete();
		health.shutdownStarted();
		assertThat(health.readiness()).isEqualTo(Status.DOWN);
		assertThat(health.liveness()).as("let it finish in-flight requests").isEqualTo(Status.UP);
	}

	static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-01-01T00:00:00Z");

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
