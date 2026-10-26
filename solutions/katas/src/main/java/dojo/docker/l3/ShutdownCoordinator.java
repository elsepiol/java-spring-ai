package dojo.docker.l3;

import java.time.Duration;

/** Reference solution for the ShutdownCoordinator kata: one monitor guards the flag and the counter. */
public final class ShutdownCoordinator {

	private final Duration drainTimeout;
	private boolean accepting = true;
	private int inFlight;

	public ShutdownCoordinator(Duration drainTimeout) {
		this.drainTimeout = drainTimeout;
	}

	public synchronized boolean tryBegin() {
		if (!accepting) {
			return false;
		}
		inFlight++;
		return true;
	}

	public synchronized void end() {
		if (inFlight > 0 && --inFlight == 0) {
			notifyAll();
		}
	}

	public synchronized boolean isAcceptingRequests() {
		return accepting;
	}

	public synchronized int inFlight() {
		return inFlight;
	}

	public synchronized boolean shutdown() throws InterruptedException {
		accepting = false;
		long deadline = System.nanoTime() + drainTimeout.toNanos();
		while (inFlight > 0) {
			long left = deadline - System.nanoTime();
			if (left <= 0) {
				return false;
			}
			wait(Math.max(1, left / 1_000_000));
		}
		return true;
	}

}
