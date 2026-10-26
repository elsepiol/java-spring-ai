package dojo.spring.l3;

import org.springframework.stereotype.Service;

/**
 * Reference solution. `this.basePrice()` calls the method on the TARGET object directly, not through
 * the caching proxy that Spring put in front of the bean, so the advice never runs. Fix: move the
 * cached method to another bean (BasePrices) and call it through its proxy. (Alternatives: inject a
 * lazy reference to yourself, or use AspectJ weaving; moving it out is the clean one.)
 */
@Service
public class PriceService {

	private final BasePrices basePrices;

	public PriceService(BasePrices basePrices) {
		this.basePrices = basePrices;
	}

	public long basePrice(String sku) {
		return basePrices.of(sku);
	}

	public long priceWithVat(String sku) {
		return Math.round(basePrices.of(sku) * 1.19);
	}

}
