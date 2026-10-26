package dojo.testing.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dojo.testing.l1.UserAccounts;

/**
 * The contract every UserAccounts implementation must satisfy. Abstract: each implementation
 * gets a subclass that only says how to create one (the fake here; in a real project also the
 * JDBC implementation, run against a real database, so the fake cannot drift from it).
 */
abstract class UserAccountsContract {

	protected abstract UserAccounts newAccounts();

	private UserAccounts accounts;

	@BeforeEach
	void setUp() {
		accounts = newAccounts();
	}

	@Test
	void idsStartAtOneAndIncrease() {
		assertThat(accounts.create("a@example.com", "h")).isEqualTo(1);
		assertThat(accounts.create("b@example.com", "h")).isEqualTo(2);
	}

	@Test
	void findsWhatWasCreated() {
		long id = accounts.create("a@example.com", "h");
		assertThat(accounts.emailOf(id)).contains("a@example.com");
		assertThat(accounts.emailTaken("a@example.com")).isTrue();
	}

	@Test
	void unknownThingsAreAbsent() {
		assertThat(accounts.emailOf(99)).isEmpty();
		assertThat(accounts.emailTaken("nobody@example.com")).isFalse();
	}

	@Test
	void duplicateEmailsAreRejected() {
		accounts.create("a@example.com", "h");
		assertThatIllegalStateException().isThrownBy(() -> accounts.create("a@example.com", "other"));
		assertThat(accounts.create("b@example.com", "h")).as("a failed create uses no id").isEqualTo(2);
	}

	@Test
	void emailsAreComparedExactly() {
		accounts.create("a@example.com", "h");
		assertThat(accounts.emailTaken("A@example.com")).isFalse();
	}

	@Test
	void concurrentCreatesStayConsistent() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			List<Callable<Long>> tasks = new ArrayList<>();
			for (int i = 0; i < 2_000; i++) {
				String email = "user" + (i % 1_000) + "@example.com"; // every email twice
				tasks.add(() -> {
					try {
						return accounts.create(email, "h");
					}
					catch (IllegalStateException duplicate) {
						return -1L;
					}
				});
			}
			Set<Long> ids = new HashSet<>();
			int created = 0;
			for (Future<Long> f : pool.invokeAll(tasks)) {
				long id = f.get();
				if (id > 0) {
					created++;
					ids.add(id);
				}
			}
			assertThat(created).as("each email accepted exactly once").isEqualTo(1_000);
			assertThat(ids).as("distinct ids 1..1000").hasSize(1_000).allMatch(id -> id >= 1 && id <= 1_000);
		}
		finally {
			pool.shutdownNow();
		}
	}

}
