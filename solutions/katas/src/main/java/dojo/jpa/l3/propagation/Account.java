package dojo.jpa.l3.propagation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Given. */
@Entity
@Table(name = "jpa_account")
public class Account {

	@Id
	private Long id;
	private String owner;
	@Column(name = "balance_cents")
	private long balanceCents;

	protected Account() {
	}

	public Account(long id, String owner, long balanceCents) {
		this.id = id;
		this.owner = owner;
		this.balanceCents = balanceCents;
	}

	public void withdraw(long cents) {
		if (cents > balanceCents) {
			throw new IllegalStateException("insufficient funds in account " + id);
		}
		balanceCents -= cents;
	}

	public void deposit(long cents) {
		balanceCents += cents;
	}

	public long getBalanceCents() {
		return balanceCents;
	}

}
