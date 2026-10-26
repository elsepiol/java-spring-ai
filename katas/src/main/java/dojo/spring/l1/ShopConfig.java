package dojo.spring.l1;

import org.springframework.context.annotation.Configuration;

/**
 * KATA spring L1 "ShopConfig" (tracks/06-spring-core/L1-basics.md, section 5: Java configuration).
 *
 * Wire the given classes in dojo.spring.l1.shop (do not change them; they have no Spring
 * annotations) with @Bean methods, so that ShopConfigTest's context provides:
 * - a TaxPolicy bean "germanVat" (GermanVat) that is the DEFAULT TaxPolicy, and one "noTax" (NoTax);
 * - a PriceCalculator "domesticPrices" using German VAT, which is the DEFAULT PriceCalculator,
 *   and "exportPrices" using no tax;
 * - a Checkout bean using domesticPrices and exportPrices respectively.
 * Hints: @Bean, @Primary, @Qualifier, and @Bean method parameters (Spring injects them).
 */
@Configuration
public class ShopConfig {
}
