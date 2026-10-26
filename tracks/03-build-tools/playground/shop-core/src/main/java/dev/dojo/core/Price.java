package dev.dojo.core;

/** A price in cents. The module has no dependencies: anything can use it. */
public record Price(long cents) {

	public Price {
		if (cents < 0) {
			throw new IllegalArgumentException("a price cannot be negative");
		}
	}

	public Price withVat(int percent) {
		return new Price(Math.round(cents * (100 + percent) / 100.0));
	}

	@Override
	public String toString() {
		return cents / 100 + "." + String.format("%02d", cents % 100);
	}

}
