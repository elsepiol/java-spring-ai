package dojo.concurrency.l1;

import dojo.Kata;

/**
 * KATA concurrency L1 "Counters" (tracks/02-concurrency/L1-basics.md, sections 3-4).
 *
 * UNSAFE below is what most people write first. count++ is three steps (read, add, write), so two
 * threads can read the same value and one increment is lost. Run
 * tracks/02-concurrency/examples/RaceCondition.java to watch it lose thousands.
 *
 * Make three correct counters, each a different tool:
 *   synchronizedCounter(): a lock (synchronized)
 *   atomicCounter():       compare-and-swap (AtomicLong)
 *   adderCounter():        striped counting (LongAdder): fastest when many threads increment
 */
public final class Counters {

	public interface Counter {
		void increment();

		long get();
	}

	/** Broken on purpose: for the example program. Do not "fix" this one. */
	public static final Counter UNSAFE = new Counter() {
		private long count;

		@Override
		public void increment() {
			count++;
		}

		@Override
		public long get() {
			return count;
		}
	};

	private Counters() {
	}

	public static Counter synchronizedCounter() {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 4 (synchronized)");
	}

	public static Counter atomicCounter() {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 4 (AtomicLong)");
	}

	public static Counter adderCounter() {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 4 (LongAdder)");
	}

}
