import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Run: java tracks/02-concurrency/examples/Deadlock.java  (L3, section 3)
 * While it is stuck you can also run, in another terminal:  jcmd <pid> Thread.print
 * and read "Found one Java-level deadlock" at the end.
 */
public class Deadlock {
	static final Object ACCOUNT_A = new Object();
	static final Object ACCOUNT_B = new Object();

	public static void main(String[] args) throws Exception {
		Thread t1 = new Thread(() -> lockBoth(ACCOUNT_A, ACCOUNT_B), "transfer A->B");
		Thread t2 = new Thread(() -> lockBoth(ACCOUNT_B, ACCOUNT_A), "transfer B->A");
		t1.setDaemon(true);
		t2.setDaemon(true);
		t1.start();
		t2.start();
		Thread.sleep(500);

		ThreadMXBean mx = ManagementFactory.getThreadMXBean();
		long[] ids = mx.findDeadlockedThreads(); // what jstack / jcmd Thread.print do for you
		if (ids == null) {
			System.out.println("no deadlock this time (unlucky timing); run it again");
			return;
		}
		System.out.println("DEADLOCK between " + ids.length + " threads:");
		for (ThreadInfo info : mx.getThreadInfo(ids)) {
			System.out.printf("  '%s' holds a lock and waits for %s, owned by '%s'%n",
					info.getThreadName(), info.getLockName(), info.getLockOwnerName());
		}
		System.out.println("Fix: always take the locks in the same global order (e.g. by account id).");
	}

	static void lockBoth(Object first, Object second) {
		synchronized (first) {
			sleep(100); // make the bad interleaving (almost) certain
			synchronized (second) {
				System.out.println(Thread.currentThread().getName() + " done");
			}
		}
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
