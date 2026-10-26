package dojo.jpa.l1.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Reference solution. */
@Entity
@Table(name = "jpa_customer")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // the database's identity column assigns the id on INSERT
	private Long id;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	@Enumerated(EnumType.STRING) // ORDINAL (the default) stores 0/1: reordering the enum silently corrupts data
	@Column(nullable = false)
	private Tier tier;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Customer() {
	}

	public Customer(String fullName, String email, Tier tier) {
		this.fullName = fullName;
		this.email = email;
		this.tier = tier;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public Tier getTier() {
		return tier;
	}

	public void upgrade() {
		this.tier = Tier.GOLD;
	}

}
