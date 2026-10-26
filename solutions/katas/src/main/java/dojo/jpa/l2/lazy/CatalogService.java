package dojo.jpa.l2.lazy;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reference solution. Outside a transaction, findById's persistence context closes when the
 * repository call returns; the entity is detached and its lazy collection can no longer load.
 * Building the DTO inside a (read-only) transaction keeps the context open until the view is built.
 * (Alternatives: a fetch join / @EntityGraph query, or a DTO projection query. Never: switching the
 * collection to EAGER, or relying on open-session-in-view.)
 */
@Service
public class CatalogService {

	public record CategoryView(String name, List<String> itemNames) {
	}

	private final CategoryRepository categories;

	public CatalogService(CategoryRepository categories) {
		this.categories = categories;
	}

	@Transactional(readOnly = true)
	public CategoryView category(long id) {
		Category category = categories.findById(id).orElseThrow();
		return new CategoryView(category.getName(), category.getItems().stream().map(CatalogItem::getName).toList());
	}

}
