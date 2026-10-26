package dojo.jpa.l3.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * KATA jpa L3 "OptimisticLocking" part 1 (tracks/08-jpa-transactions/L3-production.md, section 3).
 *
 * Two admins edit the same product: both load version 0, both save. The second save must FAIL
 * instead of silently overwriting the first. The table has a version column; make JPA use it.
 */
@Entity
@Table(name = "jpa_product")
public class EditableProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String sku;
	private String name;
	private String category;
	@Column(name = "price_cents")
	private long priceCents;
	private int version;

	protected EditableProduct() {
	}

	public EditableProduct(String sku, String name, String category, long priceCents) {
		this.sku = sku;
		this.name = name;
		this.category = category;
		this.priceCents = priceCents;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void rename(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

}
