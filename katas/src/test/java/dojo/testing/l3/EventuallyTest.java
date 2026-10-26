package dojo.testing.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("testing")
@Tag("L3")
class EventuallyTest {

	private static final Duration POLL = Duration.ofMillis(10);

	@Test
	void returnsImmediatelyWhenTheConditionAlreadyHolds() {
		long start = System.nanoTime();
		Eventually.await(Duration.ofSeconds(5), POLL, () -> true);
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
	}

	@Test
	void returnsSoonAfterTheConditionBecomesTrue() throws Exception {
		AtomicBoolean done = new AtomicBoolean();
		Thread worker = Thread.ofVirtual().start(() -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				return;
			}
			done.set(true);
		});
		long start = System.nanoTime();
		Eventually.await(Duration.ofSeconds(5), POLL, done::get);
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
		worker.join();
	}

	@Test
	void failsAfterTheTimeoutWithoutWaitingLonger() {
		long start = System.nanoTime();
		assertThatThrownBy(() -> Eventually.await(Duration.ofMillis(200), Duration.ofMillis(50), () -> false))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("not met within");
		Duration took = Duration.ofNanos(System.nanoTime() - start);
		assertThat(took).isGreaterThanOrEqualTo(Duration.ofMillis(200)).isLessThan(Duration.ofSeconds(2));
	}

	@Test
	void keepsPollingWhileTheConditionThrows() {
		AtomicInteger calls = new AtomicInteger();
		Eventually.await(Duration.ofSeconds(5), POLL, () -> {
			if (calls.incrementAndGet() < 3) {
				throw new IllegalStateException("not there yet");
			}
			return true;
		});
		assertThat(calls).hasValue(3);
	}

	@Test
	void explainsTheFailureWithTheLastException() {
		assertThatThrownBy(() -> Eventually.await(Duration.ofMillis(100), POLL, () -> {
			throw new IllegalStateException("order 42 not found");
		})).isInstanceOf(AssertionError.class).cause().hasMessage("order 42 not found");
	}

	@Test
	void stopsWhenInterrupted() {
		Thread.currentThread().interrupt();
		try {
			long start = System.nanoTime();
			assertThatThrownBy(() -> Eventually.await(Duration.ofSeconds(5), POLL, () -> false)).isInstanceOf(AssertionError.class);
			assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
			assertThat(Thread.currentThread().isInterrupted()).as("interrupt flag restored").isTrue();
		}
		finally {
			Thread.interrupted(); // do not leak the flag into other tests
		}
	}

}
