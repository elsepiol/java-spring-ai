package dojo.javacore.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L1")
class BugHuntTest {

	@Test
	void isAdminWorksForNamesThatWereBuiltAtRuntime() {
		String typed = new StringBuilder("ad").append("min").toString(); // like text read from a request
		assertThat(BugHunt.isAdmin(typed)).isTrue();
		assertThat(BugHunt.isAdmin("Admin")).isFalse();
		assertThat(BugHunt.isAdmin(null)).isFalse();
	}

	@Test
	void sameQuantityWorksForLargeNumbersAndNulls() {
		assertThat(BugHunt.sameQuantity(5, 5)).isTrue();
		assertThat(BugHunt.sameQuantity(1000, 1000)).isTrue();
		assertThat(BugHunt.sameQuantity(null, null)).isTrue();
		assertThat(BugHunt.sameQuantity(null, 1)).isFalse();
		assertThat(BugHunt.sameQuantity(1, null)).isFalse();
	}

	@Test
	void averageKeepsTheFraction() {
		assertThat(BugHunt.average(new int[] { 1, 2 })).isEqualTo(1.5);
		assertThat(BugHunt.average(new int[] {})).isZero();
		assertThat(BugHunt.average(new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE })).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void withTagLeavesTheCallersListAlone() {
		List<String> mine = new ArrayList<>(List.of("new"));
		List<String> tagged = BugHunt.withTag(mine, "sale");
		assertThat(tagged).containsExactly("new", "sale");
		assertThat(mine).containsExactly("new");
	}

	@Test
	void totalDoesNotOverflow() {
		int[] amounts = new int[3];
		Arrays.fill(amounts, 1_000_000_000); // three orders of 10 million euros, in cents
		assertThat(BugHunt.total(amounts)).isEqualTo(3_000_000_000L);
	}

}
