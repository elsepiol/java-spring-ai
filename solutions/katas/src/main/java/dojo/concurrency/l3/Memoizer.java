package dojo.concurrency.l3;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Reference solution (Brian Goetz's Memoizer, from "Java Concurrency in Practice", with
 * CompletableFuture). The map holds FUTURES, not values: the first caller installs a future
 * with putIfAbsent and runs the computation outside any lock; everyone else finds the future and
 * waits on it.
 *
 * Why not just map.computeIfAbsent(key, compute)? It is single-flight too, but it runs the
 * computation while holding a lock on part of the map: a slow call blocks other keys in the
 * same bin, and a computation that touches the same map can deadlock or throw. Fine for fast
 * functions, wrong for remote calls.
 */
public final class Memoizer<K, V> {

	private final ConcurrentHashMap<K, CompletableFuture<V>> cache = new ConcurrentHashMap<>();
	private final Function<K, V> compute;

	public Memoizer(Function<K, V> compute) {
		this.compute = compute;
	}

	public V get(K key) {
		CompletableFuture<V> future = cache.get(key);
		if (future == null) {
			CompletableFuture<V> mine = new CompletableFuture<>();
			future = cache.putIfAbsent(key, mine);
			if (future == null) { // we won the race: we compute, outside any lock
				future = mine;
				try {
					mine.complete(compute.apply(key));
				} catch (RuntimeException e) {
					cache.remove(key, mine); // do not cache failures
					mine.completeExceptionally(e);
				}
			}
		}
		try {
			return future.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof RuntimeException cause) {
				throw cause; // unchanged, as the caller would see without the cache
			}
			throw e;
		}
	}

}
