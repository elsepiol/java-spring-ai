package dojo.concurrency.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L1")
class GracefulWorkerTest {

	private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
			Thread.sleep(5);
		}
	}

	@Test
	void runsStepsUntilStoppedEvenWhenTheyAreFast() throws Exception {
		AtomicInteger work = new AtomicInteger();
		GracefulWorker worker = new GracefulWorker(work::incrementAndGet); // a tight loop: tests visibility
		worker.start();
		waitUntil(() -> worker.steps() > 1000);

		worker.stop();

		assertThat(worker.awaitStopped(Duration.ofSeconds(1))).as("stopped within a second").isTrue();
		long after = worker.steps();
		Thread.sleep(50);
		assertThat(worker.steps()).as("no steps after stopping").isEqualTo(after);
	}

	@Test
	void stopWakesAStepThatIsSleeping() throws Exception {
		GracefulWorker worker = new GracefulWorker(() -> Thread.sleep(60_000)); // e.g. waiting for a message
		worker.start();
		Thread.sleep(100);

		long start = System.nanoTime();
		worker.stop();

		assertThat(worker.awaitStopped(Duration.ofSeconds(1))).isTrue();
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
	}

	@Test
	void aFailingStepDoesNotKillTheWorker() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		GracefulWorker worker = new GracefulWorker(() -> {
			if (calls.incrementAndGet() % 2 == 0) {
				throw new IllegalStateException("bad message");
			}
		});
		worker.start();
		waitUntil(() -> worker.failures() >= 10 && worker.steps() >= 10);
		worker.stop();
		assertThat(worker.awaitStopped(Duration.ofSeconds(1))).isTrue();
		assertThat(worker.failures()).isGreaterThanOrEqualTo(10);
		assertThat(worker.steps()).isGreaterThanOrEqualTo(10);
	}

	@Test
	void startingTwiceIsAnError() {
		GracefulWorker worker = new GracefulWorker(() -> Thread.sleep(10));
		worker.start();
		try {
			assertThatThrownBy(worker::start).isInstanceOf(IllegalStateException.class);
		} finally {
			worker.stop();
		}
	}

}
