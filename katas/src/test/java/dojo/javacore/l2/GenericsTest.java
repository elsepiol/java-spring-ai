package dojo.javacore.l2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L2")
class GenericsTest {

	@Test
	void copiesIntegersIntoAListOfNumbers() {
		List<Integer> ints = List.of(1, 2);
		List<Number> numbers = new ArrayList<>(List.of(0.5));
		Generics.copyInto(ints, numbers); // needs ? extends T and ? super T
		assertThat(numbers).containsExactly(0.5, 1, 2);
	}

	@Test
	void maxByWorksWithAComparatorOfASuperType() {
		Comparator<Number> byDouble = Comparator.comparingDouble(Number::doubleValue);
		List<Integer> ints = List.of(3, 9, 4);
		assertThat(Generics.<Integer>maxBy(ints, byDouble)).contains(9);
		assertThat(Generics.maxBy(List.<Integer>of(), byDouble)).isEmpty();
	}

	@Test
	void groupByKeepsOrderInsideGroups() {
		Map<Integer, List<String>> byLength = Generics.groupBy(List.of("ab", "c", "de", "f", "ghi"), String::length);
		assertThat(byLength.get(2)).containsExactly("ab", "de");
		assertThat(byLength.get(1)).containsExactly("c", "f");
		assertThat(byLength.get(3)).containsExactly("ghi");
	}

	@Test
	void typedRegistryNeedsNoCasts() {
		Generics.TypedRegistry registry = new Generics.TypedRegistry();
		registry.put(String.class, "jdbc:postgresql://db/shop");
		registry.put(Duration.class, Duration.ofSeconds(5));

		String url = registry.get(String.class).orElseThrow(); // no cast
		Duration timeout = registry.get(Duration.class).orElseThrow();
		assertThat(url).startsWith("jdbc:");
		assertThat(timeout).hasSeconds(5);
		assertThat(registry.get(Integer.class)).isEmpty();
	}

}
