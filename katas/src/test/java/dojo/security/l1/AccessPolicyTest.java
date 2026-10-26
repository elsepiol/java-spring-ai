package dojo.security.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("security")
@Tag("L1")
class AccessPolicyTest {

	private final AccessPolicy policy = new AccessPolicy();

	private final Caller alice = new Caller("alice", Set.of(Role.USER));
	private final Caller bob = new Caller("bob", Set.of(Role.USER));
	private final Caller support = new Caller("sam", Set.of(Role.USER, Role.SUPPORT));
	private final Caller admin = new Caller("root", Set.of(Role.USER, Role.ADMIN));

	private final Document alicesNote = new Document(1, "alice", false);
	private final Document alicesSecret = new Document(2, "alice", true);

	@Test
	void anonymousCallersMustAuthenticateFirst() {
		assertThat(policy.read(null, alicesNote)).isEqualTo(Decision.UNAUTHENTICATED);
		assertThat(policy.read(null, null)).isEqualTo(Decision.UNAUTHENTICATED);
		assertThat(policy.delete(null, alicesNote)).isEqualTo(Decision.UNAUTHENTICATED);
	}

	@Test
	void ownersReadAndDeleteTheirDocuments() {
		assertThat(policy.read(alice, alicesSecret)).isEqualTo(Decision.ALLOW);
		assertThat(policy.delete(alice, alicesSecret)).isEqualTo(Decision.ALLOW);
	}

	@Test
	void otherUsersCannotTellForeignDocumentsFromMissingOnes() {
		assertThat(policy.read(bob, alicesNote)).isEqualTo(Decision.NOT_FOUND);
		assertThat(policy.read(bob, null)).isEqualTo(Decision.NOT_FOUND);
		assertThat(policy.delete(bob, alicesNote)).isEqualTo(Decision.NOT_FOUND);
	}

	@Test
	void supportReadsOnlyNonConfidentialDocuments() {
		assertThat(policy.read(support, alicesNote)).isEqualTo(Decision.ALLOW);
		assertThat(policy.read(support, alicesSecret)).isEqualTo(Decision.NOT_FOUND);
	}

	@Test
	void supportMayReadButNotDelete() {
		assertThat(policy.delete(support, alicesNote)).isEqualTo(Decision.FORBIDDEN);
		assertThat(policy.delete(support, alicesSecret)).isEqualTo(Decision.NOT_FOUND);
	}

	@Test
	void adminsCanDoEverything() {
		assertThat(policy.read(admin, alicesSecret)).isEqualTo(Decision.ALLOW);
		assertThat(policy.delete(admin, alicesSecret)).isEqualTo(Decision.ALLOW);
	}

	@Test
	void missingDocumentsAreNotFoundForEveryLoggedInCaller() {
		assertThat(policy.read(admin, null)).isEqualTo(Decision.NOT_FOUND);
		assertThat(policy.delete(admin, null)).isEqualTo(Decision.NOT_FOUND);
	}

}
