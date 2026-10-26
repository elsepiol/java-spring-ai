/** Run: java tracks/01-java-core/examples/StringPool.java  (L1, section 3) */
public class StringPool {
	public static void main(String[] args) {
		String a = "shop";                  // literal: from the string pool
		String b = "shop";                  // the same pooled object
		String c = new String("shop");      // a new object on the heap
		String d = "sh" + "op";             // constant expression: folded at compile time -> pooled
		String part = "sh";
		String e = part + "op";             // computed at runtime -> a new object

		System.out.println("a == b          : " + (a == b));          // true
		System.out.println("a == c          : " + (a == c));          // false
		System.out.println("a == d          : " + (a == d));          // true
		System.out.println("a == e          : " + (a == e));          // false
		System.out.println("a == e.intern() : " + (a == e.intern())); // true
		System.out.println("a.equals(e)     : " + a.equals(e));       // true: ALWAYS use equals for text

		String s = "immutable";
		s.toUpperCase();                    // returns a NEW string; s is unchanged
		System.out.println("s after toUpperCase(): " + s);

		long start = System.nanoTime();
		String slow = "";
		for (int i = 0; i < 20_000; i++) {
			slow += i;                      // copies the whole string every time: O(n^2)
		}
		long t1 = System.nanoTime() - start;
		start = System.nanoTime();
		StringBuilder fast = new StringBuilder();
		for (int i = 0; i < 20_000; i++) {
			fast.append(i);                 // amortised O(1) per append
		}
		long t2 = System.nanoTime() - start;
		System.out.printf("+= in a loop: %d ms, StringBuilder: %d ms, same text: %b%n", t1 / 1_000_000, t2 / 1_000_000,
				slow.equals(fast.toString()));
	}
}
