package dojo.javacore.l1;

import java.util.HashMap;
import java.util.Map;

/** Reference solution. Both are O(n) time, O(n) extra space. */
public final class Hashing {

	private Hashing() {
	}

	public static int[] twoSum(int[] nums, int target) {
		Map<Integer, Integer> indexOf = new HashMap<>(); // value -> first index where it was seen
		for (int j = 0; j < nums.length; j++) {
			Integer i = indexOf.get(target - nums[j]);
			if (i != null) {
				return new int[] { i, j };
			}
			indexOf.putIfAbsent(nums[j], j);
		}
		return new int[0];
	}

	public static int firstUniqueIndex(String s) {
		Map<Character, Integer> counts = new HashMap<>();
		for (char c : s.toCharArray()) {
			counts.merge(c, 1, Integer::sum);
		}
		for (int i = 0; i < s.length(); i++) {
			if (counts.get(s.charAt(i)) == 1) {
				return i;
			}
		}
		return -1;
	}

}
