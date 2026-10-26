package dojo.spring.l1.shop;

/** Given. Uses the default price calculator and, for export orders, a tax-free one. */
public class Checkout {
	private final PriceCalculator domestic;
	private final PriceCalculator export;

	public Checkout(PriceCalculator domestic, PriceCalculator export) {
		this.domestic = domestic;
		this.export = export;
	}

	public long total(long netCents, boolean isExport) {
		return (isExport ? export : domestic).grossCents(netCents);
	}
}
