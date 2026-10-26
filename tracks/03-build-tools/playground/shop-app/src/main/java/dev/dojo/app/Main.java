package dev.dojo.app;

import dev.dojo.core.Price;

/** Run after a build:  java -cp shop-app/target/classes:shop-core/target/classes dev.dojo.app.Main */
public class Main {

	public static void main(String[] args) {
		System.out.println("19.99 with 21% VAT = " + checkoutTotal(1999));
	}

	static Price checkoutTotal(long cents) {
		return new Price(cents).withVat(21);
	}

}
