package dojo.design.l2;

import java.util.function.LongSupplier;

/** Reference solution for the Snowflake kata. */
public final class Snowflake {

	public static final long EPOCH = 1_767_225_600_000L; // 2026-01-01T00:00:00Z

	private static final int WORKER_BITS = 10;
	private static final int SEQUENCE_BITS = 12;
	private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
	private static final long MAX_BACKWARDS_MS = 5;

	private final long workerId;
	private final LongSupplier clock;
	private long lastTimestamp = -1;
	private long sequence;

	public Snowflake(int workerId, LongSupplier clock) {
		if (workerId < 0 || workerId >= (1 << WORKER_BITS)) {
			throw new IllegalArgumentException("worker id must be 0..1023");
		}
		this.workerId = workerId;
		this.clock = clock;
	}

	public synchronized long nextId() {
		long now = clock.getAsLong();
		if (now < lastTimestamp) {
			if (lastTimestamp - now > MAX_BACKWARDS_MS) {
				throw new IllegalStateException("clock moved backwards by " + (lastTimestamp - now) + " ms");
			}
			now = waitUntil(lastTimestamp);
		}
		if (now == lastTimestamp) {
			sequence = (sequence + 1) & MAX_SEQUENCE;
			if (sequence == 0) {
				now = waitUntil(lastTimestamp + 1); // 4096 ids used up in this millisecond
			}
		}
		else {
			sequence = 0;
		}
		lastTimestamp = now;
		return ((now - EPOCH) << (WORKER_BITS + SEQUENCE_BITS)) | (workerId << SEQUENCE_BITS) | sequence;
	}

	public static long[] parts(long id) {
		return new long[] { (id >>> (WORKER_BITS + SEQUENCE_BITS)) + EPOCH, (id >>> SEQUENCE_BITS) & ((1L << WORKER_BITS) - 1),
				id & MAX_SEQUENCE };
	}

	private long waitUntil(long target) {
		long now = clock.getAsLong();
		while (now < target) {
			Thread.onSpinWait();
			now = clock.getAsLong();
		}
		return now;
	}

}
