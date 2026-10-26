package dojo.design.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("design")
@Tag("L1")
class HashRingTest {

	private static final int KEYS = 100_000;

	private static HashRing ring(int nodes) {
		HashRing ring = new HashRing(200);
		for (int i = 0; i < nodes; i++) {
			ring.add("cache-" + i);
		}
		return ring;
	}

	private static Map<String, String> assignment(HashRing ring) {
		Map<String, String> owner = new HashMap<>();
		for (int i = 0; i < KEYS; i++) {
			owner.put("user:" + i, ring.nodeFor("user:" + i));
		}
		return owner;
	}

	@Test
	void sameKeySameNode() {
		HashRing ring = ring(3);
		assertThat(ring.nodeFor("user:42")).isEqualTo(ring.nodeFor("user:42"));
	}

	@Test
	void spreadsKeysEvenlyWithVirtualNodes() {
		Map<String, Long> counts = new HashMap<>();
		assignment(ring(4)).values().forEach(n -> counts.merge(n, 1L, Long::sum));
		assertThat(counts).hasSize(4);
		assertThat(counts.values()).allSatisfy(c -> assertThat(c).isBetween(KEYS / 4 * 80 / 100L, KEYS / 4 * 120 / 100L));
	}

	@Test
	void addingANodeMovesOnlyAboutOneNth() {
		HashRing ring = ring(4);
		Map<String, String> before = assignment(ring);
		ring.add("cache-4");
		Map<String, String> after = assignment(ring);
		long moved = before.keySet().stream().filter(k -> !before.get(k).equals(after.get(k))).count();
		assertThat(moved).as("about 1/5 of the keys, not 4/5 as with modulo").isBetween(KEYS * 15 / 100L, KEYS * 25 / 100L);
		assertThat(before.keySet().stream().filter(k -> !before.get(k).equals(after.get(k))))
			.as("moved keys go to the new node only")
			.allMatch(k -> after.get(k).equals("cache-4"));
	}

	@Test
	void removingANodeMovesOnlyItsKeys() {
		HashRing ring = ring(5);
		Map<String, String> before = assignment(ring);
		ring.remove("cache-2");
		Map<String, String> after = assignment(ring);
		for (String key : before.keySet()) {
			if (!before.get(key).equals("cache-2")) {
				assertThat(after.get(key)).isEqualTo(before.get(key));
			}
		}
		assertThat(after.values()).doesNotContain("cache-2");
	}

	@Test
	void validatesMembership() {
		HashRing ring = new HashRing(10);
		assertThatIllegalStateException().isThrownBy(() -> ring.nodeFor("k"));
		ring.add("a");
		assertThat(ring.nodeFor("anything")).isEqualTo("a");
		assertThatIllegalArgumentException().isThrownBy(() -> ring.add("a"));
		assertThatIllegalArgumentException().isThrownBy(() -> ring.remove("b"));
	}

}
