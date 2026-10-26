package dojo.jpa.l3.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * KATA jpa L3 "TransferService" (find the bug) (tracks/08-jpa-transactions/L3-production.md, section 2: propagation).
 *
 * Compliance rule: EVERY transfer attempt is audited, also the ones that fail. A failed transfer
 * must change no balance. The developer used REQUIRES_NEW so the audit row survives the transfer's
 * rollback; yet after a failed transfer the audit table is empty. Fix it. You may add classes in
 * this package. Keep transfer() one transaction for the money movement.
 */
@Service
public class TransferService {

	private final AccountRepository accounts;
	private final AuditRepository audit;

	public TransferService(AccountRepository accounts, AuditRepository audit) {
		this.accounts = accounts;
		this.audit = audit;
	}

	@Transactional
	public void transfer(long fromId, long toId, long cents) {
		recordAttempt("transfer " + fromId + "->" + toId + " " + cents);
		Account from = accounts.findById(fromId).orElseThrow();
		Account to = accounts.findById(toId).orElseThrow();
		from.withdraw(cents); // may throw: everything in this transaction rolls back
		to.deposit(cents);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAttempt(String action) {
		audit.save(new AuditEntry(action));
	}

}
