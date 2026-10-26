/** Run: java tracks/01-java-core/examples/IntegerCache.java  (L1, section 4) */
public class IntegerCache {
	public static void main(String[] args) {
		Integer a = 127, b = 127;
		Integer c = 128, d = 128;
		System.out.println("127 == 127 : " + (a == b));      // true: Integer.valueOf caches -128..127
		System.out.println("128 == 128 : " + (c == d));      // false: two different objects
		System.out.println("128 equals : " + c.equals(d));   // true: compares the values

		Long big = 1000L;
		System.out.println("Long 1000 equals int 1000 ? " + big.equals(1000)); // false: Integer is not a Long

		Integer missing = null;
		try {
			int x = missing; // auto-unboxing calls missing.intValue()
			System.out.println(x);
		} catch (NullPointerException e) {
			System.out.println("unboxing null -> NullPointerException");
		}

		long start = System.nanoTime();
		Long sum = 0L; // boxed: every += creates a new Long object
		for (int i = 0; i < 10_000_000; i++) {
			sum += i;
		}
		long boxed = System.nanoTime() - start;
		start = System.nanoTime();
		long primitive = 0;
		for (int i = 0; i < 10_000_000; i++) {
			primitive += i;
		}
		long prim = System.nanoTime() - start;
		System.out.printf("boxed Long sum: %d ms, primitive long sum: %d ms (same result: %b)%n",
				boxed / 1_000_000, prim / 1_000_000, sum == primitive);
	}
}
