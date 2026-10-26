package dojo.concurrency.l3;

import dojo.Kata;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * KATA concurrency L3 "FanOut" (tracks/02-concurrency/L3-production.md, section 6: virtual threads).
 *
 * Call many slow, I/O-bound tasks at once (an HTTP call per item, a query per shard) and collect
 * the results IN THE ORDER OF THE TASKS. 10,000 tasks that each wait 100 ms must finish in about
 * a second, which a pool of 200 platform threads cannot do (10,000 / 200 x 100 ms = 5 s).
 * - Use virtual threads (Executors.newVirtualThreadPerTaskExecutor()), closed with try-with-resources.
 * - If any task fails, throw FanOutException with that failure as the cause, promptly, and cancel
 *   (interrupt) the tasks still running: nobody needs their results any more.
 * - If not everything is done within `timeout`, throw FanOutException whose cause is a
 *   java.util.concurrent.TimeoutException, and cancel the rest.
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
		return Kata.todo("tracks/02-concurrency/L3-production.md, section 6");
	}

}
