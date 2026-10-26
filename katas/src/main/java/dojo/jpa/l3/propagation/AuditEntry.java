package dojo.jpa.l3.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Given. created_at is filled by the database default. */
@Entity
@Table(name = "jpa_audit")
public class AuditEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String action;

	protected AuditEntry() {
	}

	public AuditEntry(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}

}
