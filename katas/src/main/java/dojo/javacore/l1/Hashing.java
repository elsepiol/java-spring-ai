package dojo.javacore.l1;

import dojo.Kata;

/**
 * KATA javacore L1 "Hashing" + DSA (tracks/01-java-core/L1-basics.md, section 7).
 *
 * Two classic interview problems that are really about one idea: a HashMap answers
 * "have I seen this before?" in O(1), which turns an O(n^2) double loop into one O(n) pass.
 * The tests use 200,000 elements, so the double loop is too slow on purpose.
 */
public final class Hashing {

	private Hashing() {
	}

	/**
	 * Returns the indices {i, j}, i < j, of two numbers that add up to target, or an empty array
	 * if there are none. If several pairs exist, return the one whose j is smallest.
	 * Example: nums = [3, 8, 4, 1], target = 5. Scanning left to right, the first j that completes a
	 * pair is j = 3 (value 1), whose partner 4 is at index 2, so the answer is [2, 3].
	 */
	public static int[] twoSum(int[] nums, int target) {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 7");
	}

	/** The index of the first character that appears exactly once in s, or -1. "swiss" -> 1 ('w'). */
	public static int firstUniqueIndex(String s) {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 7");
	}

}
