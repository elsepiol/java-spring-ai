package dojo.testing.l2;

import java.util.Optional;

import dojo.Kata;
import dojo.testing.l1.UserAccounts;

/**
 * KATA testing L2 "InMemoryUserAccounts" (tracks/10-testing/L2-practical.md, section 2).
 *
 * A FAKE: a real, working, in-memory implementation of UserAccounts for fast tests, as opposed
 * to a mock that only replays answers. A fake is only useful if it behaves like the real
 * database-backed implementation, so it must pass the same contract (UserAccountsContract, in the
 * tests), which describes what every UserAccounts must do:
 * - ids start at 1 and increase by 1;
 * - creating an account for a taken email fails with IllegalStateException (the real one has a
 *   unique constraint), and emails are compared exactly (normalising is the service's job);
 * - emailOf(unknown id) is empty;
 * - safe for concurrent use: concurrent creates never hand out the same id or accept a duplicate
 *   email.
 */
public final class InMemoryUserAccounts implements UserAccounts {

	@Override
	public boolean emailTaken(String email) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 2");
	}

	@Override
	public long create(String email, String passwordHash) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 2");
	}

	@Override
	public Optional<String> emailOf(long id) {
		return Kata.todo("tracks/10-testing/L2-practical.md section 2");
	}

}
