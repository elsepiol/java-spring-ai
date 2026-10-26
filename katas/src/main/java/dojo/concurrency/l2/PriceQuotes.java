package dojo.concurrency.l2;

import dojo.Kata;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * KATA concurrency L2 "PriceQuotes" (tracks/02-concurrency/L2-practical.md, section 4: CompletableFuture).
 *
 * A checkout asks several shipping providers for a price. Each call is slow (an HTTP request)
 * and any of them can fail or hang. Rules:
 * - ask ALL providers IN PARALLEL on the given executor (never on the common pool for I/O);
 * - cheapestWithin: the lowest price among the answers that arrive within `timeout`; failed and
 *   late providers are ignored; empty if nobody answered in time. It returns after about `timeout`
 *   at most, however slow a provider is.
 * - withFallback: the primary's answer if it arrives within `timeout` without failing, otherwise
 *   the fallback's answer (a cached or default price). Exceptions from the fallback propagate.
 */
public final class PriceQuotes {

	private PriceQuotes() {
	}

	public static Optional<Long> cheapestWithin(List<Supplier<Long>> providers, Duration timeout, Executor executor) {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 4 (supplyAsync, completeOnTimeout, allOf)");
	}

	public static long withFallback(Supplier<Long> primary, Supplier<Long> fallback, Duration timeout, Executor executor) {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 4 (orTimeout, exceptionally)");
	}

}
