package dojo.concurrency.l2;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Reference solution. */
public final class PriceQuotes {

	private PriceQuotes() {
	}

	public static Optional<Long> cheapestWithin(List<Supplier<Long>> providers, Duration timeout, Executor executor) {
		List<CompletableFuture<Long>> quotes = providers.stream()
				.map(p -> CompletableFuture.supplyAsync(p, executor)
						.exceptionally(failure -> null) // a failed provider is just "no answer"
						.completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS)) // so is a late one
				.toList();
		// Every future now completes within the timeout, with a price or null, so this cannot hang.
		CompletableFuture.allOf(quotes.toArray(CompletableFuture[]::new)).join();
		return quotes.stream().map(CompletableFuture::join).filter(Objects::nonNull).min(Long::compare);
	}

	public static long withFallback(Supplier<Long> primary, Supplier<Long> fallback, Duration timeout, Executor executor) {
		return CompletableFuture.supplyAsync(primary, executor)
				.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS) // completes exceptionally with TimeoutException
				.exceptionally(failure -> fallback.get())
				.join();
	}

}
