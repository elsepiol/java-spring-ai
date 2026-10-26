package dojo.jpa.l3.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reference solution. The bug: this.recordAttempt() was a self-invocation, so @Transactional(REQUIRES_NEW)
 * was ignored and the audit row joined the transfer's transaction, which rolled back.
 */
@Service
public class TransferService {

	private final AccountRepository accounts;
	private final TransferAudit audit;

	public TransferService(AccountRepository accounts, TransferAudit audit) {
		this.accounts = accounts;
		this.audit = audit;
	}

	@Transactional
	public void transfer(long fromId, long toId, long cents) {
		audit.recordAttempt("transfer " + fromId + "->" + toId + " " + cents); // through the proxy: its own transaction
		Account from = accounts.findById(fromId).orElseThrow();
		Account to = accounts.findById(toId).orElseThrow();
		from.withdraw(cents);
		to.deposit(cents);
	}

}
