package dojo.boot.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/** ApplicationContextRunner: start a small Boot context per assertion, in milliseconds. */
@Tag("boot")
@Tag("L3")
class ShippingPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(ShippingConfig.class);

	@Test
	void bindsWithDefaults() {
		runner.withPropertyValues("shipping.carrier-url=https://api.carrier.example").run(context -> {
			assertThat(context).hasNotFailed();
			ShippingProperties p = context.getBean(ShippingProperties.class);
			assertThat(p.carrierUrl()).isEqualTo("https://api.carrier.example");
			assertThat(p.timeout()).isEqualTo(Duration.ofSeconds(2));
			assertThat(p.maxParcels()).isEqualTo(10);
		});
	}

	@Test
	void bindsDurationsAndNumbers() {
		runner.withPropertyValues("shipping.carrier-url=https://x.example", "shipping.timeout=500ms", "shipping.max-parcels=3")
				.run(context -> {
					ShippingProperties p = context.getBean(ShippingProperties.class);
					assertThat(p.timeout()).isEqualTo(Duration.ofMillis(500));
					assertThat(p.maxParcels()).isEqualTo(3);
				});
	}

	@Test
	void aMissingCarrierUrlStopsTheStart() {
		runner.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void anInsecureUrlStopsTheStart() {
		runner.withPropertyValues("shipping.carrier-url=http://plain.example").run(context -> assertThat(context).hasFailed());
	}

	@Test
	void anOutOfRangeValueStopsTheStart() {
		runner.withPropertyValues("shipping.carrier-url=https://x.example", "shipping.max-parcels=500")
				.run(context -> assertThat(context).hasFailed());
	}

}
