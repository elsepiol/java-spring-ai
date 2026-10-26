package dojo.concurrency.l1;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/** Reference solution. */
public final class GracefulWorker {

	@FunctionalInterface
	public interface Step {
		void run() throws InterruptedException;
	}

	private final Step step;
	private final AtomicLong steps = new AtomicLong();
	private final AtomicLong failures = new AtomicLong();
	private volatile boolean running = true; // volatile: the worker thread must SEE stop()'s write
	private Thread thread;

	public GracefulWorker(Step step) {
		this.step = step;
	}

	public synchronized void start() {
		if (thread != null) {
			throw new IllegalStateException("already started");
		}
		thread = new Thread(this::loop, "graceful-worker");
		thread.start();
	}

	private void loop() {
		while (running && !Thread.currentThread().isInterrupted()) {
			try {
				step.run();
				steps.incrementAndGet();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // keep the flag set, then leave
				return;
			} catch (RuntimeException e) {
				failures.incrementAndGet();
			}
		}
	}

	public synchronized void stop() {
		running = false;
		if (thread != null) {
			thread.interrupt(); // wakes a step blocked in sleep/wait/BlockingQueue.take
		}
	}

	public boolean awaitStopped(Duration timeout) throws InterruptedException {
		Thread t;
		synchronized (this) {
			t = thread;
		}
		if (t == null) {
			return true;
		}
		t.join(timeout.toMillis());
		return !t.isAlive();
	}

	public long steps() {
		return steps.get();
	}

	public long failures() {
		return failures.get();
	}

}
