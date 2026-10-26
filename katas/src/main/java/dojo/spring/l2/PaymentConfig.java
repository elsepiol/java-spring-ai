package dojo.spring.l2;

import org.springframework.context.annotation.Configuration;

/**
 * KATA spring L2 "PaymentConfig" (tracks/06-spring-core/L2-practical.md, sections 3-4: profiles and properties).
 *
 * The same application must use the fake gateway on a laptop and the real one in production,
 * without code changes: the choice is configuration.
 * - profile "prod": a PaymentGateway bean that is a StripeGateway built from the properties
 *   payment.api-key (required: startup must FAIL without it) and payment.timeout-ms
 *   (optional, default 2000);
 * - any other profile, or none: a FakePaymentGateway;
 * - exactly one PaymentGateway bean in every case.
 * Classes in dojo.spring.l2.payments are given; do not change them.
 */
@Configuration
public class PaymentConfig {
}
