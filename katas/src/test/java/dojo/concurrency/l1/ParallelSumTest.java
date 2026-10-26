package dojo.concurrency.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L1")
class ParallelSumTest {

	@Test
	void sumsLikeASingleThread() {
		int[] data = new Random(42).ints(1_000_003, -1000, 1000).toArray();
		long expected = IntStream.of(data).asLongStream().sum();
		for (int threads : new int[] { 1, 2, 3, 4, 7, 16 }) {
			assertThat(ParallelSum.sum(data, threads)).as("%d threads", threads).isEqualTo(expected);
		}
	}

	@Test
	void doesNotOverflow() {
		int[] data = new int[10];
		java.util.Arrays.fill(data, Integer.MAX_VALUE);
		assertThat(ParallelSum.sum(data, 3)).isEqualTo(10L * Integer.MAX_VALUE);
	}

	@Test
	void moreThreadsThanElementsAndEmptyArrays() {
		assertThat(ParallelSum.sum(new int[] { 1, 2, 3 }, 8)).isEqualTo(6);
		assertThat(ParallelSum.sum(new int[0], 4)).isZero();
	}

	@Test
	void rejectsZeroThreads() {
		assertThatThrownBy(() -> ParallelSum.sum(new int[] { 1 }, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void anInterruptedCallerKeepsItsInterruptFlag() throws Exception {
		int[] data = new int[10_000_000]; // big enough that the workers are still running when the caller joins
		Boolean[] flagAfter = new Boolean[1];
		Throwable[] thrown = new Throwable[1];
		Thread caller = new Thread(() -> {
			Thread.currentThread().interrupt(); // interrupted before it even starts joining
			try {
				ParallelSum.sum(data, 2);
			} catch (Throwable t) {
				thrown[0] = t;
			}
			flagAfter[0] = Thread.currentThread().isInterrupted();
		});
		caller.start();
		caller.join(10_000);
		assertThat(thrown[0]).isInstanceOf(IllegalStateException.class);
		assertThat(flagAfter[0]).as("interrupt flag restored").isTrue();
	}

}
