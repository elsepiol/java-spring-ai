import java.util.ArrayList;
import java.util.List;

/**
 * Run with a GC log and a small heap:
 *   java -Xmx64m -Xlog:gc tracks/01-java-core/examples/HeapAndGc.java
 * (L3, sections 2-4.) Watch short-lived garbage die young, and a retained list fill the old generation.
 */
public class HeapAndGc {
	public static void main(String[] args) throws Exception {
		Runtime rt = Runtime.getRuntime();
		System.out.printf("max heap %d MB, GC in use: %s%n", rt.maxMemory() >> 20,
				java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream().map(b -> b.getName()).toList());

		// 1. Lots of short-lived garbage: cheap. Young collections reclaim it almost for free.
		long junk = 0;
		for (int i = 0; i < 5_000_000; i++) {
			junk += new byte[64].length;
		}
		System.out.printf("allocated %d MB of temporary arrays; used now: %d MB%n", junk >> 20, (rt.totalMemory() - rt.freeMemory()) >> 20);

		// 2. A "cache" that only grows: each array is still REACHABLE, so the GC must keep it.
		List<byte[]> leak = new ArrayList<>();
		try {
			while (true) {
				leak.add(new byte[1 << 20]); // 1 MB
				if (leak.size() % 10 == 0) {
					System.out.printf("retained %d MB, used %d MB%n", leak.size(), (rt.totalMemory() - rt.freeMemory()) >> 20);
				}
			}
		} catch (OutOfMemoryError e) {
			int retained = leak.size();
			leak = null; // let it go so we can print
			System.out.println("OutOfMemoryError after retaining " + retained + " MB: " + e.getMessage());
		}
	}
}
