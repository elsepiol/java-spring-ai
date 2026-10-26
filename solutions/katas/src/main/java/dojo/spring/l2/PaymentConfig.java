package dojo.spring.l2;

import dojo.spring.l2.payments.FakePaymentGateway;
import dojo.spring.l2.payments.PaymentGateway;
import dojo.spring.l2.payments.StripeGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Reference solution. ${name} without a default fails startup if missing; ${name:default} does not. */
@Configuration
public class PaymentConfig {

	@Bean
	@Profile("prod")
	PaymentGateway stripe(@Value("${payment.api-key}") String apiKey, @Value("${payment.timeout-ms:2000}") int timeoutMillis) {
		return new StripeGateway(apiKey, timeoutMillis);
	}

	@Bean
	@Profile("!prod")
	PaymentGateway fake() {
		return new FakePaymentGateway();
	}

}
