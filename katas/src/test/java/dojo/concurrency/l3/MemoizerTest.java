package dojo.concurrency.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L3")
class MemoizerTest {

	@Test
	void computesOnceAndCaches() {
		AtomicInteger calls = new AtomicInteger();
		Memoizer<String, Integer> m = new Memoizer<>(k -> {
			calls.incrementAndGet();
			return k.length();
		});
		assertThat(m.get("hello")).isEqualTo(5);
		assertThat(m.get("hello")).isEqualTo(5);
		assertThat(m.get("hi")).isEqualTo(2);
		assertThat(calls).hasValue(2);
	}

	@Test
	void fiftyConcurrentCallersForTheSameKeyCauseOneComputation() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		Memoizer<String, String> m = new Memoizer<>(k -> {
			calls.incrementAndGet();
			sleep(200); // a slow query
			return "report-" + k;
		});
		ExecutorService pool = Executors.newFixedThreadPool(50);
		CountDownLatch gate = new CountDownLatch(1);
		List<Future<String>> results = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			results.add(pool.submit(() -> {
				gate.await();
				return m.get("2026-10");
			}));
		}
		gate.countDown();
		for (Future<String> f : results) {
			assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo("report-2026-10");
		}
		pool.shutdown();
		assertThat(calls).as("single-flight").hasValue(1);
	}

	@Test
	void differentKeysComputeInParallel() {
		CountDownLatch bStarted = new CountDownLatch(1);
		Memoizer<String, String> m = new Memoizer<>(k -> {
			if (k.equals("a")) {
				// a waits until b's computation has STARTED: impossible if one lock serialises them
				try {
					if (!bStarted.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("b never started while a was computing");
					}
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			} else {
				bStarted.countDown();
			}
			return k.toUpperCase();
		});
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			ExecutorService pool = Executors.newFixedThreadPool(2);
			Future<String> a = pool.submit(() -> m.get("a"));
			Thread.sleep(100); // a is computing
			Future<String> b = pool.submit(() -> m.get("b"));
			assertThat(b.get()).isEqualTo("B");
			assertThat(a.get()).isEqualTo("A");
			pool.shutdown();
		});
	}

	@Test
	void failuresAreNotCached() {
		AtomicInteger calls = new AtomicInteger();
		Memoizer<String, String> m = new Memoizer<>(k -> {
			if (calls.incrementAndGet() == 1) {
				throw new IllegalStateException("database timeout");
			}
			return "ok";
		});
		assertThatThrownBy(() -> m.get("k")).isInstanceOf(IllegalStateException.class).hasMessage("database timeout");
		assertThat(m.get("k")).isEqualTo("ok");
		assertThat(calls).hasValue(2);
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

}
