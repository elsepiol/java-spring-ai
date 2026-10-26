package dojo.jpa.l3.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Reference solution: @Version on the version field. */
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
	@Version // UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?; 0 rows -> OptimisticLockException
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
