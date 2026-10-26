import java.util.ArrayList;
import java.util.List;

/** Run: java tracks/01-java-core/examples/PassByValue.java  (L1, section 4) */
public class PassByValue {

	static void reassign(List<String> list) {
		list = new ArrayList<>();   // changes the LOCAL copy of the reference only
		list.add("lost");
	}

	static void mutate(List<String> list) {
		list.add("kept");           // follows the copied reference to the SAME object
	}

	static void increment(int n) {
		n++;                        // the caller's int is a different variable
	}

	public static void main(String[] args) {
		List<String> names = new ArrayList<>();
		reassign(names);
		System.out.println("after reassign: " + names); // []
		mutate(names);
		System.out.println("after mutate:   " + names); // [kept]

		int count = 1;
		increment(count);
		System.out.println("count:          " + count); // 1

		// Java is ALWAYS pass-by-value. For objects, the value passed is the reference.
	}
}
