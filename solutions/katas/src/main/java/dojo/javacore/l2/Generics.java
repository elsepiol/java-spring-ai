package dojo.javacore.l2;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reference solution. */
public final class Generics {

	private Generics() {
	}

	public static <T> void copyInto(Collection<? extends T> src, Collection<? super T> dst) {
		dst.addAll(src); // reading from src gives Ts, writing Ts into dst is allowed
	}

	public static <T> Optional<T> maxBy(Collection<? extends T> items, Comparator<? super T> comparator) {
		T best = null;
		for (T item : items) {
			if (best == null || comparator.compare(item, best) > 0) {
				best = item;
			}
		}
		return Optional.ofNullable(best);
	}

	public static <T, K> Map<K, List<T>> groupBy(Collection<? extends T> items, Function<? super T, ? extends K> key) {
		return items.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
	}

	public static final class TypedRegistry {

		private final Map<Class<?>, Object> values = new HashMap<>();

		public <T> void put(Class<T> type, T value) {
			values.put(type, type.cast(value)); // cast checks at runtime what erasure cannot
		}

		public <T> Optional<T> get(Class<T> type) {
			return Optional.ofNullable(type.cast(values.get(type)));
		}

	}

}
