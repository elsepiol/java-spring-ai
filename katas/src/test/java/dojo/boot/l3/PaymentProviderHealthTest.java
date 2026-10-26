package dojo.boot.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@Tag("boot")
@Tag("L3")
class PaymentProviderHealthTest {

	private static final Duration SLOW = Duration.ofMillis(500);

	@Test
	void fastProviderIsUp() {
		Health h = new PaymentProviderHealth(() -> Duration.ofMillis(42), SLOW).health();
		assertThat(h.getStatus()).isEqualTo(Status.UP);
		assertThat(h.getDetails()).containsEntry("latencyMs", 42L).doesNotContainKey("slow");
	}

	@Test
	void slowProviderIsUpButFlagged() {
		Health h = new PaymentProviderHealth(() -> Duration.ofMillis(900), SLOW).health();
		assertThat(h.getStatus()).isEqualTo(Status.UP);
		assertThat(h.getDetails()).containsEntry("latencyMs", 900L).containsEntry("slow", true);
	}

	@Test
	void unreachableProviderIsDownWithoutInternals() {
		Health h = new PaymentProviderHealth(() -> {
			throw new IllegalStateException("connect timed out");
		}, SLOW).health();
		assertThat(h.getStatus()).isEqualTo(Status.DOWN);
		assertThat(h.getDetails()).containsEntry("error", "connect timed out");
		assertThat(h.getDetails().toString()).doesNotContain("IllegalStateException");
	}

}
