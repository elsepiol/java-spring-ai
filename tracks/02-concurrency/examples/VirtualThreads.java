import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/** Run: java tracks/02-concurrency/examples/VirtualThreads.java  (L3, section 6) */
public class VirtualThreads {
	public static void main(String[] args) {
		int tasks = 10_000;
		time("platform pool of 200", Executors.newFixedThreadPool(200), tasks);
		time("virtual thread per task", Executors.newVirtualThreadPerTaskExecutor(), tasks);

		Thread v = Thread.ofVirtual().unstarted(() -> {});
		System.out.println("a virtual thread: " + v + ", isVirtual=" + v.isVirtual());
		System.out.println("CPU-bound work does NOT get faster with virtual threads: there are still only "
				+ Runtime.getRuntime().availableProcessors() + " cores.");
	}

	static void time(String label, ExecutorService executor, int tasks) {
		long start = System.nanoTime();
		try (executor) { // close() waits for all tasks
			IntStream.range(0, tasks).forEach(i -> executor.submit(() -> {
				Thread.sleep(Duration.ofMillis(100)); // stands for a blocking HTTP or JDBC call
				return i;
			}));
		}
		System.out.printf("%-24s %,d tasks x 100 ms blocking: %,d ms%n", label, tasks, (System.nanoTime() - start) / 1_000_000);
	}
}
