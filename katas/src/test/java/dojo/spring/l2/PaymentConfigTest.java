package dojo.spring.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.spring.l2.payments.FakePaymentGateway;
import dojo.spring.l2.payments.PaymentGateway;
import dojo.spring.l2.payments.StripeGateway;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

@Tag("spring")
@Tag("L2")
class PaymentConfigTest {

	private static AnnotationConfigApplicationContext start(Map<String, Object> properties, String... profiles) {
		var context = new AnnotationConfigApplicationContext();
		context.getEnvironment().setActiveProfiles(profiles);
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
		// Spring Boot registers this for you. Without it, plain Spring resolves ${...} leniently and
		// injects the literal text "${payment.api-key}" when the property is missing.
		context.registerBean(PropertySourcesPlaceholderConfigurer.class);
		context.register(PaymentConfig.class);
		context.refresh();
		return context;
	}

	@Test
	void withoutProfilesTheFakeIsUsed() {
		try (var context = start(Map.of())) {
			assertThat(context.getBeansOfType(PaymentGateway.class)).hasSize(1);
			assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(FakePaymentGateway.class);
		}
	}

	@Test
	void devUsesTheFakeToo() {
		try (var context = start(Map.of(), "dev")) {
			assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(FakePaymentGateway.class);
		}
	}

	@Test
	void prodUsesStripeWithItsProperties() {
		try (var context = start(Map.of("payment.api-key", "sk_live_123", "payment.timeout-ms", "500"), "prod")) {
			assertThat(context.getBeansOfType(PaymentGateway.class)).hasSize(1);
			PaymentGateway gateway = context.getBean(PaymentGateway.class);
			assertThat(gateway).isInstanceOf(StripeGateway.class);
			assertThat(((StripeGateway) gateway).timeoutMillis()).isEqualTo(500);
			assertThat(gateway.charge(1200)).isEqualTo("stripe-sk_l-1200");
		}
	}

	@Test
	void theTimeoutHasADefault() {
		try (var context = start(Map.of("payment.api-key", "sk_live_123"), "prod")) {
			assertThat(((StripeGateway) context.getBean(PaymentGateway.class)).timeoutMillis()).isEqualTo(2000);
		}
	}

	@Test
	void prodWithoutAnApiKeyFailsAtStartup() {
		// fail fast: better a pod that never becomes ready than one that fails the first real payment
		assertThatThrownBy(() -> start(Map.of(), "prod")).rootCause().hasMessageContaining("payment.api-key");
	}

}
