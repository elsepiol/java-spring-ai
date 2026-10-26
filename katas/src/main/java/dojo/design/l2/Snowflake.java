package dojo.design.l2;

import java.util.function.LongSupplier;

import dojo.Kata;

/**
 * KATA design L2 "Snowflake" (tracks/15-system-design/L2-practical.md, section 2).
 *
 * Unique, roughly time-ordered 64-bit ids without a central database sequence (Twitter's
 * Snowflake): | 0 | 41 bits: ms since EPOCH | 10 bits: worker id | 12 bits: sequence |
 * - nextId(): time = clock.getAsLong() (epoch millis); the same millisecond as the previous id
 *   -> sequence + 1; a new millisecond -> sequence 0;
 * - more than 4096 ids in one millisecond: WAIT (spin on the clock) until the next millisecond;
 * - the clock moving BACKWARDS (an NTP correction) must never produce duplicates: if it is behind
 *   the last timestamp by at most 5 ms, wait for it to catch up; more than that ->
 *   IllegalStateException (refuse rather than risk duplicate ids);
 * - worker id 0..1023, else IllegalArgumentException;
 * - the static parts() method splits an id back into {timestamp millis, worker, sequence}.
 */
public final class Snowflake {

	public static final long EPOCH = 1_767_225_600_000L; // 2026-01-01T00:00:00Z

	public Snowflake(int workerId, LongSupplier clock) {
		Kata.todo("tracks/15-system-design/L2-practical.md section 2");
	}

	public long nextId() {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 2");
	}

	/** @return {epoch millis, worker id, sequence} */
	public static long[] parts(long id) {
		return Kata.todo("tracks/15-system-design/L2-practical.md section 2");
	}

}
