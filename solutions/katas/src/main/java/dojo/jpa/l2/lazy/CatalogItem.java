package dojo.jpa.l2.lazy;

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
@Table(name = "jpa_catalog_item")
public class CatalogItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	private String name;

	protected CatalogItem() {
	}

	CatalogItem(Category category, String name) {
		this.category = category;
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
