import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Run: java tracks/02-concurrency/examples/RaceCondition.java  (L1, sections 3-4) */
public class RaceCondition {
	static long unsafe;
	static long locked;
	static final Object LOCK = new Object();
	static final AtomicLong atomic = new AtomicLong();

	public static void main(String[] args) throws InterruptedException {
		int threads = 8, perThread = 1_000_000;
		List<Thread> all = new ArrayList<>();
		for (int t = 0; t < threads; t++) {
			Thread thread = new Thread(() -> {
				for (int i = 0; i < perThread; i++) {
					unsafe++;                       // read, add, write: three steps, interleaved
					synchronized (LOCK) {
						locked++;                   // one thread at a time
					}
					atomic.incrementAndGet();       // a single atomic compare-and-swap
				}
			});
			all.add(thread);
			thread.start();
		}
		for (Thread thread : all) {
			thread.join();
		}
		long expected = (long) threads * perThread;
		System.out.printf("expected     %,d%n", expected);
		System.out.printf("unsafe++     %,d   (lost %,d increments)%n", unsafe, expected - unsafe);
		System.out.printf("synchronized %,d%n", locked);
		System.out.printf("AtomicLong   %,d%n", atomic.get());
	}
}
