package dojo.testing.l1;

import java.util.Optional;

/** A collaborator (in production: a JPA repository). The tests replace it with a Mockito mock. */
public interface UserAccounts {

	boolean emailTaken(String email);

	/** Saves the account and returns its new id. */
	long create(String email, String passwordHash);

	Optional<String> emailOf(long id);

}
