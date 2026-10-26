package dojo.concurrency.l2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L2")
class TokenBucketTest {

	private final AtomicLong now = new AtomicLong(1_000_000_000L); // any start value

	private void advanceMillis(long ms) {
		now.addAndGet(ms * 1_000_000L);
	}

	@Test
	void startsFullAndAllowsABurstUpToCapacity() {
		TokenBucket bucket = new TokenBucket(5, 1, now::get);
		for (int i = 0; i < 5; i++) {
			assertThat(bucket.tryAcquire()).isTrue();
		}
		assertThat(bucket.tryAcquire()).isFalse();
		assertThat(bucket.available()).isZero();
	}

	@Test
	void refillsAtTheConfiguredRate() {
		TokenBucket bucket = new TokenBucket(10, 2, now::get); // 2 per second
		for (int i = 0; i < 10; i++) {
			bucket.tryAcquire();
		}
		advanceMillis(1500);
		assertThat(bucket.available()).isEqualTo(3);
	}

	@Test
	void keepsFractionsOfATokenBetweenCalls() {
		TokenBucket bucket = new TokenBucket(1, 2, now::get); // a token every 500 ms
		assertThat(bucket.tryAcquire()).isTrue();
		advanceMillis(250);
		assertThat(bucket.tryAcquire()).as("half a token").isFalse();
		advanceMillis(250);
		assertThat(bucket.tryAcquire()).as("the two halves add up").isTrue();
	}

	@Test
	void neverHoldsMoreThanCapacity() {
		TokenBucket bucket = new TokenBucket(3, 100, now::get);
		advanceMillis(60_000); // a quiet minute
		assertThat(bucket.available()).isEqualTo(3);
	}

	@Test
	void underContentionGrantsExactlyTheTokensThatExist() throws Exception {
		TokenBucket bucket = new TokenBucket(100, 1, now::get); // the clock does not move: no refill
		AtomicInteger granted = new AtomicInteger();
		CountDownLatch gate = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < 16; t++) {
			Thread thread = new Thread(() -> {
				try {
					gate.await();
				} catch (InterruptedException e) {
					return;
				}
				for (int i = 0; i < 1_000; i++) {
					if (bucket.tryAcquire()) {
						granted.incrementAndGet();
					}
				}
			});
			thread.start();
			threads.add(thread);
		}
		gate.countDown();
		for (Thread thread : threads) {
			thread.join();
		}
		assertThat(granted).hasValue(100);
	}

}
