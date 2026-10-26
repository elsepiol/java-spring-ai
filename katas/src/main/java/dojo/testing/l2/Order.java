package dojo.testing.l2;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Given: a domain object with validation, the kind that makes tests noisy to set up. */
public record Order(String id, String customer, List<Line> lines, Status status, Instant createdAt) {

	public enum Status {
		NEW, PAID, SHIPPED
	}

	public record Line(String sku, int quantity, long unitPriceCents) {

		public Line {
			Objects.requireNonNull(sku);
			if (quantity <= 0 || unitPriceCents < 0) {
				throw new IllegalArgumentException("bad line " + sku);
			}
		}

	}

	public Order {
		Objects.requireNonNull(id);
		Objects.requireNonNull(customer);
		Objects.requireNonNull(status);
		Objects.requireNonNull(createdAt);
		if (lines == null || lines.isEmpty()) {
			throw new IllegalArgumentException("an order needs at least one line");
		}
		lines = List.copyOf(lines);
	}

	public long totalCents() {
		return lines.stream().mapToLong(l -> l.quantity() * l.unitPriceCents()).sum();
	}

}
