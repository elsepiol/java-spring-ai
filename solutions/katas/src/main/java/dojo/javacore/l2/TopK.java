package dojo.javacore.l2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

/** Reference solution: count with a HashMap, then a size-k min-heap of the "best so far". */
public final class TopK {

	private TopK() {
	}

	public static List<String> mostFrequentWords(String text, int k) {
		if (k <= 0) {
			return List.of();
		}
		Map<String, Integer> counts = new HashMap<>();
		for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
			if (!word.isEmpty()) {
				counts.merge(word, 1, Integer::sum);
			}
		}
		// "Better" = higher count, then earlier in the alphabet. The heap's head is the WORST of the
		// k kept so far, so a new candidate only has to beat the head to get in.
		Comparator<Map.Entry<String, Integer>> better = Map.Entry.<String, Integer>comparingByValue()
				.thenComparing(Map.Entry.<String, Integer>comparingByKey().reversed());
		PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>(better);
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			heap.offer(e);
			if (heap.size() > k) {
				heap.poll(); // drop the worst
			}
		}
		List<String> result = new ArrayList<>(heap.size());
		while (!heap.isEmpty()) {
			result.add(heap.poll().getKey()); // worst first
		}
		Collections.reverse(result);
		return result;
	}

}
