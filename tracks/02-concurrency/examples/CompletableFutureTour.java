import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Run: java tracks/02-concurrency/examples/CompletableFutureTour.java  (L2, section 4) */
public class CompletableFutureTour {
	public static void main(String[] args) {
		ExecutorService io = Executors.newFixedThreadPool(8); // a pool for blocking calls, not the common pool
		try {
			CompletableFuture<String> user = CompletableFuture.supplyAsync(() -> slow("user 7", 300), io);
			CompletableFuture<String> cart = CompletableFuture.supplyAsync(() -> slow("cart of 7", 300), io);

			// thenCombine: both in parallel, ~300 ms total instead of 600
			long start = System.nanoTime();
			String page = user.thenCombine(cart, (u, c) -> u + " + " + c).join();
			System.out.printf("combined '%s' in %d ms%n", page, (System.nanoTime() - start) / 1_000_000);

			// thenApply = map (sync transform); thenCompose = flatMap (the next step is itself async)
			String shipped = CompletableFuture.supplyAsync(() -> 99L, io)
					.thenApply(orderId -> "order " + orderId)
					.thenCompose(order -> CompletableFuture.supplyAsync(() -> slow("label for " + order, 100), io))
					.join();
			System.out.println(shipped);

			// errors flow down the chain until something handles them
			String recovered = CompletableFuture.supplyAsync(() -> {
				if (true) {
					throw new IllegalStateException("payment service down");
				}
				return "paid";
			}, io).exceptionally(e -> "fallback because " + e.getCause().getMessage()).join();
			System.out.println(recovered);

			// timeouts
			String timedOut = CompletableFuture.supplyAsync(() -> slow("never in time", 5_000), io)
					.completeOnTimeout("default price", 200, TimeUnit.MILLISECONDS).join();
			System.out.println(timedOut);

			// allOf: wait for many, then read them
			List<CompletableFuture<String>> all = List.of(1, 2, 3).stream()
					.map(i -> CompletableFuture.supplyAsync(() -> slow("shard " + i, 100), io)).toList();
			CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).join();
			System.out.println(all.stream().map(CompletableFuture::join).toList());
		} finally {
			io.shutdownNow();
		}
	}

	static String slow(String what, long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return what;
	}
}
