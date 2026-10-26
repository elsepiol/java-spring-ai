package dojo.boot.l3;

import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Reference solution. */
public class PaymentProviderHealth implements HealthIndicator {

	private final Supplier<Duration> ping;
	private final Duration slowThreshold;

	public PaymentProviderHealth(Supplier<Duration> ping, Duration slowThreshold) {
		this.ping = ping;
		this.slowThreshold = slowThreshold;
	}

	@Override
	public Health health() {
		try {
			Duration latency = ping.get();
			Health.Builder up = Health.up().withDetail("latencyMs", latency.toMillis());
			if (latency.compareTo(slowThreshold) >= 0) {
				up.withDetail("slow", true);
			}
			return up.build();
		} catch (RuntimeException e) {
			return Health.down().withDetail("error", String.valueOf(e.getMessage())).build();
		}
	}

}
