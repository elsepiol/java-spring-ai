package dojo.web.l3;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Reference solution: per client, the counts of the current and the previous fixed window. */
public final class SlidingWindowLimiter {

	private static final class Counter {
		long windowIndex;
		long current;
		long previous;
	}

	private final int limit;
	private final long windowNanos;
	private final LongSupplier nanoClock;
	private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>(); // in production: bounded (Caffeine) or Redis

	public SlidingWindowLimiter(int limit, Duration window, LongSupplier nanoClock) {
		this.limit = limit;
		this.windowNanos = window.toNanos();
		this.nanoClock = nanoClock;
	}

	public boolean tryAcquire(String clientId) {
		Counter c = counters.computeIfAbsent(clientId, k -> new Counter());
		synchronized (c) {
			long now = nanoClock.getAsLong();
			long index = now / windowNanos;
			if (index != c.windowIndex) {
				c.previous = index == c.windowIndex + 1 ? c.current : 0; // a gap of 2+ windows: nothing recent
				c.current = 0;
				c.windowIndex = index;
			}
			double elapsedFraction = (double) (now - index * windowNanos) / windowNanos;
			double estimate = c.previous * (1 - elapsedFraction) + c.current;
			if (estimate < limit) {
				c.current++;
				return true;
			}
			return false;
		}
	}

}
