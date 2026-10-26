package dojo.ops.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dojo.ops.l3.Bisect.Verdict;

@Tag("ops")
@Tag("L3")
class BisectTest {

	private static List<Integer> history(int n) {
		return IntStream.range(0, n).boxed().toList();
	}

	/** Counts tests and remembers which commits were tested. */
	private static final class Tester implements Function<Integer, Verdict> {

		final int firstBad;
		final Set<Integer> untestable;
		final Set<Integer> tested = new HashSet<>();
		int calls;

		Tester(int firstBad, Set<Integer> untestable) {
			this.firstBad = firstBad;
			this.untestable = untestable;
		}

		@Override
		public Verdict apply(Integer commit) {
			calls++;
			assertThat(tested.add(commit)).as("commit %d tested twice", commit).isTrue();
			assertThat(commit).as("the known good and bad ends are never tested").isBetween(1, 998);
			if (untestable.contains(commit)) {
				return Verdict.SKIP;
			}
			return commit >= firstBad ? Verdict.BAD : Verdict.GOOD;
		}

	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 499, 500, 637, 998, 999 })
	void findsTheFirstBadCommitInLogarithmicTime(int firstBad) {
		Tester tester = new Tester(firstBad, Set.of());
		assertThat(Bisect.firstBad(history(1000), tester)).containsExactly(firstBad);
		assertThat(tester.calls).as("ceil(log2(999)) = 10").isLessThanOrEqualTo(10);
	}

	@Test
	void worksOnTinyHistories() {
		assertThat(Bisect.firstBad(List.of("good", "bad"), c -> {
			throw new AssertionError("nothing to test");
		})).containsExactly(1);
		assertThat(Bisect.firstBad(List.of("good", "x", "bad"), c -> Verdict.GOOD)).containsExactly(2);
	}

	@Test
	void stepsAroundCommitsThatDoNotBuild() {
		Tester tester = new Tester(637, Set.of(499, 500, 501));
		assertThat(Bisect.firstBad(history(1000), tester)).containsExactly(637);
		assertThat(tester.calls).isLessThanOrEqualTo(14);
	}

	@Test
	void reportsAllCandidatesWhenSkipsHideTheAnswer() {
		Tester tester = new Tester(637, Set.of(635, 636));
		assertThat(Bisect.firstBad(history(1000), tester)).containsExactly(635, 636, 637);
	}

}
