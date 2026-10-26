import java.math.BigDecimal;
import java.math.RoundingMode;

/** Run: java tracks/01-java-core/examples/FloatingMoney.java  (L1, section 2) */
public class FloatingMoney {
	public static void main(String[] args) {
		System.out.println("0.1 + 0.2        = " + (0.1 + 0.2));          // 0.30000000000000004
		System.out.println("0.1 + 0.2 == 0.3 ? " + (0.1 + 0.2 == 0.3));   // false

		double total = 0;
		for (int i = 0; i < 1000; i++) {
			total += 0.10; // a thousand 10-cent items
		}
		System.out.println("1000 x 0.10      = " + total);                // not 100.0

		long cents = 0;
		for (int i = 0; i < 1000; i++) {
			cents += 10;
		}
		System.out.println("1000 x 10 cents  = " + cents + " cents (exact)");

		BigDecimal price = new BigDecimal("19.99");                      // from a String, never new BigDecimal(19.99)
		BigDecimal withVat = price.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.HALF_EVEN);
		System.out.println("19.99 + 21% VAT  = " + withVat);
		System.out.println("new BigDecimal(0.1) = " + new BigDecimal(0.1)); // shows the binary approximation
		System.out.println("2.0 equals 2.00 ? " + new BigDecimal("2.0").equals(new BigDecimal("2.00"))
				+ ", compareTo == 0 ? " + (new BigDecimal("2.0").compareTo(new BigDecimal("2.00")) == 0));
	}
}
