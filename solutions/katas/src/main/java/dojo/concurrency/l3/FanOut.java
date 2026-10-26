package dojo.concurrency.l3;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reference solution. A CompletionService hands back futures in COMPLETION order, so the first
 * failure is seen as soon as it happens (waiting on the futures in task order would only notice
 * it after every earlier task finished). Results are still returned in task order.
 * Structured concurrency (StructuredTaskScope, preview in 21) packages exactly this pattern.
 */
public final class FanOut {

	public static final class FanOutException extends RuntimeException {
		public FanOutException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private FanOut() {
	}

	public static <T> List<T> callAll(List<? extends Callable<T>> tasks, Duration timeout) {
		long deadline = System.nanoTime() + timeout.toNanos();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			CompletionService<T> completion = new ExecutorCompletionService<>(executor);
			List<Future<T>> futures = new ArrayList<>(tasks.size());
			for (Callable<T> task : tasks) {
				futures.add(completion.submit(task));
			}
			try {
				for (int done = 0; done < tasks.size(); done++) {
					Future<T> next = completion.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
					if (next == null) {
						throw new FanOutException("not done within " + timeout, new TimeoutException());
					}
					next.get(); // throws ExecutionException if that task failed
				}
				List<T> results = new ArrayList<>(futures.size());
				for (Future<T> f : futures) {
					results.add(f.resultNow());
				}
				return results;
			} catch (ExecutionException e) {
				throw new FanOutException("a task failed", e.getCause());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new FanOutException("interrupted", e);
			} finally {
				futures.forEach(f -> f.cancel(true)); // no-op for finished tasks; interrupts the others
			}
		} // close() waits for cancelled tasks to end: no thread outlives this method
	}

}
