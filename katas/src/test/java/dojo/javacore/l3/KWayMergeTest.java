package dojo.javacore.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L3")
class KWayMergeTest {

	private static <T> List<T> drain(Iterator<T> it) {
		List<T> out = new ArrayList<>();
		it.forEachRemaining(out::add);
		return out;
	}

	@Test
	void mergesSortedInputs() {
		Iterator<Integer> merged = KWayMerge.merge(List.of(List.of(1, 4, 9).iterator(), List.of(2, 3, 10).iterator(),
				List.<Integer>of().iterator(), List.of(5).iterator()), Comparator.naturalOrder());
		assertThat(drain(merged)).containsExactly(1, 2, 3, 4, 5, 9, 10);
	}

	@Test
	void keepsDuplicates() {
		Iterator<String> merged = KWayMerge.merge(List.of(List.of("a", "c").iterator(), List.of("a", "b").iterator()), Comparator.naturalOrder());
		assertThat(drain(merged)).containsExactly("a", "a", "b", "c");
	}

	@Test
	void noInputsMeansNothing() {
		Iterator<Integer> merged = KWayMerge.merge(List.<Iterator<Integer>>of(), Comparator.naturalOrder());
		assertThat(merged.hasNext()).isFalse();
		assertThatThrownBy(merged::next).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void isLazyEvenWithEndlessInputs() {
		AtomicInteger reads = new AtomicInteger();
		List<Iterator<Integer>> endless = IntStream.range(0, 3)
				.mapToObj(k -> Stream.iterate(k, i -> i + 3).peek(i -> reads.incrementAndGet()).iterator()).toList();

		Iterator<Integer> merged = KWayMerge.merge(endless, Comparator.naturalOrder());
		List<Integer> first = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			first.add(merged.next());
		}
		assertThat(first).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertThat(reads.get()).as("read about one element per output, not everything").isLessThanOrEqualTo(13);
	}

	@Test
	void mergesInTheGivenOrder() {
		Iterator<Integer> merged = KWayMerge.merge(List.of(List.of(9, 5, 1).iterator(), List.of(8, 2).iterator()), Comparator.reverseOrder());
		assertThat(drain(merged)).containsExactly(9, 8, 5, 2, 1);
	}

}
