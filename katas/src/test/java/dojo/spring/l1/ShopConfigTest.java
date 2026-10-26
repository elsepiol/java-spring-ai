package dojo.spring.l1;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.spring.l1.shop.Checkout;
import dojo.spring.l1.shop.GermanVat;
import dojo.spring.l1.shop.NoTax;
import dojo.spring.l1.shop.PriceCalculator;
import dojo.spring.l1.shop.TaxPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Tag("spring")
@Tag("L1")
class ShopConfigTest {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void start() {
		context = new AnnotationConfigApplicationContext(ShopConfig.class); // a plain Spring container, no Boot
	}

	@AfterEach
	void stop() {
		context.close();
	}

	@Test
	void theDefaultTaxPolicyIsGermanVat() {
		assertThat(context.getBean(TaxPolicy.class)).isInstanceOf(GermanVat.class);
		assertThat(context.getBean("noTax")).isInstanceOf(NoTax.class);
	}

	@Test
	void twoPriceCalculators() {
		assertThat(context.getBean(PriceCalculator.class).grossCents(10_000)).isEqualTo(11_900);
		assertThat(context.getBean("exportPrices", PriceCalculator.class).grossCents(10_000)).isEqualTo(10_000);
		assertThat(context.getBean(PriceCalculator.class)).isSameAs(context.getBean("domesticPrices"));
	}

	@Test
	void checkoutUsesTheRightCalculators() {
		Checkout checkout = context.getBean(Checkout.class);
		assertThat(checkout.total(10_000, false)).isEqualTo(11_900);
		assertThat(checkout.total(10_000, true)).isEqualTo(10_000);
	}

	@Test
	void beansAreSingletons() {
		assertThat(context.getBean(Checkout.class)).isSameAs(context.getBean(Checkout.class));
	}

}
