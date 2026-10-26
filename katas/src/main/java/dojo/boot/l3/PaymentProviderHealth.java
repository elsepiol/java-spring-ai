package dojo.boot.l3;

import dojo.Kata;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * KATA boot L3 "PaymentProviderHealth" (tracks/07-spring-boot-web/L3-production.md, section 3: health).
 *
 * /actuator/health aggregates HealthIndicators. Write one for the payment provider. `ping` calls the
 * provider and returns how long it took (it throws if the provider is unreachable):
 * - reachable and faster than `slowThreshold`: UP, detail "latencyMs" = the latency in ms
 * - reachable but slow:                         UP, details "latencyMs" and "slow" = true
 * - ping throws:                                DOWN, detail "error" = the exception's message
 *                                              (never the stack trace or class: health may be public)
 * Think about it: should this indicator be part of the READINESS group (L3 lesson)?
 */
public class PaymentProviderHealth implements HealthIndicator {

	public PaymentProviderHealth(Supplier<Duration> ping, Duration slowThreshold) {
	}

	@Override
	public Health health() {
		return Kata.todo("tracks/07-spring-boot-web/L3-production.md, section 3");
	}

}
