package dojo.jpa.l2.lazy;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * KATA jpa L2 "CatalogService" (find the bug) (tracks/08-jpa-transactions/L2-practical.md, section 2).
 *
 * Works in every @DataJpaTest the team wrote. In the running application the category page fails
 * with LazyInitializationException: "failed to lazily initialize a collection ... no Session".
 * The test reproduces production (no test transaction around it). Fix it so that it works
 * outside any caller's transaction and returns the item names in id order.
 * Ask yourself why the team's tests never caught it.
 */
@Service
public class CatalogService {

	public record CategoryView(String name, List<String> itemNames) {
	}

	private final CategoryRepository categories;

	public CatalogService(CategoryRepository categories) {
		this.categories = categories;
	}

	public CategoryView category(long id) {
		Category category = categories.findById(id).orElseThrow();
		return new CategoryView(category.getName(), category.getItems().stream().map(CatalogItem::getName).toList());
	}

}
