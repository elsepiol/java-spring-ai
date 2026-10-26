package dojo.jpa.l3.propagation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Part of the reference solution. In its own bean, the call goes through the transaction proxy, so
 * REQUIRES_NEW really suspends the caller's transaction and commits the audit row on its own.
 */
@Component
public class TransferAudit {

	private final AuditRepository audit;

	public TransferAudit(AuditRepository audit) {
		this.audit = audit;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAttempt(String action) {
		audit.save(new AuditEntry(action));
	}

}
