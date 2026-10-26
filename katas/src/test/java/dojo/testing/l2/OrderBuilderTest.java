package dojo.testing.l2;

import static dojo.testing.l2.OrderBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("testing")
@Tag("L2")
class OrderBuilderTest {

	@Test
	void theDefaultOrderIsValid() {
		Order order = anOrder().build();
		assertThat(order.id()).isEqualTo("order-1");
		assertThat(order.customer()).isEqualTo("customer-1");
		assertThat(order.lines()).containsExactly(new Order.Line("SKU-1", 1, 1000));
		assertThat(order.status()).isEqualTo(Order.Status.NEW);
		assertThat(order.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
	}

	@Test
	void testsStateOnlyWhatMattersToThem() {
		Order order = anOrder().paid().withCustomer("ada").build();
		assertThat(order.status()).isEqualTo(Order.Status.PAID);
		assertThat(order.customer()).isEqualTo("ada");
		assertThat(order.lines()).hasSize(1);
	}

	@Test
	void theFirstLineReplacesTheDefaultAndLaterLinesAdd() {
		Order order = anOrder().withLine("A", 2, 500).withLine("B", 1, 250).build();
		assertThat(order.lines()).extracting(Order.Line::sku).containsExactly("A", "B");
		assertThat(order.totalCents()).isEqualTo(1_250);
	}

	@Test
	void overridesEachField() {
		Order order = anOrder().withId("o-9").createdAt(Instant.parse("2026-06-01T08:00:00Z")).shipped().build();
		assertThat(order.id()).isEqualTo("o-9");
		assertThat(order.createdAt()).isEqualTo(Instant.parse("2026-06-01T08:00:00Z"));
		assertThat(order.status()).isEqualTo(Order.Status.SHIPPED);
	}

	@Test
	void buildCanBeCalledRepeatedly() {
		OrderBuilder builder = anOrder().withLine("A", 1, 100);
		assertThat(builder.build()).isEqualTo(builder.build());
	}

	@Test
	void butDerivesVariantsWithoutChangingTheBase() {
		OrderBuilder base = anOrder().withCustomer("ada").withLine("A", 1, 100);
		Order paid = base.but().paid().withLine("B", 1, 200).build();
		Order original = base.build();

		assertThat(paid.status()).isEqualTo(Order.Status.PAID);
		assertThat(paid.lines()).hasSize(2);
		assertThat(original.status()).isEqualTo(Order.Status.NEW);
		assertThat(original.lines()).extracting(Order.Line::sku).containsExactly("A");
		assertThat(original.customer()).isEqualTo("ada");
	}

	@Test
	void aCopyOfTheDefaultsStillReplacesTheDefaultLine() {
		Order order = anOrder().but().withLine("X", 3, 10).build();
		assertThat(order.lines()).extracting(Order.Line::sku).containsExactly("X");
	}

}
