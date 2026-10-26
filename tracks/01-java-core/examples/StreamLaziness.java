import java.util.List;
import java.util.stream.Stream;

/** Run: java tracks/01-java-core/examples/StreamLaziness.java  (L2, section 5) */
public class StreamLaziness {
	public static void main(String[] args) {
		Stream<String> pipeline = Stream.of("ann", "bob", "cat", "dan")
				.filter(n -> {
					System.out.println("  filter " + n);
					return !n.startsWith("b");
				})
				.map(n -> {
					System.out.println("  map    " + n);
					return n.toUpperCase();
				});
		System.out.println("pipeline built, nothing printed yet: streams are lazy");

		System.out.println("findFirst -> " + pipeline.findFirst().orElseThrow()); // processes only "ann"

		Stream<Integer> once = List.of(1, 2, 3).stream();
		once.count();
		try {
			once.count();
		} catch (IllegalStateException e) {
			System.out.println("a stream can be consumed only once: " + e.getMessage());
		}

		// Element by element, not stage by stage: watch the order of the prints.
		Stream.of(3, 1, 2).peek(i -> System.out.println("  saw " + i)).map(i -> i * 10).forEach(i -> System.out.println("  got " + i));

		// sorted() is a barrier: it must see everything before it can emit the first element.
		Stream.of(3, 1, 2).peek(i -> System.out.println("  before sort " + i)).sorted().forEach(i -> System.out.println("  after sort " + i));
	}
}
