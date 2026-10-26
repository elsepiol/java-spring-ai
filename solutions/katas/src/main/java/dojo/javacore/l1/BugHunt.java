package dojo.javacore.l1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reference solution, with the reason for each bug. */
public final class BugHunt {

	private BugHunt() {
	}

	/** == compares references. Two different String objects with the same text are not ==. */
	public static boolean isAdmin(String name) {
		return "admin".equals(name); // constant first: no NullPointerException for null
	}

	/** Integer == compares references; only -128..127 are cached, so 1000 == 1000 is false. */
	public static boolean sameQuantity(Integer a, Integer b) {
		return Objects.equals(a, b); // null-safe; a.equals(b) would throw for a == null
	}

	/** int / int is integer division: 3 / 2 == 1. Make one side a double first. */
	public static double average(int[] scores) {
		long sum = 0;
		for (int s : scores) {
			sum += s;
		}
		return scores.length == 0 ? 0 : (double) sum / scores.length;
	}

	/** "copy = tags" copies the REFERENCE: both names point to the same list. */
	public static List<String> withTag(List<String> tags, String tag) {
		List<String> copy = new ArrayList<>(tags);
		copy.add(tag);
		return copy;
	}

	/** An int overflows silently past 2,147,483,647 and wraps to negative. Sum in a long. */
	public static long total(int[] amountsInCents) {
		long total = 0;
		for (int a : amountsInCents) {
			total += a;
		}
		return total;
	}

}
