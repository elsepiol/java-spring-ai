package dojo.testing.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Every test builds its own FeatureFlags, so their order must not matter. Here it is fixed on purpose. */
@Tag("testing")
@Tag("L3")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeatureFlagsTest {

	@Test
	@Order(1)
	void canEnableTheNewCheckout() {
		FeatureFlags flags = new FeatureFlags(Map.of("new-checkout", false));
		flags.enable("new-checkout");
		assertThat(flags.isEnabled("new-checkout")).isTrue();
	}

	@Test
	@Order(2)
	void theNewCheckoutIsOffByDefault() {
		FeatureFlags flags = new FeatureFlags(Map.of("new-checkout", false));
		assertThat(flags.isEnabled("new-checkout")).isFalse();
	}

	@Test
	@Order(3)
	void defaultsApplyToEachInstance() {
		FeatureFlags flags = new FeatureFlags(Map.of("dark-mode", true));
		flags.disable("dark-mode");
		assertThat(new FeatureFlags(Map.of("dark-mode", true)).isEnabled("dark-mode")).isTrue();
	}

	@Test
	@Order(4)
	void unknownFlagsAreOff() {
		assertThat(new FeatureFlags(Map.of()).isEnabled("dark-mode")).isFalse();
	}

}
