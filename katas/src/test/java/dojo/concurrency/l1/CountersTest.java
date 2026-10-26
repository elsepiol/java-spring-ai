package dojo.concurrency.l1;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.concurrency.l1.Counters.Counter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L1")
class CountersTest {

	static final int THREADS = 8;
	static final int PER_THREAD = 100_000;

	/** All threads wait at a gate and start together, to maximise contention. */
	static long hammer(Counter counter) throws InterruptedException {
		CountDownLatch gate = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < THREADS; t++) {
			Thread thread = new Thread(() -> {
				try {
					gate.await();
				} catch (InterruptedException e) {
					return;
				}
				for (int i = 0; i < PER_THREAD; i++) {
					counter.increment();
				}
			});
			thread.start();
			threads.add(thread);
		}
		gate.countDown();
		for (Thread thread : threads) {
			thread.join();
		}
		return counter.get();
	}

	private static void check(Supplier<Counter> factory) throws InterruptedException {
		Counter fresh = factory.get();
		assertThat(fresh.get()).isZero();
		assertThat(hammer(fresh)).isEqualTo((long) THREADS * PER_THREAD);
	}

	@Test
	void synchronizedCounterLosesNothing() throws InterruptedException {
		check(Counters::synchronizedCounter);
	}

	@Test
	void atomicCounterLosesNothing() throws InterruptedException {
		check(Counters::atomicCounter);
	}

	@Test
	void adderCounterLosesNothing() throws InterruptedException {
		check(Counters::adderCounter);
	}

	@Test
	void eachFactoryCallReturnsANewCounter() {
		Counter a = Counters.atomicCounter();
		a.increment();
		assertThat(Counters.atomicCounter().get()).isZero();
		Counter s = Counters.synchronizedCounter();
		s.increment();
		assertThat(Counters.synchronizedCounter().get()).isZero();
	}

}
