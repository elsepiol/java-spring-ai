import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Run: java tracks/02-concurrency/examples/ExecutorBasics.java  (L2, sections 1-2) */
public class ExecutorBasics {
	public static void main(String[] args) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			Future<Integer> answer = pool.submit(() -> {
				Thread.sleep(200);
				return 42;
			});
			System.out.println("submitted; doing other work...");
			System.out.println("result: " + answer.get(1, TimeUnit.SECONDS)); // blocks until done (or timeout)

			Future<Integer> failing = pool.submit(() -> {
				throw new IllegalStateException("boom");
			});
			try {
				failing.get();
			} catch (ExecutionException e) {
				System.out.println("a task's exception arrives wrapped: " + e.getCause());
			}

			pool.execute(() -> {
				throw new IllegalStateException("lost?"); // execute(): printed by the thread's handler, nobody else sees it
			});

			List<Callable<String>> jobs = List.of(() -> name("a"), () -> name("b"), () -> name("c"));
			for (Future<String> f : pool.invokeAll(jobs)) {
				System.out.println("invokeAll: " + f.get());
			}
		} finally {
			pool.shutdown(); // stop accepting tasks; let running ones finish
			if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // interrupt what is still running
			}
		}
		System.out.println("pool terminated: " + pool.isTerminated());
	}

	static String name(String job) {
		return job + " ran on " + Thread.currentThread().getName();
	}
}
