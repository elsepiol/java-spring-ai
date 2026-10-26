package dojo.spring.l2.payments;

/** Given. The real one needs an API key and a timeout. */
public class StripeGateway implements PaymentGateway {
	private final String apiKey;
	private final int timeoutMillis;

	public StripeGateway(String apiKey, int timeoutMillis) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("payment.api-key is required");
		}
		this.apiKey = apiKey;
		this.timeoutMillis = timeoutMillis;
	}

	public int timeoutMillis() {
		return timeoutMillis;
	}

	@Override
	public String charge(long cents) {
		return "stripe-" + apiKey.substring(0, 4) + "-" + cents;
	}
}
