package dojo.spring.l1;

import dojo.spring.l1.shop.Checkout;
import dojo.spring.l1.shop.GermanVat;
import dojo.spring.l1.shop.NoTax;
import dojo.spring.l1.shop.PriceCalculator;
import dojo.spring.l1.shop.TaxPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Reference solution. The bean name is the method name. */
@Configuration
public class ShopConfig {

	@Bean
	@Primary // chosen when someone asks for "a TaxPolicy" without saying which
	TaxPolicy germanVat() {
		return new GermanVat();
	}

	@Bean
	TaxPolicy noTax() {
		return new NoTax();
	}

	@Bean
	@Primary
	PriceCalculator domesticPrices(TaxPolicy taxes) { // the primary TaxPolicy
		return new PriceCalculator(taxes);
	}

	@Bean
	PriceCalculator exportPrices(@Qualifier("noTax") TaxPolicy taxes) {
		return new PriceCalculator(taxes);
	}

	@Bean
	Checkout checkout(PriceCalculator domesticPrices, @Qualifier("exportPrices") PriceCalculator exportPrices) {
		return new Checkout(domesticPrices, exportPrices);
	}

}
