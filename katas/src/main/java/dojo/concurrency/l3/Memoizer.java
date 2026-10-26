package dojo.concurrency.l3;

import dojo.Kata;
import java.util.function.Function;

/**
 * KATA concurrency L3 "Memoizer" (tracks/02-concurrency/L3-production.md, section 5: cache stampedes).
 *
 * A thread-safe cache in front of an expensive computation (a slow query, a remote call).
 * The production problem it must solve: a popular key expires, and 500 requests arrive in the
 * same second. A naive cache runs the expensive call 500 times (a "cache stampede" or "thundering
 * herd") and can take the database down. Requirements:
 * - SINGLE-FLIGHT: concurrent get(k) calls for the same key run the computation ONCE; the others
 *   wait for that result.
 * - Different keys compute IN PARALLEL (no global lock around the computation).
 * - A computation that throws is NOT cached: the exception (a RuntimeException) reaches every
 *   waiting caller unchanged, and a later get(k) tries again.
 */
public final class Memoizer<K, V> {

	public Memoizer(Function<K, V> compute) {
	}

	public V get(K key) {
		return Kata.todo("tracks/02-concurrency/L3-production.md, section 5");
	}

}
