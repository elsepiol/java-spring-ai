package dojo.spring.l1.shop;

/** Given. Note: no Spring annotations, no `new` inside: it receives what it needs (dependency injection). */
public class PriceCalculator {
	private final TaxPolicy taxes;

	public PriceCalculator(TaxPolicy taxes) {
		this.taxes = taxes;
	}

	public long grossCents(long netCents) {
		return netCents + taxes.taxCents(netCents);
	}
}
