package dojo.testing.l2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dojo.testing.l1.UserAccounts;

/** Reference solution for the InMemoryUserAccounts kata: one lock keeps the email check and the insert atomic. */
public final class InMemoryUserAccounts implements UserAccounts {

	private final Map<Long, String> emailsById = new HashMap<>();
	private final Map<String, Long> idsByEmail = new HashMap<>();
	private long nextId = 1;

	@Override
	public synchronized boolean emailTaken(String email) {
		return idsByEmail.containsKey(email);
	}

	@Override
	public synchronized long create(String email, String passwordHash) {
		if (idsByEmail.containsKey(email)) {
			throw new IllegalStateException("duplicate email: " + email);
		}
		long id = nextId++;
		emailsById.put(id, email);
		idsByEmail.put(email, id);
		return id;
	}

	@Override
	public synchronized Optional<String> emailOf(long id) {
		return Optional.ofNullable(emailsById.get(id));
	}

}
