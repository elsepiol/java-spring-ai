package dojo.jpa.l1.queries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Given: a mapped entity. */
@Entity
@Table(name = "jpa_product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String sku;
	private String name;
	private String category;
	@Column(name = "price_cents")
	private long priceCents;

	protected Product() {
	}

	public Product(String sku, String name, String category, long priceCents) {
		this.sku = sku;
		this.name = name;
		this.category = category;
		this.priceCents = priceCents;
	}

	public Long getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public long getPriceCents() {
		return priceCents;
	}

}
