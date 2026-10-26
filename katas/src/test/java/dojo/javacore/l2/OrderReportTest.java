package dojo.javacore.l2;

import static dojo.javacore.l2.OrderReport.Status.CANCELLED;
import static dojo.javacore.l2.OrderReport.Status.PAID;
import static dojo.javacore.l2.OrderReport.Status.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import dojo.javacore.l2.OrderReport.Order;
import dojo.javacore.l2.OrderReport.Status;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L2")
class OrderReportTest {

	private static final List<Order> ORDERS = List.of(
			new Order(1, "ann", "books", 1200, LocalDate.of(2026, 1, 5), PAID),
			new Order(2, "bob", "toys", 300, LocalDate.of(2026, 1, 20), PAID),
			new Order(3, "ann", "toys", 1800, LocalDate.of(2026, 2, 1), PAID),
			new Order(4, "cat", "books", 9999, LocalDate.of(2026, 2, 2), CANCELLED),
			new Order(5, "dan", "garden", 500, LocalDate.of(2026, 2, 3), PENDING),
			new Order(6, "bob", "books", 2700, LocalDate.of(2026, 2, 9), PAID),
			new Order(7, "eve", "toys", 3000, LocalDate.of(2026, 3, 1), PAID));

	@Test
	void revenueByCustomerCountsOnlyPaidOrders() {
		assertThat(OrderReport.revenueByCustomer(ORDERS)).containsOnly(entry("ann", 3000L), entry("bob", 3000L), entry("eve", 3000L));
	}

	@Test
	void topCustomersBreaksTiesByName() {
		assertThat(OrderReport.topCustomers(ORDERS, 2)).containsExactly("ann", "bob");
		List<Order> clear = List.of(new Order(1, "zed", "x", 50, LocalDate.of(2026, 1, 1), PAID),
				new Order(2, "amy", "x", 10, LocalDate.of(2026, 1, 1), PAID));
		assertThat(OrderReport.topCustomers(clear, 5)).containsExactly("zed", "amy");
	}

	@Test
	void revenueByMonthAndCategoryIsInMonthOrder() {
		Map<YearMonth, Map<String, Long>> report = OrderReport.revenueByMonthAndCategory(ORDERS);
		assertThat(report.keySet()).containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(report.get(YearMonth.of(2026, 2))).containsOnly(entry("toys", 1800L), entry("books", 2700L));
	}

	@Test
	void averagePaidOrder() {
		assertThat(OrderReport.averagePaidOrder(ORDERS)).hasValue(9000 / 5.0);
		assertThat(OrderReport.averagePaidOrder(List.of())).isEmpty();
	}

	@Test
	void statusCountsIncludeZeros() {
		List<Order> noPending = ORDERS.stream().filter(o -> o.status() != PENDING).toList();
		assertThat(OrderReport.statusCounts(noPending)).containsOnly(entry(PAID, 5L), entry(CANCELLED, 1L), entry(Status.PENDING, 0L));
	}

	@Test
	void categoriesOfPaidOrders() {
		assertThat(OrderReport.categories(ORDERS)).isEqualTo("books, toys");
		assertThat(OrderReport.categories(List.of())).isEmpty();
	}

}
