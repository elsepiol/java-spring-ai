package dojo.jpa.l2.nplus1;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Given. One order, many items; the collection is LAZY (the default for @OneToMany). */
@Entity
@Table(name = "jpa_order_header")
public class OrderHeader {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String customer;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected OrderHeader() {
	}

	public OrderHeader(String customer) {
		this.customer = customer;
	}

	public OrderHeader add(String sku, int quantity, long unitPriceCents) {
		items.add(new OrderItem(this, sku, quantity, unitPriceCents));
		return this;
	}

	public Long getId() {
		return id;
	}

	public String getCustomer() {
		return customer;
	}

	public List<OrderItem> getItems() {
		return items;
	}

}
