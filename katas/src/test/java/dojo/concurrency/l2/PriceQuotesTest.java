package dojo.concurrency.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L2")
class PriceQuotesTest {

	private final ExecutorService io = Executors.newFixedThreadPool(8);

	@AfterEach
	void shutdown() {
		io.shutdownNow();
	}

	static Supplier<Long> answers(long price, long afterMillis) {
		return () -> {
			sleep(afterMillis);
			return price;
		};
	}

	static Supplier<Long> fails(long afterMillis) {
		return () -> {
			sleep(afterMillis);
			throw new IllegalStateException("provider down");
		};
	}

	static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	static long millisOf(Runnable r) {
		long start = System.nanoTime();
		r.run();
		return (System.nanoTime() - start) / 1_000_000;
	}

	@Test
	void picksTheCheapestAnswer() {
		assertThat(PriceQuotes.cheapestWithin(List.of(answers(700, 10), answers(450, 20), answers(900, 5)), Duration.ofSeconds(1), io))
				.contains(450L);
	}

	@Test
	void asksAllProvidersInParallel() {
		long ms = millisOf(() -> assertThat(PriceQuotes.cheapestWithin(
				List.of(answers(1, 300), answers(2, 300), answers(3, 300)), Duration.ofSeconds(2), io)).contains(1L));
		assertThat(ms).as("three 300 ms calls in parallel, not 900 ms in a row").isLessThan(750);
	}

	@Test
	void ignoresFailedAndLateProvidersAndDoesNotWaitForThem() {
		long ms = millisOf(() -> assertThat(PriceQuotes.cheapestWithin(
				List.of(answers(100, 5_000), fails(10), answers(800, 50)), Duration.ofMillis(300), io)).contains(800L));
		assertThat(ms).as("returns after about the timeout, not after the slow provider").isLessThan(1_500);
	}

	@Test
	void emptyWhenNobodyAnswersInTime() {
		assertThat(PriceQuotes.cheapestWithin(List.of(fails(1), answers(5, 5_000)), Duration.ofMillis(200), io)).isEmpty();
	}

	@Test
	void fallbackOnFailureAndOnTimeout() {
		assertThat(PriceQuotes.withFallback(answers(300, 10), () -> 999L, Duration.ofSeconds(1), io)).isEqualTo(300);
		assertThat(PriceQuotes.withFallback(fails(10), () -> 999L, Duration.ofSeconds(1), io)).isEqualTo(999);
		long ms = millisOf(() -> assertThat(PriceQuotes.withFallback(answers(300, 5_000), () -> 999L, Duration.ofMillis(200), io)).isEqualTo(999));
		assertThat(ms).isLessThan(1_500);
	}

	@Test
	void aFailingFallbackPropagates() {
		assertThatThrownBy(() -> PriceQuotes.withFallback(fails(1), () -> {
			throw new IllegalStateException("no cached price");
		}, Duration.ofSeconds(1), io)).hasRootCauseMessage("no cached price");
	}

}
