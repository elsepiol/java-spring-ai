package dojo.concurrency.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L2")
class BoundedBufferTest {

	@Test
	void isFirstInFirstOutAcrossTheWrapAround() throws Exception {
		BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < 10; i++) { // goes round the ring several times
			buffer.put(i);
			if (i % 2 == 1) {
				out.add(buffer.take());
				out.add(buffer.take());
			}
		}
		assertThat(out).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertThat(buffer.size()).isZero();
	}

	@Test
	void offerGivesUpWhenFull() throws Exception {
		BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
		assertThat(buffer.offer("a", 10, TimeUnit.MILLISECONDS)).isTrue();
		long start = System.nanoTime();
		assertThat(buffer.offer("b", 200, TimeUnit.MILLISECONDS)).isFalse();
		assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isBetween(150L, 2000L);
		assertThat(buffer.size()).isEqualTo(1);
	}

	@Test
	void putBlocksWhileFullUntilSomeoneTakes() throws Exception {
		BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
		buffer.put("first");
		AtomicBoolean secondPutDone = new AtomicBoolean();
		Thread producer = new Thread(() -> {
			try {
				buffer.put("second");
				secondPutDone.set(true);
			} catch (InterruptedException ignored) {
			}
		});
		producer.start();
		Thread.sleep(200);
		assertThat(secondPutDone).as("blocked while full").isFalse();

		assertThat(buffer.take()).isEqualTo("first");
		producer.join(2000);
		assertThat(secondPutDone).isTrue();
		assertThat(buffer.take()).isEqualTo("second");
	}

	@Test
	void takeBlocksWhileEmptyAndCanBeInterrupted() throws Exception {
		BoundedBuffer<String> buffer = new BoundedBuffer<>(2);
		CountDownLatch interrupted = new CountDownLatch(1);
		Thread consumer = new Thread(() -> {
			try {
				buffer.take();
			} catch (InterruptedException e) {
				interrupted.countDown();
			}
		});
		consumer.start();
		Thread.sleep(100);
		consumer.interrupt();
		assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void manyProducersAndConsumersDeliverEveryItemExactlyOnce() throws Exception {
		BoundedBuffer<Integer> buffer = new BoundedBuffer<>(16);
		int producers = 4, consumers = 4, perProducer = 25_000;
		ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
		ConcurrentHashMap<Integer, Boolean> seen = new ConcurrentHashMap<>();
		List<Future<?>> tasks = new ArrayList<>();
		for (int p = 0; p < producers; p++) {
			int base = p * perProducer;
			tasks.add(pool.submit(() -> {
				for (int i = 0; i < perProducer; i++) {
					buffer.put(base + i);
				}
				return null;
			}));
		}
		for (int c = 0; c < consumers; c++) {
			tasks.add(pool.submit(() -> {
				for (int i = 0; i < perProducer; i++) {
					Integer item = buffer.take();
					assertThat(seen.put(item, true)).as("delivered twice: %s", item).isNull();
				}
				return null;
			}));
		}
		for (Future<?> f : tasks) {
			f.get(20, TimeUnit.SECONDS);
		}
		pool.shutdown();
		assertThat(seen).hasSize(producers * perProducer);
		assertThat(buffer.size()).isZero();
	}

	@Test
	void rejectsNull() {
		BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
		assertThatThrownBy(() -> buffer.put(null)).isInstanceOf(NullPointerException.class);
	}

}
