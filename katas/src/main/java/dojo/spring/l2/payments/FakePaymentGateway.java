package dojo.spring.l2.payments;

/** Given. Accepts everything; for local development and tests. */
public class FakePaymentGateway implements PaymentGateway {
	@Override
	public String charge(long cents) {
		return "fake-ok-" + cents;
	}
}
