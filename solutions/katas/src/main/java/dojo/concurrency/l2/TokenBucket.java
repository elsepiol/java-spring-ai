package dojo.concurrency.l2;

import java.util.function.LongSupplier;

/**
 * Reference solution, with a lock. Tokens are kept as a double so fractions of a token carry over.
 * (A lock-free version keeps an immutable State(tokens, lastNanos) in an AtomicReference and
 * retries compareAndSet; under heavy contention the lock is usually just as fast.)
 */
public final class TokenBucket {

	private final long capacity;
	private final double tokensPerNano;
	private final LongSupplier nanoClock;
	private double tokens; // guarded by this
	private long lastRefill; // guarded by this

	public TokenBucket(long capacity, double tokensPerSecond, LongSupplier nanoClock) {
		if (capacity < 1 || tokensPerSecond <= 0) {
			throw new IllegalArgumentException("capacity >= 1 and tokensPerSecond > 0 required");
		}
		this.capacity = capacity;
		this.tokensPerNano = tokensPerSecond / 1_000_000_000.0;
		this.nanoClock = nanoClock;
		this.tokens = capacity;
		this.lastRefill = nanoClock.getAsLong();
	}

	public synchronized boolean tryAcquire() {
		refill();
		if (tokens >= 1) { // check-then-act: only safe because the whole method holds the lock
			tokens -= 1;
			return true;
		}
		return false;
	}

	public synchronized long available() {
		refill();
		return (long) tokens;
	}

	private void refill() {
		long now = nanoClock.getAsLong();
		long elapsed = now - lastRefill; // nanoTime may only be compared by difference
		if (elapsed > 0) {
			tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
			lastRefill = now;
		}
	}

}
