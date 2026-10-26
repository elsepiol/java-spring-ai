import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Run: java tracks/02-concurrency/examples/ThreadLocalLeak.java  (L3, section 7) */
public class ThreadLocalLeak {
	static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

	public static void main(String[] args) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(1); // like Tomcat: threads are REUSED

		pool.submit(() -> handleRequest("alice", true)).get();
		pool.submit(() -> handleRequest(null, true)).get(); // an anonymous request on the same thread

		pool.submit(() -> handleRequest("bob", false)).get(); // this one cleans up
		pool.submit(() -> handleRequest(null, false)).get();

		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.SECONDS);
	}

	static void handleRequest(String user, boolean forgetToClean) {
		if (user != null) {
			CURRENT_USER.set(user);
		}
		try {
			System.out.printf("request for %-5s sees current user = %s%n", user, CURRENT_USER.get());
		} finally {
			if (!forgetToClean) {
				CURRENT_USER.remove(); // ALWAYS in finally when the thread is pooled
			}
		}
	}
}
