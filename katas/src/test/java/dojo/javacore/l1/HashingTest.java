package dojo.javacore.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L1")
class HashingTest {

	@Test
	void twoSumFindsThePair() {
		assertThat(Hashing.twoSum(new int[] { 3, 8, 4, 1 }, 5)).containsExactly(2, 3);
		assertThat(Hashing.twoSum(new int[] { 2, 7, 11, 15 }, 9)).containsExactly(0, 1);
		assertThat(Hashing.twoSum(new int[] { 3, 3 }, 6)).containsExactly(0, 1);
		assertThat(Hashing.twoSum(new int[] { -4, 10, 1 }, 6)).containsExactly(0, 1);
	}

	@Test
	void twoSumReturnsAnEmptyArrayWhenThereIsNoPair() {
		assertThat(Hashing.twoSum(new int[] { 1, 2, 3 }, 100)).isEmpty();
		assertThat(Hashing.twoSum(new int[] {}, 0)).isEmpty();
		assertThat(Hashing.twoSum(new int[] { 5 }, 10)).as("one element cannot pair with itself").isEmpty();
	}

	@Test
	void twoSumIsLinear() {
		int[] big = IntStream.range(0, 200_000).map(i -> i * 2).toArray(); // even numbers only
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
			assertThat(Hashing.twoSum(big, -1)).isEmpty(); // odd target: no pair, the worst case
			assertThat(Hashing.twoSum(big, 799_994)).containsExactly(199_998, 199_999); // the last two
		});
	}

	@Test
	void firstUniqueIndex() {
		assertThat(Hashing.firstUniqueIndex("swiss")).isEqualTo(1);
		assertThat(Hashing.firstUniqueIndex("leetcode")).isEqualTo(0);
		assertThat(Hashing.firstUniqueIndex("aabb")).isEqualTo(-1);
		assertThat(Hashing.firstUniqueIndex("")).isEqualTo(-1);
	}

	@Test
	void firstUniqueIndexIsLinear() {
		String s = "ab".repeat(100_000) + "c";
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertThat(Hashing.firstUniqueIndex(s)).isEqualTo(200_000));
	}

}
