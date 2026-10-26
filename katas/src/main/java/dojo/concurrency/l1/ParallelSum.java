package dojo.concurrency.l1;

import dojo.Kata;

/**
 * KATA concurrency L1 "ParallelSum" (tracks/02-concurrency/L1-basics.md, sections 2 and 5).
 *
 * Sum a large array with `threads` plain Threads (new Thread, start, join; no executors yet).
 * Split the array into `threads` contiguous slices; each thread sums its slice into ITS OWN slot of
 * a long[] (nothing shared is written by two threads, so no locks are needed); join them all; add
 * the slots. join() is also what makes the slots' values visible to the main thread.
 *
 * - threads may be larger than data.length (some slices are empty); threads < 1 is an
 *   IllegalArgumentException.
 * - If the calling thread is interrupted while joining, restore its interrupt flag and throw
 *   IllegalStateException (L1 section 6: never swallow an interrupt).
 */
public final class ParallelSum {

	private ParallelSum() {
	}

	public static long sum(int[] data, int threads) {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 5");
	}

}
