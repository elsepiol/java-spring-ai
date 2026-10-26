package dojo.concurrency.l1;

/** Reference solution. */
public final class ParallelSum {

	private ParallelSum() {
	}

	public static long sum(int[] data, int threads) {
		if (threads < 1) {
			throw new IllegalArgumentException("threads must be at least 1");
		}
		long[] partial = new long[threads]; // one slot per thread: no two threads write the same slot
		Thread[] workers = new Thread[threads];
		int chunk = (data.length + threads - 1) / threads;
		for (int t = 0; t < threads; t++) {
			int from = Math.min(data.length, t * chunk);
			int to = Math.min(data.length, from + chunk);
			int slot = t;
			workers[t] = new Thread(() -> {
				long s = 0;
				for (int i = from; i < to; i++) {
					s += data[i];
				}
				partial[slot] = s;
			}, "sum-" + t);
			workers[t].start();
		}
		try {
			for (Thread w : workers) {
				w.join(); // happens-before: everything w wrote is visible after join returns
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted while summing", e);
		}
		long total = 0;
		for (long p : partial) {
			total += p;
		}
		return total;
	}

}
