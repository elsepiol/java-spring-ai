package dojo.ops.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ops")
@Tag("L2")
class CommitGraphTest {

	//   A - B - C - D        main
	//        \
	//         E - F          feature
	private final CommitGraph g = new CommitGraph();

	@BeforeEach
	void history() {
		g.add("A");
		g.add("B", "A");
		g.add("C", "B");
		g.add("D", "C");
		g.add("E", "B");
		g.add("F", "E");
	}

	@Test
	void ancestry() {
		assertThat(g.isAncestor("A", "F")).isTrue();
		assertThat(g.isAncestor("B", "D")).isTrue();
		assertThat(g.isAncestor("C", "F")).isFalse();
		assertThat(g.isAncestor("F", "B")).isFalse();
		assertThat(g.isAncestor("D", "D")).as("reflexive").isTrue();
	}

	@Test
	void rangesAreWhatAMergeBringsIn() {
		assertThat(g.range("D", "F")).containsExactlyInAnyOrder("E", "F");
		assertThat(g.range("F", "D")).containsExactlyInAnyOrder("C", "D");
		assertThat(g.range("D", "B")).isEmpty();
	}

	@Test
	void fastForwardOnlyWhenTheTargetIsAnAncestor() {
		assertThat(g.canFastForward("B", "D")).isTrue();
		assertThat(g.canFastForward("D", "F")).as("diverged: needs a merge or a rebase").isFalse();
	}

	@Test
	void findsTheMergeBase() {
		assertThat(g.mergeBase("D", "F")).contains("B");
		assertThat(g.mergeBase("F", "D")).contains("B");
		assertThat(g.mergeBase("C", "D")).contains("C");
	}

	@Test
	void afterAMergeTheBaseMovesForward() {
		g.add("M", "D", "F"); // merge feature into main
		g.add("G", "F");      // more work on feature
		assertThat(g.mergeBase("M", "G")).contains("F");
		assertThat(g.range("M", "G")).containsExactly("G");
	}

	@Test
	void crissCrossMergesHaveSeveralBestBases() {
		g.add("X", "D", "F");
		g.add("Y", "F", "D");
		assertThat(g.mergeBase("X", "Y")).contains("D"); // D and F are both best; the smaller id
	}

	@Test
	void unrelatedHistoriesHaveNoBase() {
		g.add("Z");
		assertThat(g.mergeBase("Z", "D")).isEmpty();
	}

	@Test
	void rejectsUnknownCommits() {
		assertThatIllegalArgumentException().isThrownBy(() -> g.add("Q", "nope"));
		assertThatIllegalArgumentException().isThrownBy(() -> g.isAncestor("A", "nope"));
		assertThatIllegalArgumentException().isThrownBy(() -> g.mergeBase("nope", "A"));
	}

	@Test
	void handlesLongHistoriesWithoutRecursion() {
		CommitGraph big = new CommitGraph();
		big.add("c0");
		for (int i = 1; i < 200_000; i++) {
			big.add("c" + i, "c" + (i - 1));
		}
		assertThat(big.isAncestor("c0", "c199999")).isTrue();
		assertThat(big.range("c199990", "c199999")).hasSize(9);
	}

}
