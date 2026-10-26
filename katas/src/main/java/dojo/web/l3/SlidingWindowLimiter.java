package dojo.web.l3;

import dojo.Kata;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * KATA web L3 "SlidingWindowLimiter" + DSA (tracks/05-web-http/L3-production.md, section 4).
 *
 * "At most `limit` requests per `window` per client". A FIXED window counter (reset at every full
 * minute) lets a client send `limit` at 12:00:59 and `limit` more at 12:01:00: twice the limit in
 * one second. The SLIDING WINDOW COUNTER approximation fixes that with two counters per client:
 *
 *   estimate = previousWindowCount * (fraction of the previous window still inside the sliding window)
 *              + currentWindowCount
 *   allow if estimate < limit (then count the request in the current window)
 *
 * e.g. window 60 s, limit 100, 15 s into the current window, previous window had 80, current 30:
 * estimate = 80 * 45/60 + 30 = 90 -> allowed.
 * Windows are aligned to multiples of `window` since time 0 of nanoClock. Clients are independent.
 * Memory: two counters per client, O(1) per request (compare: a log of timestamps per request).
 */
public final class SlidingWindowLimiter {

	public SlidingWindowLimiter(int limit, Duration window, LongSupplier nanoClock) {
	}

	public boolean tryAcquire(String clientId) {
		return Kata.todo("tracks/05-web-http/L3-production.md, section 4");
	}

}
