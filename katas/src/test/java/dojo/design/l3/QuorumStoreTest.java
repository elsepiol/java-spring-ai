package dojo.design.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.design.l3.QuorumStore.QuorumException;

@Tag("design")
@Tag("L3")
class QuorumStoreTest {

	@Test
	void readsWhatWasWritten() {
		QuorumStore s = new QuorumStore(3, 2, 2);
		assertThat(s.get("cart:1")).isEmpty();
		s.put("cart:1", "book");
		assertThat(s.get("cart:1")).contains("book");
		s.put("cart:1", "book,pen");
		assertThat(s.get("cart:1")).contains("book,pen");
		assertThat(s.versionAt(0, "cart:1")).isEqualTo(2);
	}

	@Test
	void overlappingQuorumsSeeTheLatestWriteDespiteAFailure() {
		QuorumStore s = new QuorumStore(3, 2, 2); // R + W = 4 > 3
		s.put("k", "v1");
		s.setUp(0, false);
		s.put("k", "v2"); // only replicas 1 and 2 have v2
		s.setUp(0, true);
		s.setUp(2, false); // the read asks 0 (stale) and 1 (fresh)
		assertThat(s.get("k")).contains("v2");
	}

	@Test
	void readRepairFixesStaleReplicas() {
		QuorumStore s = new QuorumStore(3, 2, 2);
		s.put("k", "v1");
		s.setUp(0, false);
		s.put("k", "v2");
		s.setUp(0, true);
		assertThat(s.versionAt(0, "k")).isEqualTo(1);
		s.setUp(2, false);
		s.get("k");
		assertThat(s.versionAt(0, "k")).as("repaired during the read").isEqualTo(2);
	}

	@Test
	void nonOverlappingQuorumsCanReadStaleData() {
		QuorumStore s = new QuorumStore(3, 1, 1); // R + W = 2 <= 3: fast, but only eventually consistent
		s.put("k", "v1");
		s.setUp(0, false);
		s.setUp(1, false);
		s.put("k", "v2"); // only replica 2 has v2
		s.setUp(0, true);
		s.setUp(1, true);
		assertThat(s.get("k")).as("the first answering replica is stale").contains("v1");
	}

	@Test
	void failsWhenAQuorumIsNotReachable() {
		QuorumStore s = new QuorumStore(3, 2, 2);
		s.put("k", "v1");
		s.setUp(1, false);
		s.setUp(2, false);
		assertThatThrownBy(() -> s.put("k", "v2")).isInstanceOf(QuorumException.class);
		assertThat(s.versionAt(0, "k")).as("a failed write is not rolled back").isEqualTo(2);
		assertThatThrownBy(() -> s.get("k")).isInstanceOf(QuorumException.class);
	}

	@Test
	void versionsKeepIncreasingAcrossPartialFailures() {
		QuorumStore s = new QuorumStore(5, 3, 3);
		s.put("k", "a");
		s.setUp(4, false);
		s.put("k", "b");
		s.setUp(4, true);
		s.put("k", "c");
		assertThat(s.versionAt(4, "k")).isEqualTo(3);
		assertThat(s.get("k")).contains("c");
	}

}
