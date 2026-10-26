package dojo.design.l1;

import dojo.Kata;

/**
 * KATA design L1 "Estimates" (tracks/15-system-design/L1-basics.md, section 2).
 *
 * Back-of-the-envelope numbers, the first five minutes of every design interview:
 * - averageQps: requests per day / 86,400 (seconds per day), as a double;
 * - peakQps: averageQps * peakFactor, rounded UP to a whole request;
 * - storageBytes: newRecordsPerDay * bytesPerRecord * retentionDays * replicationFactor (long:
 *   it overflows int quickly);
 * - serversNeeded: how many servers handle peakQps when one server does qpsPerServer at
 *   most targetUtilisation (e.g. 0.6, headroom for spikes and failures) of its capacity, PLUS
 *   one spare (N+1), rounded up; at least 2 for availability.
 * - humanBytes: 1,500 -> "1.5 KB", 2_500_000_000L -> "2.5 GB" (decimal units, B KB MB GB TB PB,
 *   one decimal, values below 1 KB as "999 B").
 */
public final class Estimates {

	private Estimates() {
	}

	public static double averageQps(long requestsPerDay) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 2");
	}

	public static long peakQps(long requestsPerDay, double peakFactor) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 2");
	}

	public static long storageBytes(long newRecordsPerDay, long bytesPerRecord, int retentionDays, int replicationFactor) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 2");
	}

	public static int serversNeeded(long peakQps, long qpsPerServer, double targetUtilisation) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 2");
	}

	public static String humanBytes(long bytes) {
		return Kata.todo("tracks/15-system-design/L1-basics.md section 2");
	}

}
