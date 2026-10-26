import java.util.Random;

/**
 * Run: java tracks/05-web-http/examples/RetryStorm.java   (L3, section 2)
 * 1000 clients fail at t=0 and retry. Count retries per 100 ms bucket: without jitter they arrive in
 * synchronised waves; with full jitter they spread out.
 */
public class RetryStorm {
	public static void main(String[] args) {
		simulate("no jitter      ", false);
		simulate("full jitter    ", true);
		int layers = 3, attempts = 4;
		System.out.printf("%n%d layers each making up to %d attempts: one user request can become %d calls to the bottom service%n",
				layers, attempts, (int) Math.pow(attempts, layers));
	}

	static void simulate(String label, boolean jitter) {
		int[] buckets = new int[32];
		Random random = new Random(1);
		for (int client = 0; client < 1000; client++) {
			long t = 0;
			for (int attempt = 1; attempt <= 3; attempt++) {
				long ceiling = 100L << attempt; // 200, 400, 800 ms
				t += jitter ? (long) (random.nextDouble() * ceiling) : ceiling;
				int b = (int) (t / 100);
				if (b < buckets.length) {
					buckets[b]++;
				}
			}
		}
		StringBuilder sb = new StringBuilder(label + " retries per 100 ms: ");
		for (int b : buckets) {
			sb.append(b == 0 ? "." : b > 500 ? "#" : b > 100 ? "+" : "-");
		}
		System.out.println(sb + "   (# >500, + >100, - some, . none)");
	}
}
