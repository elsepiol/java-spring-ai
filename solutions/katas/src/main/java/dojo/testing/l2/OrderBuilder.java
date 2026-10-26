package dojo.testing.l2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reference solution for the OrderBuilder kata. */
public final class OrderBuilder {

	private String id = "order-1";
	private String customer = "customer-1";
	private List<Order.Line> lines = new ArrayList<>(List.of(new Order.Line("SKU-1", 1, 1000)));
	private boolean defaultLines = true;
	private Order.Status status = Order.Status.NEW;
	private Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

	private OrderBuilder() {
	}

	public static OrderBuilder anOrder() {
		return new OrderBuilder();
	}

	public OrderBuilder withId(String id) {
		this.id = id;
		return this;
	}

	public OrderBuilder withCustomer(String customer) {
		this.customer = customer;
		return this;
	}

	public OrderBuilder withLine(String sku, int quantity, long unitPriceCents) {
		if (defaultLines) {
			lines.clear();
			defaultLines = false;
		}
		lines.add(new Order.Line(sku, quantity, unitPriceCents));
		return this;
	}

	public OrderBuilder createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public OrderBuilder paid() {
		this.status = Order.Status.PAID;
		return this;
	}

	public OrderBuilder shipped() {
		this.status = Order.Status.SHIPPED;
		return this;
	}

	public OrderBuilder but() {
		OrderBuilder copy = new OrderBuilder();
		copy.id = id;
		copy.customer = customer;
		copy.lines = new ArrayList<>(lines);
		copy.defaultLines = defaultLines;
		copy.status = status;
		copy.createdAt = createdAt;
		return copy;
	}

	public Order build() {
		return new Order(id, customer, lines, status, createdAt);
	}

}
