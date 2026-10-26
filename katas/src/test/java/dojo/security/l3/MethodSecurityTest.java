package dojo.security.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import dojo.security.l3.notes.NoteSecurityConfig;
import dojo.security.l3.notes.NoteService;

@Tag("security")
@Tag("L3")
@SpringJUnitConfig({ NoteSecurityConfig.class, NoteService.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // delete() changes the notes
class MethodSecurityTest {

	@Autowired
	NoteService notes;

	@Test
	@WithMockUser(username = "alice")
	void ownersReadTheirNotes() {
		assertThat(notes.read(1).text()).isEqualTo("alice's shopping list");
	}

	@Test
	@WithMockUser(username = "alice")
	void othersCannotReadThem() {
		assertThatThrownBy(() -> notes.read(2)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(username = "root", roles = { "USER", "ADMIN" })
	void adminsReadEverything() {
		assertThat(notes.read(2).owner()).isEqualTo("bob");
	}

	@Test
	@WithMockUser(username = "alice")
	void onlyAdminsDelete() {
		assertThatThrownBy(() -> notes.delete(1)).isInstanceOf(AccessDeniedException.class);
		assertThat(notes.exists(1)).isTrue();
	}

	@Test
	@WithMockUser(username = "root", roles = { "USER", "ADMIN" })
	void adminsDelete() {
		notes.delete(2);
		assertThat(notes.exists(2)).isFalse();
	}

	@Test
	@WithMockUser(username = "bob")
	void mineListsOnlyMyNotes() {
		assertThat(notes.mine()).extracting("id").containsExactly(2L);
	}

	@Test
	void anonymousCallsAreRejectedBeforeTheMethodRuns() {
		assertThatThrownBy(() -> notes.mine()).isInstanceOf(AuthenticationException.class);
		assertThatThrownBy(() -> notes.delete(1)).isInstanceOf(AuthenticationException.class);
		assertThat(notes.exists(1)).isTrue();
	}

}
