package dojo.spring.l3;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** Part of the reference solution: the cached call lives in its own bean, so callers go through its proxy. */
@Component
public class BasePrices {

	private final PriceSource source;

	public BasePrices(PriceSource source) {
		this.source = source;
	}

	@Cacheable("basePrices")
	public long of(String sku) {
		return source.fetch(sku);
	}

}
