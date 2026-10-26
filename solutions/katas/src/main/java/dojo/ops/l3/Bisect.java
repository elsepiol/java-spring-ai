package dojo.ops.l3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Reference solution for the Bisect kata. */
public final class Bisect {

	public enum Verdict {
		GOOD, BAD, SKIP
	}

	private Bisect() {
	}

	public static <T> List<Integer> firstBad(List<T> commits, Function<T, Verdict> test) {
		if (commits.size() < 2) {
			throw new IllegalArgumentException("need a good and a bad commit");
		}
		int good = 0;
		int bad = commits.size() - 1;
		Set<Integer> skipped = new HashSet<>();
		while (true) {
			int mid = pick(good, bad, skipped);
			if (mid < 0) {
				break;
			}
			switch (test.apply(commits.get(mid))) {
				case GOOD -> good = mid;
				case BAD -> bad = mid;
				case SKIP -> skipped.add(mid);
			}
		}
		List<Integer> candidates = new ArrayList<>();
		for (int i = good + 1; i < bad; i++) {
			candidates.add(i); // only skipped commits can be left here
		}
		candidates.add(bad);
		return candidates;
	}

	/** The untested, non-skipped index strictly between the bounds that is closest to the middle; -1 if none. */
	private static int pick(int good, int bad, Set<Integer> skipped) {
		if (bad - good <= 1) {
			return -1;
		}
		int middle = good + (bad - good) / 2;
		for (int distance = 0; distance < bad - good; distance++) {
			int older = middle - distance;
			if (older > good && !skipped.contains(older)) {
				return older;
			}
			int newer = middle + distance;
			if (newer < bad && !skipped.contains(newer)) {
				return newer;
			}
		}
		return -1;
	}

}
