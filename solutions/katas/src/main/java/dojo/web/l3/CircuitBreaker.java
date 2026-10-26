package dojo.web.l3;

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Reference solution. State changes under a lock; the protected call itself runs outside it. */
public final class CircuitBreaker {

	public enum State { CLOSED, OPEN, HALF_OPEN }

	public static final class CallNotPermittedException extends RuntimeException {
		public CallNotPermittedException() {
			super("circuit is open");
		}
	}

	private final int failureThreshold;
	private final long openForNanos;
	private final LongSupplier nanoClock;
	private State state = State.CLOSED;
	private int failures;
	private long openedAt;
	private boolean trialInFlight;

	public CircuitBreaker(int failureThreshold, Duration openFor, LongSupplier nanoClock) {
		this.failureThreshold = failureThreshold;
		this.openForNanos = openFor.toNanos();
		this.nanoClock = nanoClock;
	}

	public <T> T call(Supplier<T> call) {
		boolean trial;
		synchronized (this) {
			advance();
			if (state == State.OPEN || (state == State.HALF_OPEN && trialInFlight)) {
				throw new CallNotPermittedException();
			}
			trial = state == State.HALF_OPEN;
			trialInFlight = trial;
		}
		try {
			T result = call.get();
			synchronized (this) {
				failures = 0;
				state = State.CLOSED;
				trialInFlight = false;
			}
			return result;
		} catch (RuntimeException e) {
			synchronized (this) {
				trialInFlight = false;
				if (trial || ++failures >= failureThreshold) {
					state = State.OPEN;
					openedAt = nanoClock.getAsLong();
					failures = 0;
				}
			}
			throw e;
		}
	}

	public synchronized State state() {
		advance();
		return state;
	}

	private void advance() {
		if (state == State.OPEN && nanoClock.getAsLong() - openedAt >= openForNanos) {
			state = State.HALF_OPEN;
		}
	}

}
