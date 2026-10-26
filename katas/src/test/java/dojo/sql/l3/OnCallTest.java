package dojo.sql.l3;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.ShopDatabase;
import java.sql.Connection;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L3")
class OnCallTest extends ShopDatabase {

	@BeforeEach
	void twoDoctorsOnCall() throws Exception {
		execute("""
				DROP TABLE IF EXISTS doctors;
				CREATE TABLE doctors (id bigint PRIMARY KEY, name text NOT NULL, shift_id bigint NOT NULL, on_call boolean NOT NULL);
				INSERT INTO doctors VALUES (1, 'Alice', 10, true), (2, 'Bob', 10, true), (3, 'Carol', 20, true);
				""");
	}

	@AfterEach
	void resetHook() {
		OnCall.afterCheck = () -> {
		};
	}

	private int onCallInShift(long shift) throws Exception {
		return Integer.parseInt(query("SELECT count(*) FROM doctors WHERE shift_id = ? AND on_call", shift).get(0).get(0));
	}

	@Test
	void oneDoctorAtATimeWorks() throws Exception {
		assertThat(OnCall.goOffCall(db, 1)).isTrue();
		assertThat(OnCall.goOffCall(db, 2)).as("the last one must stay").isFalse();
		assertThat(OnCall.goOffCall(db, 3)).as("alone in shift 20").isFalse();
		assertThat(onCallInShift(10)).isEqualTo(1);
	}

	/** Both transactions do their check before either updates: the interleaving that breaks the rule. */
	@Test
	void twoDoctorsAtTheSameMomentLeaveOneOnCall() throws Exception {
		CyclicBarrier bothChecked = new CyclicBarrier(2);
		OnCall.afterCheck = () -> {
			try {
				bothChecked.await(500, TimeUnit.MILLISECONDS);
			} catch (Exception timedOut) {
				// the other transaction could not check at the same time: fine
			}
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try (Connection a = connect(); Connection b = connect()) {
			Future<Boolean> alice = pool.submit(() -> OnCall.goOffCall(a, 1));
			Future<Boolean> bob = pool.submit(() -> OnCall.goOffCall(b, 2));
			boolean aliceOff = alice.get(10, TimeUnit.SECONDS);
			boolean bobOff = bob.get(10, TimeUnit.SECONDS);

			assertThat(aliceOff ^ bobOff).as("exactly one may go off call (alice=%s, bob=%s)", aliceOff, bobOff).isTrue();
			assertThat(onCallInShift(10)).isEqualTo(1);
		} finally {
			pool.shutdownNow();
		}
	}

}
