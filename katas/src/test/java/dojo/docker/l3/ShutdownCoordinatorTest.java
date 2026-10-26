package dojo.docker.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("docker")
@Tag("L3")
@Timeout(20)
class ShutdownCoordinatorTest {

	@Test
	void countsInFlightRequests() {
		ShutdownCoordinator c = new ShutdownCoordinator(Duration.ofSeconds(5));
		assertThat(c.isAcceptingRequests()).isTrue();
		assertThat(c.tryBegin()).isTrue();
		assertThat(c.tryBegin()).isTrue();
		assertThat(c.inFlight()).isEqualTo(2);
		c.end();
		assertThat(c.inFlight()).isEqualTo(1);
	}

	@Test
	void shutdownWithNothingInFlightReturnsAtOnce() throws Exception {
		ShutdownCoordinator c = new ShutdownCoordinator(Duration.ofSeconds(10));
		long start = System.nanoTime();
		assertThat(c.shutdown()).isTrue();
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
		assertThat(c.isAcceptingRequests()).isFalse();
		assertThat(c.tryBegin()).as("new requests are refused").isFalse();
	}

	@Test
	void waitsForInFlightRequestsToFinish() throws Exception {
		ShutdownCoordinator c = new ShutdownCoordinator(Duration.ofSeconds(10));
		assertThat(c.tryBegin()).isTrue();
		CompletableFuture<Boolean> drained = CompletableFuture.supplyAsync(() -> {
			try {
				return c.shutdown();
			}
			catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		});
		Thread.sleep(200);
		assertThat(drained).as("still one request running").isNotDone();
		assertThat(c.tryBegin()).isFalse();
		c.end();
		assertThat(drained.get(5, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void givesUpAfterTheDrainTimeout() throws Exception {
		ShutdownCoordinator c = new ShutdownCoordinator(Duration.ofMillis(300));
		c.tryBegin();
		long start = System.nanoTime();
		assertThat(c.shutdown()).isFalse();
		Duration took = Duration.ofNanos(System.nanoTime() - start);
		assertThat(took).isGreaterThanOrEqualTo(Duration.ofMillis(300)).isLessThan(Duration.ofSeconds(3));
		assertThat(c.inFlight()).isEqualTo(1);
		c.end();
		assertThat(c.shutdown()).as("a second call just waits again").isTrue();
	}

	@Test
	void isThreadSafeUnderLoad() throws Exception {
		ShutdownCoordinator c = new ShutdownCoordinator(Duration.ofSeconds(10));
		AtomicInteger handled = new AtomicInteger();
		AtomicInteger refused = new AtomicInteger();
		CountDownLatch go = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < 8; t++) {
			threads.add(Thread.ofPlatform().start(() -> {
				try {
					go.await();
				}
				catch (InterruptedException e) {
					return;
				}
				for (int i = 0; i < 5_000; i++) {
					if (c.tryBegin()) {
						try {
							handled.incrementAndGet();
						}
						finally {
							c.end();
						}
					}
					else {
						refused.incrementAndGet();
					}
				}
			}));
		}
		go.countDown();
		Thread.sleep(5);
		assertThat(c.shutdown()).isTrue();
		assertThat(c.inFlight()).isZero();
		for (Thread t : threads) {
			t.join();
		}
		assertThat(c.inFlight()).isZero();
		assertThat(handled.get() + refused.get()).isEqualTo(40_000);
	}

}
