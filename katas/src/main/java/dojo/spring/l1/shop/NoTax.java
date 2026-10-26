package dojo.spring.l1.shop;

public class NoTax implements TaxPolicy {
	@Override
	public long taxCents(long netCents) {
		return 0;
	}
}
