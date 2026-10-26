package dojo.concurrency.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L3")
class FanOutTest {

	static long millisOf(Runnable r) {
		long start = System.nanoTime();
		r.run();
		return (System.nanoTime() - start) / 1_000_000;
	}

	@Test
	void tenThousandSlowCallsFinishInAboutTheTimeOfOne() {
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (int i = 0; i < 10_000; i++) {
			int n = i;
			tasks.add(() -> {
				Thread.sleep(100); // waiting on I/O
				return n;
			});
		}
		List<Integer>[] result = new List[1];
		long ms = millisOf(() -> result[0] = FanOut.callAll(tasks, Duration.ofSeconds(10)));
		assertThat(result[0]).hasSize(10_000);
		assertThat(result[0].get(0)).isZero();
		assertThat(result[0].get(9_999)).isEqualTo(9_999);
		assertThat(ms).as("10,000 x 100 ms in parallel").isLessThan(3_000);
	}

	@Test
	void resultsComeBackInTaskOrderNotCompletionOrder() {
		List<Callable<String>> tasks = List.of(() -> {
			Thread.sleep(300);
			return "slow";
		}, () -> "fast");
		assertThat(FanOut.callAll(tasks, Duration.ofSeconds(5))).containsExactly("slow", "fast");
	}

	@Test
	void aFailureFailsFastAndCancelsTheRest() throws Exception {
		AtomicInteger interrupted = new AtomicInteger();
		List<Callable<String>> tasks = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			tasks.add(() -> {
				try {
					Thread.sleep(10_000);
				} catch (InterruptedException e) {
					interrupted.incrementAndGet();
					throw e;
				}
				return "late";
			});
		}
		tasks.add(() -> {
			Thread.sleep(100);
			throw new IllegalStateException("shard 3 is down");
		});
		long ms = millisOf(() -> assertThatThrownBy(() -> FanOut.callAll(tasks, Duration.ofSeconds(30)))
				.isInstanceOf(FanOut.FanOutException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("shard 3 is down"));
		assertThat(ms).as("did not wait for the slow tasks").isLessThan(3_000);
		Thread.sleep(200);
		assertThat(interrupted).as("the other tasks were cancelled").hasValue(20);
	}

	@Test
	void timesOut() {
		List<Callable<String>> tasks = List.of(() -> "quick", () -> {
			Thread.sleep(10_000);
			return "never";
		});
		long ms = millisOf(() -> assertThatThrownBy(() -> FanOut.callAll(tasks, Duration.ofMillis(300)))
				.isInstanceOf(FanOut.FanOutException.class)
				.hasCauseInstanceOf(TimeoutException.class));
		assertThat(ms).isLessThan(3_000);
	}

}
