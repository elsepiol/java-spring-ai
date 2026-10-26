package dojo.security.l3.notes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * KATA security L3 "MethodSecurity" (tracks/09-security/L3-production.md, section 1).
 *
 * The URL rules of a filter chain cannot know who owns note 42. Protect the service methods
 * themselves with Spring Security's method security (and enable it in NoteSecurityConfig):
 * - read(id): only the note's owner or an ADMIN; checked on the RETURNED note;
 * - delete(id): ADMIN only;
 * - mine(): any authenticated user (it already filters by the current user);
 * - every method: nobody logged in -> an AuthenticationException, not a result.
 * Use annotations with SpEL (authentication.name, hasRole, returnObject), not if-statements, so the
 * rules are visible on the method and enforced by the proxy. Remember what proxies cannot see
 * (track 06): a self-call would skip all of it.
 */
@Service
public class NoteService {

	private final Map<Long, Note> notes = new ConcurrentHashMap<>(Map.of(
			1L, new Note(1, "alice", "alice's shopping list"),
			2L, new Note(2, "bob", "bob's diary")));

	public Note read(long id) {
		Note note = notes.get(id);
		if (note == null) {
			throw new NoSuchElementException("no note " + id);
		}
		return note;
	}

	public void delete(long id) {
		notes.remove(id);
	}

	public List<Note> mine() {
		String me = SecurityContextHolder.getContext().getAuthentication().getName();
		return notes.values().stream().filter(n -> n.owner().equals(me)).toList();
	}

	public boolean exists(long id) {
		return notes.containsKey(id);
	}

}
