package dojo.spring.l1.shop;

public class GermanVat implements TaxPolicy {
	@Override
	public long taxCents(long netCents) {
		return Math.round(netCents * 0.19);
	}
}
