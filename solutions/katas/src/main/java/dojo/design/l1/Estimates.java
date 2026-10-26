package dojo.design.l1;

import java.util.Locale;

/** Reference solution for the Estimates kata. */
public final class Estimates {

	private static final String[] UNITS = { "B", "KB", "MB", "GB", "TB", "PB" };

	private Estimates() {
	}

	public static double averageQps(long requestsPerDay) {
		return requestsPerDay / 86_400.0;
	}

	public static long peakQps(long requestsPerDay, double peakFactor) {
		return (long) Math.ceil(averageQps(requestsPerDay) * peakFactor);
	}

	public static long storageBytes(long newRecordsPerDay, long bytesPerRecord, int retentionDays, int replicationFactor) {
		return Math.multiplyExact(Math.multiplyExact(Math.multiplyExact(newRecordsPerDay, bytesPerRecord), (long) retentionDays), (long) replicationFactor);
	}

	public static int serversNeeded(long peakQps, long qpsPerServer, double targetUtilisation) {
		int forLoad = (int) Math.ceil(peakQps / (qpsPerServer * targetUtilisation));
		return Math.max(2, forLoad + 1);
	}

	public static String humanBytes(long bytes) {
		if (bytes < 1000) {
			return bytes + " B";
		}
		double value = bytes;
		int unit = 0;
		while (value >= 1000 && unit < UNITS.length - 1) {
			value /= 1000;
			unit++;
		}
		return String.format(Locale.ROOT, "%.1f %s", value, UNITS[unit]);
	}

}
