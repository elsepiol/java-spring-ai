/**
 * Run: java tracks/02-concurrency/examples/Visibility.java  (L1, section 5)
 *
 * Without volatile, the JIT may hoist the read of `running` out of the loop: the worker never
 * sees the main thread's write and spins forever. Whether it hangs depends on the JVM and
 * machine; on most HotSpot JVMs it does. A watchdog ends the demo after 3 seconds either way.
 */
public class Visibility {
	static boolean running = true;            // try adding volatile here
	static volatile boolean runningVolatile = true;

	public static void main(String[] args) throws InterruptedException {
		Thread plain = new Thread(() -> {
			long spins = 0;
			while (running) {
				spins++;                      // no synchronization, no volatile read inside the loop
			}
			System.out.println("plain worker saw the stop after " + spins + " spins");
		});
		Thread withVolatile = new Thread(() -> {
			long spins = 0;
			while (runningVolatile) {
				spins++;
			}
			System.out.println("volatile worker saw the stop after " + spins + " spins");
		});
		plain.setDaemon(true);
		withVolatile.setDaemon(true);
		plain.start();
		withVolatile.start();
		Thread.sleep(500);                    // let the JIT compile the loops

		running = false;
		runningVolatile = false;
		System.out.println("main: both flags set to false");

		withVolatile.join(3000);
		plain.join(3000);
		if (plain.isAlive()) {
			System.out.println("plain worker is STILL spinning: it never saw the write. That is a visibility bug.");
		}
	}
}
