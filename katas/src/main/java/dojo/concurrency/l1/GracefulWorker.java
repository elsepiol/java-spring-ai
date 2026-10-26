package dojo.concurrency.l1;

import dojo.Kata;
import java.time.Duration;

/**
 * KATA concurrency L1 "GracefulWorker" (tracks/02-concurrency/L1-basics.md, sections 5-6:
 * visibility and interruption).
 *
 * A background thread that runs `step` over and over until stop() is called, like a queue
 * consumer or a poller (the shop's outbox poller is one).
 * - start(): starts the thread; calling it twice throws IllegalStateException.
 * - stop(): asks the worker to finish. It must stop within a second even if the step is blocked
 *   in sleep() or wait() (hint: a flag alone cannot wake a sleeping thread).
 * - A step that throws InterruptedException ends the loop. Any other exception from a step is
 *   counted in failures() and the loop goes on (one bad message must not kill the consumer).
 * - awaitStopped(timeout): waits for the thread to end; true if it ended.
 * - steps(): how many steps completed normally; readable from any thread.
 */
public final class GracefulWorker {

	@FunctionalInterface
	public interface Step {
		void run() throws InterruptedException;
	}

	public GracefulWorker(Step step) {
	}

	public void start() {
		Kata.todo("tracks/02-concurrency/L1-basics.md, section 6");
	}

	public void stop() {
		Kata.todo("tracks/02-concurrency/L1-basics.md, section 6");
	}

	public boolean awaitStopped(Duration timeout) throws InterruptedException {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 6");
	}

	public long steps() {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 6");
	}

	public long failures() {
		return Kata.todo("tracks/02-concurrency/L1-basics.md, section 6");
	}

}
