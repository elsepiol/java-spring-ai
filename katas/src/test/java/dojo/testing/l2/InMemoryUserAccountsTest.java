package dojo.testing.l2;

import org.junit.jupiter.api.Tag;

import dojo.testing.l1.UserAccounts;

/** Runs the whole contract against the fake. */
@Tag("testing")
@Tag("L2")
class InMemoryUserAccountsTest extends UserAccountsContract {

	@Override
	protected UserAccounts newAccounts() {
		return new InMemoryUserAccounts();
	}

}
