package dojo.spring.l3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Tag("spring")
@Tag("L3")
class PriceServiceTest {

	@Configuration
	@EnableCaching
	@ComponentScan(basePackageClasses = PriceService.class, useDefaultFilters = false,
			includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dojo\\.spring\\.l3\\.(Price|Base).*"))
	static class Config {
		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("basePrices");
		}
	}

	@Test
	void basePriceIsCachedWhenCalledFromOutside() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			PriceService prices = context.getBean(PriceService.class);
			PriceSource source = context.getBean(PriceSource.class);
			prices.basePrice("MUG-1");
			prices.basePrice("MUG-1");
			assertThat(source.calls).hasValue(1);
		}
	}

	@Test
	void priceWithVatUsesTheCacheToo() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			PriceService prices = context.getBean(PriceService.class);
			PriceSource source = context.getBean(PriceSource.class);
			assertThat(prices.priceWithVat("MUG-1")).isEqualTo(5950);
			assertThat(prices.priceWithVat("MUG-1")).isEqualTo(5950);
			assertThat(prices.priceWithVat("PAN-1")).isEqualTo(5950);
			assertThat(source.calls).as("one remote call per SKU").hasValue(2);
		}
	}

}
