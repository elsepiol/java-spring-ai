package dojo.jpa.l1.mapping;

/**
 * KATA jpa L1 "Mapping", part 2. Map to jpa_purchase_order: many orders belong to one customer
 * (column customer_id). Loading an order must NOT load its customer until it is used (lazy).
 */
public class PurchaseOrder {

	private Long id;
	private Customer customer;
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
