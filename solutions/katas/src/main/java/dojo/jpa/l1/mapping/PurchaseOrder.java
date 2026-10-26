package dojo.jpa.l1.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Reference solution. @ManyToOne is EAGER by default in JPA: always say LAZY. */
@Entity
@Table(name = "jpa_purchase_order")
public class PurchaseOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Column(name = "total_cents", nullable = false)
	private long totalCents;

	protected PurchaseOrder() {
	}

	public PurchaseOrder(Customer customer, long totalCents) {
		this.customer = customer;
		this.totalCents = totalCents;
	}

	public Long getId() {
		return id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public long getTotalCents() {
		return totalCents;
	}

}
