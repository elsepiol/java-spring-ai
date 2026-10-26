package dojo.concurrency.l3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KATA concurrency L3 "Bank": a deadlock (tracks/02-concurrency/L3-production.md, section 3).
 *
 * transfer() locks both accounts so no other transfer sees a half-done move. It works in every
 * single-threaded test. In production, two customers paying each other at the same moment freeze
 * two request threads forever; then the pool runs out of threads and the service stops answering.
 * The tests force exactly that interleaving. Fix it so that it can NEVER deadlock, keeping both
 * locks (do not make the whole bank one big lock: transfers between unrelated accounts must still
 * run in parallel).
 *
 * `afterFirstLock` is a test hook, called while holding the first lock. Keep calling it.
 */
public class Bank {

	public static final class Account {
		final long id;
		long balanceCents; // guarded by the account's own monitor

		Account(long id, long balanceCents) {
			this.id = id;
			this.balanceCents = balanceCents;
		}
	}

	private final Map<Long, Account> accounts = new ConcurrentHashMap<>();
	volatile Runnable afterFirstLock = () -> {
	};

	public void open(long id, long balanceCents) {
		accounts.put(id, new Account(id, balanceCents));
	}

	public long balance(long id) {
		Account a = accounts.get(id);
		synchronized (a) {
			return a.balanceCents;
		}
	}

	public void transfer(long fromId, long toId, long cents) {
		if (fromId == toId) {
			throw new IllegalArgumentException("cannot transfer to the same account");
		}
		Account from = accounts.get(fromId);
		Account to = accounts.get(toId);
		synchronized (from) {
			afterFirstLock.run();
			synchronized (to) {
				if (from.balanceCents < cents) {
					throw new IllegalStateException("insufficient funds in " + fromId);
				}
				from.balanceCents -= cents;
				to.balanceCents += cents;
			}
		}
	}

}
