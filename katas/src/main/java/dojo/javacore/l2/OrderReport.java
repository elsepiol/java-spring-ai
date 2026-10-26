package dojo.javacore.l2;

import dojo.Kata;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;

/**
 * KATA javacore L2 "OrderReport" (tracks/01-java-core/L2-practical.md, section 5: streams).
 *
 * Reports over a list of orders, the kind of code every backend has. Each method is ONE stream
 * pipeline (no for loops, no mutable variables outside the stream). Only PAID orders count as
 * revenue; CANCELLED and PENDING orders are ignored by every method except statusCounts.
 */
public final class OrderReport {

	public enum Status { PENDING, PAID, CANCELLED }

	public record Order(long id, String customer, String category, long cents, LocalDate date, Status status) {
	}

	private OrderReport() {
	}

	/** Revenue per customer: {"ann" -> 3000, "bob" -> 500}. */
	public static Map<String, Long> revenueByCustomer(List<Order> orders) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5 (groupingBy + summingLong)");
	}

	/** The n customers with the most revenue, highest first; ties broken by name A-Z. */
	public static List<String> topCustomers(List<Order> orders, int n) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5 (Comparator chaining)");
	}

	/** Revenue per month, then per category, months in order: {2026-01 -> {books -> 1200, toys -> 300}}. */
	public static SortedMap<YearMonth, Map<String, Long>> revenueByMonthAndCategory(List<Order> orders) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5 (nested groupingBy with a TreeMap)");
	}

	/** Average value of a paid order in cents; empty when there are none. */
	public static OptionalDouble averagePaidOrder(List<Order> orders) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5");
	}

	/** How many orders have each status, INCLUDING statuses with zero orders. */
	public static Map<Status, Long> statusCounts(List<Order> orders) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5 (EnumMap)");
	}

	/** Every category that has at least one paid order, sorted, no duplicates, comma-separated: "books, toys". */
	public static String categories(List<Order> orders) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 5 (distinct, sorted, joining)");
	}

}
