package dojo.web.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.web.l3.CircuitBreaker.CallNotPermittedException;
import dojo.web.l3.CircuitBreaker.State;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L3")
class CircuitBreakerTest {

	private final AtomicLong now = new AtomicLong();
	private final CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10), now::get);
	private final AtomicInteger calls = new AtomicInteger();

	private final Supplier<String> failing = () -> {
		calls.incrementAndGet();
		throw new IllegalStateException("payment service timeout");
	};
	private final Supplier<String> working = () -> {
		calls.incrementAndGet();
		return "ok";
	};

	private void failTimes(int n) {
		for (int i = 0; i < n; i++) {
			assertThatThrownBy(() -> breaker.call(failing)).isInstanceOf(IllegalStateException.class);
		}
	}

	private void advanceSeconds(long s) {
		now.addAndGet(TimeUnit.SECONDS.toNanos(s));
	}

	@Test
	void opensAfterConsecutiveFailures() {
		assertThat(breaker.state()).isEqualTo(State.CLOSED);
		failTimes(2);
		assertThat(breaker.call(working)).isEqualTo("ok"); // a success resets the count
		failTimes(2);
		assertThat(breaker.state()).isEqualTo(State.CLOSED);
		failTimes(1);
		assertThat(breaker.state()).isEqualTo(State.OPEN);
	}

	@Test
	void whileOpenItFailsFastWithoutCalling() {
		failTimes(3);
		int before = calls.get();
		assertThatThrownBy(() -> breaker.call(working)).isInstanceOf(CallNotPermittedException.class);
		assertThat(calls.get()).as("the dependency was not called").isEqualTo(before);
	}

	@Test
	void afterTheOpenPeriodATrialSuccessCloses() {
		failTimes(3);
		advanceSeconds(9);
		assertThat(breaker.state()).isEqualTo(State.OPEN);
		advanceSeconds(1);
		assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);
		assertThat(breaker.call(working)).isEqualTo("ok");
		assertThat(breaker.state()).isEqualTo(State.CLOSED);
	}

	@Test
	void aFailedTrialOpensAgainForAnotherPeriod() {
		failTimes(3);
		advanceSeconds(10);
		failTimes(1); // the trial fails
		assertThat(breaker.state()).isEqualTo(State.OPEN);
		advanceSeconds(5);
		assertThatThrownBy(() -> breaker.call(working)).isInstanceOf(CallNotPermittedException.class);
		advanceSeconds(5);
		assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);
	}

	@Test
	void onlyOneTrialAtATime() throws Exception {
		failTimes(3);
		advanceSeconds(10);
		CountDownLatch trialRunning = new CountDownLatch(1);
		CountDownLatch finish = new CountDownLatch(1);
		CompletableFuture<String> trial = CompletableFuture.supplyAsync(() -> breaker.call(() -> {
			trialRunning.countDown();
			try {
				finish.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return "ok";
		}));
		assertThat(trialRunning.await(5, TimeUnit.SECONDS)).isTrue();
		assertThatThrownBy(() -> breaker.call(working)).isInstanceOf(CallNotPermittedException.class);
		finish.countDown();
		assertThat(trial.get(5, TimeUnit.SECONDS)).isEqualTo("ok");
		assertThat(breaker.state()).isEqualTo(State.CLOSED);
	}

}
