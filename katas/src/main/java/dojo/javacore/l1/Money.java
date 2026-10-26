package dojo.javacore.l1;

import dojo.Kata;

/**
 * KATA javacore L1 "Money" (tracks/01-java-core/L1-basics.md, section 6).
 *
 * A VALUE OBJECT: an amount of money in cents plus a currency code such as "EUR".
 * - immutable: no setters, plus() returns a NEW Money;
 * - two Money values are equal when cents and currency are equal, so they work as HashMap keys
 *   and duplicates collapse in a HashSet (equals AND hashCode);
 * - of() rejects a null or blank currency with IllegalArgumentException, and upper-cases it;
 * - plus() of different currencies throws IllegalArgumentException (never add EUR to USD);
 * - toString() is "12.50 EUR" (always two decimals; -0.05 for -5 cents).
 *
 * Why cents in a long and not a double? Run tracks/01-java-core/examples/FloatingMoney.java.
 */
public final class Money {

	public static Money of(long cents, String currency) {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 6");
	}

	public long cents() {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 6");
	}

	public String currency() {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 6");
	}

	public Money plus(Money other) {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 6");
	}

}
