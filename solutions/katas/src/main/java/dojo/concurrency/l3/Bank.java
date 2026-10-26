package dojo.concurrency.l3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reference solution. A deadlock needs a CYCLE in the "waits for" graph: thread 1 holds A and
 * waits for B, thread 2 holds B and waits for A. Acquire locks in one GLOBAL order (here: by
 * account id) and no cycle can form, because everyone who needs both A and B asks for the
 * smaller id first. (Alternatives: tryLock with timeout and retry; or a single atomic UPDATE in
 * the database, which is what the shop does.)
 */
public class Bank {

	public static final class Account {
		final long id;
		long balanceCents;

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
		Account first = from.id < to.id ? from : to;
		Account second = first == from ? to : from;
		synchronized (first) {
			afterFirstLock.run();
			synchronized (second) {
				if (from.balanceCents < cents) {
					throw new IllegalStateException("insufficient funds in " + fromId);
				}
				from.balanceCents -= cents;
				to.balanceCents += cents;
			}
		}
	}

}
