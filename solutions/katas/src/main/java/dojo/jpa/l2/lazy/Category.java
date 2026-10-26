package dojo.jpa.l2.lazy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Given. */
@Entity
@Table(name = "jpa_category")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;

	@OneToMany(mappedBy = "category", cascade = jakarta.persistence.CascadeType.ALL)
	private List<CatalogItem> items = new ArrayList<>();

	protected Category() {
	}

	public Category(String name) {
		this.name = name;
	}

	public Category add(String itemName) {
		items.add(new CatalogItem(this, itemName));
		return this;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<CatalogItem> getItems() {
		return items;
	}

}
