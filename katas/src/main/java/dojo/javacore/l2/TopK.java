package dojo.javacore.l2;

import dojo.Kata;
import java.util.List;

/**
 * KATA javacore L2 "TopK" + DSA (tracks/01-java-core/L2-practical.md, section 3: PriorityQueue).
 *
 * "The 10 most searched products", "the 5 noisiest clients": top-K is everywhere.
 * Return the k most frequent words of the text, most frequent first; equal counts in A-Z order.
 * Words are runs of letters (a-z, A-Z), compared case-insensitively, returned in lower case.
 *
 * Sorting all distinct words is O(m log m). Do better: keep a min-heap of size k, O(m log k).
 * (m = number of distinct words). The test checks the result, the lesson explains the heap.
 */
public final class TopK {

	private TopK() {
	}

	public static List<String> mostFrequentWords(String text, int k) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 3");
	}

}
