package dojo.javacore.l3;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/** Reference solution. The heap holds at most one element per input. */
public final class KWayMerge {

	private KWayMerge() {
	}

	private record Head<T>(T value, Iterator<? extends T> source) {
	}

	public static <T> Iterator<T> merge(List<? extends Iterator<? extends T>> inputs, Comparator<? super T> order) {
		PriorityQueue<Head<T>> heap = new PriorityQueue<>(Math.max(1, inputs.size()), (a, b) -> order.compare(a.value(), b.value()));
		for (Iterator<? extends T> input : inputs) {
			if (input.hasNext()) {
				heap.add(new Head<>(input.next(), input));
			}
		}
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return !heap.isEmpty();
			}

			@Override
			public T next() {
				Head<T> smallest = heap.poll();
				if (smallest == null) {
					throw new NoSuchElementException();
				}
				if (smallest.source().hasNext()) {
					heap.add(new Head<>(smallest.source().next(), smallest.source())); // refill from the same input
				}
				return smallest.value();
			}
		};
	}

}
