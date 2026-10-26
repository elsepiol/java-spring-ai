package dojo.jpa.l2.nplus1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Given. */
@Entity
@Table(name = "jpa_order_item")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private OrderHeader order;

	private String sku;
	private int quantity;
	@Column(name = "unit_price_cents")
	private long unitPriceCents;

	protected OrderItem() {
	}

	OrderItem(OrderHeader order, String sku, int quantity, long unitPriceCents) {
		this.order = order;
		this.sku = sku;
		this.quantity = quantity;
		this.unitPriceCents = unitPriceCents;
	}

	public long totalCents() {
		return quantity * unitPriceCents;
	}

}
