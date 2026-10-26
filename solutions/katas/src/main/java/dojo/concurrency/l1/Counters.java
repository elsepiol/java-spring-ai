package dojo.concurrency.l1;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Reference solution. */
public final class Counters {

	public interface Counter {
		void increment();

		long get();
	}

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
		return new Counter() {
			private long count; // guarded by "this"

			@Override
			public synchronized void increment() {
				count++;
			}

			@Override
			public synchronized long get() { // reads must synchronize too, or they may see a stale value
				return count;
			}
		};
	}

	public static Counter atomicCounter() {
		AtomicLong count = new AtomicLong();
		return new Counter() {
			@Override
			public void increment() {
				count.incrementAndGet(); // a CAS loop: read, compute, swap only if nobody changed it meanwhile
			}

			@Override
			public long get() {
				return count.get();
			}
		};
	}

	public static Counter adderCounter() {
		LongAdder count = new LongAdder();
		return new Counter() {
			@Override
			public void increment() {
				count.increment(); // threads hit different cells, so they rarely contend
			}

			@Override
			public long get() {
				return count.sum(); // adds the cells: exact once increments have stopped
			}
		};
	}

}
