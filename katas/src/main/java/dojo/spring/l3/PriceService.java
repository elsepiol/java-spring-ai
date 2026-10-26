package dojo.spring.l3;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * KATA spring L3 "PriceService" (find the bug) (tracks/06-spring-core/L3-production.md, section 2:
 * self-invocation).
 *
 * basePrice() is slow (a remote pricing call: PriceSource counts the calls), so it is @Cacheable.
 * Calling basePrice() from another bean is cached. But the shop calls priceWithVat(), and every
 * call still hits the pricing service. Why? Fix it so that priceWithVat() uses the cache too.
 * Keep @Cacheable("basePrices") semantics; you may add classes in this package.
 * (The same bug makes @Transactional "not work" when a method calls another method of its own class.)
 */
@Service
public class PriceService {

	private final PriceSource source;

	public PriceService(PriceSource source) {
		this.source = source;
	}

	@Cacheable("basePrices")
	public long basePrice(String sku) {
		return source.fetch(sku);
	}

	public long priceWithVat(String sku) {
		return Math.round(basePrice(sku) * 1.19);
	}

}
