import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/** Run: java tracks/01-java-core/examples/FailFastIteration.java  (L2, section 7) */
public class FailFastIteration {
	public static void main(String[] args) {
		List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
		try {
			for (Integer n : numbers) {
				if (n % 2 == 0) {
					numbers.remove(n); // structural change during a for-each
				}
			}
		} catch (ConcurrentModificationException e) {
			System.out.println("for-each + remove -> ConcurrentModificationException");
		}

		numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
		Iterator<Integer> it = numbers.iterator();
		while (it.hasNext()) {
			if (it.next() % 2 == 0) {
				it.remove(); // the iterator's own remove is allowed
			}
		}
		System.out.println("iterator.remove: " + numbers);

		numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
		numbers.removeIf(n -> n % 2 == 0); // the modern way
		System.out.println("removeIf:        " + numbers);

		List<Integer> fixed = Arrays.asList(1, 2, 3);
		fixed.set(0, 99); // allowed: write-through to the array
		try {
			fixed.add(4);
		} catch (UnsupportedOperationException e) {
			System.out.println("Arrays.asList is fixed-size: add -> UnsupportedOperationException");
		}

		List<Integer> source = new ArrayList<>(List.of(1, 2));
		List<Integer> view = Collections.unmodifiableList(source);
		List<Integer> copy = List.copyOf(source);
		source.add(3);
		System.out.println("unmodifiable VIEW sees the change: " + view + ", List.copyOf does not: " + copy);
		try {
			List.of(1, null);
		} catch (NullPointerException e) {
			System.out.println("List.of rejects null elements");
		}
	}
}
