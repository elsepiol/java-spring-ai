package dojo.javacore.l2;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reference solution. */
public final class OrderReport {

	public enum Status { PENDING, PAID, CANCELLED }

	public record Order(long id, String customer, String category, long cents, java.time.LocalDate date, Status status) {
	}

	private OrderReport() {
	}

	private static Stream<Order> paid(List<Order> orders) {
		return orders.stream().filter(o -> o.status() == Status.PAID);
	}

	public static Map<String, Long> revenueByCustomer(List<Order> orders) {
		return paid(orders).collect(Collectors.groupingBy(Order::customer, Collectors.summingLong(Order::cents)));
	}

	public static List<String> topCustomers(List<Order> orders, int n) {
		return revenueByCustomer(orders).entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.limit(n)
				.map(Map.Entry::getKey)
				.toList();
	}

	public static SortedMap<YearMonth, Map<String, Long>> revenueByMonthAndCategory(List<Order> orders) {
		return paid(orders).collect(Collectors.groupingBy(o -> YearMonth.from(o.date()), TreeMap::new,
				Collectors.groupingBy(Order::category, Collectors.summingLong(Order::cents))));
	}

	public static OptionalDouble averagePaidOrder(List<Order> orders) {
		return paid(orders).mapToLong(Order::cents).average();
	}

	public static Map<Status, Long> statusCounts(List<Order> orders) {
		Map<Status, Long> counts = orders.stream()
				.collect(Collectors.groupingBy(Order::status, () -> new EnumMap<>(Status.class), Collectors.counting()));
		Arrays.stream(Status.values()).forEach(s -> counts.putIfAbsent(s, 0L));
		return counts;
	}

	public static String categories(List<Order> orders) {
		return paid(orders).map(Order::category).distinct().sorted().collect(Collectors.joining(", "));
	}

}
