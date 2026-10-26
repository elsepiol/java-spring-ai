package dojo.testing.l2;

import java.time.Instant;

import dojo.Kata;

/**
 * KATA testing L2 "OrderBuilder" (tracks/10-testing/L2-practical.md, section 3).
 *
 * A test data builder: tests say only what matters to them ("a paid order with two lines") and
 * get valid defaults for everything else. (In a real project it lives in src/test.)
 * - anOrder().build() is a valid order: id "order-1", customer "customer-1", ONE line
 *   ("SKU-1", 1, 1000), status NEW, createdAt 2026-01-01T00:00:00Z;
 * - withId, withCustomer, createdAt, paid(), shipped() override one thing each;
 * - the first withLine(...) REPLACES the default line; later calls add lines;
 * - each method returns the builder (fluent); build() can be called repeatedly;
 * - but() returns an independent copy, so a test can derive variants from a base builder
 *   without changing it.
 */
public final class OrderBuilder {

	public static OrderBuilder anOrder() {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder withId(String id) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder withCustomer(String customer) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder withLine(String sku, int quantity, long unitPriceCents) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder createdAt(Instant createdAt) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder paid() {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder shipped() {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public OrderBuilder but() {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

	public Order build() {
		return Kata.todo("tracks/10-testing/L2-practical.md section 3");
	}

}
