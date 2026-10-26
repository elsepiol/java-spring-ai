package dojo.concurrency.l2;

import dojo.Kata;
import java.util.function.LongSupplier;

/**
 * KATA concurrency L2 "TokenBucket" + DSA (tracks/02-concurrency/L2-practical.md, section 6).
 *
 * The rate-limiting algorithm used by API gateways (and the shop project's RateLimitFilter):
 * a bucket holds up to `capacity` tokens and starts full; each request takes one; tokens are
 * added back continuously at `tokensPerSecond`, never above capacity. Bursts up to capacity are
 * allowed; the long-run rate is capped.
 *
 * - Time comes from `nanoClock` (System::nanoTime in production), so tests can control it.
 * - Refill is computed lazily from elapsed time on each call; nothing is scheduled.
 * - Partial tokens are not lost: at 2 tokens/s, two calls 250 ms apart earn one token in total.
 * - THREAD-SAFE: 16 threads calling tryAcquire at the same instant on a bucket with 100 tokens
 *   must get exactly 100 successes. Use synchronized, or (harder, lock-free) an AtomicReference
 *   with compareAndSet.
 */
public final class TokenBucket {

	public TokenBucket(long capacity, double tokensPerSecond, LongSupplier nanoClock) {
		if (capacity < 1 || tokensPerSecond <= 0) {
			throw new IllegalArgumentException("capacity >= 1 and tokensPerSecond > 0 required");
		}
	}

	/** Takes one token if available. */
	public boolean tryAcquire() {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 6");
	}

	/** Whole tokens available right now (after refill). */
	public long available() {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 6");
	}

}
