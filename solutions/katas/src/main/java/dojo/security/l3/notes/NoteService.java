package dojo.security.l3.notes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Reference solution for the MethodSecurity kata. */
@Service
public class NoteService {

	private final Map<Long, Note> notes = new ConcurrentHashMap<>(Map.of(
			1L, new Note(1, "alice", "alice's shopping list"),
			2L, new Note(2, "bob", "bob's diary")));

	@PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
	public Note read(long id) {
		Note note = notes.get(id);
		if (note == null) {
			throw new NoSuchElementException("no note " + id);
		}
		return note;
	}

	@PreAuthorize("hasRole('ADMIN')")
	public void delete(long id) {
		notes.remove(id);
	}

	@PreAuthorize("isAuthenticated()")
	public List<Note> mine() {
		String me = SecurityContextHolder.getContext().getAuthentication().getName();
		return notes.values().stream().filter(n -> n.owner().equals(me)).toList();
	}

	public boolean exists(long id) {
		return notes.containsKey(id);
	}

}
