package dojo.javacore.l2;

import dojo.Kata;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * KATA javacore L2 "Generics" (tracks/01-java-core/L2-practical.md, section 4: wildcards and PECS).
 *
 * The SIGNATURES are the lesson: they are already written with ? extends / ? super so that the
 * tests compile (e.g. copying a List<Integer> into a List<Number>). Implement the bodies, then
 * try changing a signature to plain List<T> and watch which test stops compiling.
 */
public final class Generics {

	private Generics() {
	}

	/** Adds every element of src to the end of dst. Producer extends, consumer super. */
	public static <T> void copyInto(Collection<? extends T> src, Collection<? super T> dst) {
		Kata.todo("tracks/01-java-core/L2-practical.md, section 4");
	}

	/** The largest element by the comparator; empty for an empty collection. */
	public static <T> Optional<T> maxBy(Collection<? extends T> items, Comparator<? super T> comparator) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 4");
	}

	/** Groups items by key, keeping the order of items inside each group. */
	public static <T, K> Map<K, List<T>> groupBy(Collection<? extends T> items, Function<? super T, ? extends K> key) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 4");
	}

	/** A type-safe "heterogeneous container": a map from Class<T> to a T, no casts at the call site. */
	public static final class TypedRegistry {

		public <T> void put(Class<T> type, T value) {
			Kata.todo("tracks/01-java-core/L2-practical.md, section 4 (type tokens)");
		}

		public <T> Optional<T> get(Class<T> type) {
			return Kata.todo("tracks/01-java-core/L2-practical.md, section 4 (Class.cast)");
		}

	}

}
