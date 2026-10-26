package dojo.javacore.l1;

import java.util.Objects;

/** Reference solution. A record would give equals/hashCode/toString for free; see the comment. */
public final class Money {

	private final long cents;
	private final String currency;

	private Money(long cents, String currency) {
		this.cents = cents;
		this.currency = currency;
	}

	public static Money of(long cents, String currency) {
		if (currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("currency is required");
		}
		return new Money(cents, currency.strip().toUpperCase(java.util.Locale.ROOT));
	}

	public long cents() {
		return cents;
	}

	public String currency() {
		return currency;
	}

	public Money plus(Money other) {
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException("cannot add " + other.currency + " to " + currency);
		}
		return new Money(Math.addExact(cents, other.cents), currency); // addExact: overflow is an error, not a wrap
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Money m && cents == m.cents && currency.equals(m.currency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cents, currency); // equal objects MUST have equal hash codes
	}

	@Override
	public String toString() {
		long abs = Math.abs(cents);
		return (cents < 0 ? "-" : "") + abs / 100 + "." + String.format("%02d", abs % 100) + " " + currency;
	}

	// With a record:  public record Money(long cents, String currency) { compact constructor validates }
	// equals/hashCode/accessors are generated; only toString and plus need writing.
}
