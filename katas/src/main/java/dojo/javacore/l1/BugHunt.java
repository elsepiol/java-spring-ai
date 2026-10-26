package dojo.javacore.l1;

/**
 * KATA javacore L1 "BugHunt" (tracks/01-java-core/L1-basics.md, sections 2-4).
 *
 * Each method compiles, looks right, and is wrong. The tests show the inputs that break them.
 * Fix the code, then write down in one sentence WHY it was wrong (the quiz will ask you).
 */
public final class BugHunt {

	private BugHunt() {
	}

	/** True when the user name is exactly "admin". */
	public static boolean isAdmin(String name) {
		return name == "admin";
	}

	/** True when both orders have the same quantity. Quantities can be missing (null). */
	public static boolean sameQuantity(Integer a, Integer b) {
		return a == b;
	}

	/** The average of the scores, e.g. [1, 2] -> 1.5. An empty array has average 0. */
	public static double average(int[] scores) {
		int sum = 0;
		for (int s : scores) {
			sum += s;
		}
		return scores.length == 0 ? 0 : sum / scores.length;
	}

	/** Adds a tag to the list it is given and returns the list. Must not change the caller's list. */
	public static java.util.List<String> withTag(java.util.List<String> tags, String tag) {
		java.util.List<String> copy = tags;
		copy.add(tag);
		return copy;
	}

	/** The total of a large number of order amounts in cents. */
	public static long total(int[] amountsInCents) {
		int total = 0;
		for (int a : amountsInCents) {
			total += a;
		}
		return total;
	}

}
