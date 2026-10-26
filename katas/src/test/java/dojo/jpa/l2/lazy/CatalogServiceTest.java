package dojo.jpa.l2.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.jpa.support.JpaKataTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOT_SUPPORTED: no test-managed transaction. With @DataJpaTest's default (a transaction around every
 * test) the persistence context stays open for the whole test, and lazy loading "works": that is
 * exactly why such tests miss this production bug.
 */
@Tag("jpa")
@Tag("L2")
@Import(CatalogService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogServiceTest extends JpaKataTest {

	@Autowired
	CategoryRepository categories;

	@Autowired
	CatalogService catalog;

	@Test
	void buildsTheViewOutsideAnyCallerTransaction() {
		long id = categories.save(new Category("kitchen").add("Mug").add("Pan").add("Knife")).getId();

		CatalogService.CategoryView view = catalog.category(id);

		assertThat(view.name()).isEqualTo("kitchen");
		assertThat(view.itemNames()).containsExactly("Mug", "Pan", "Knife");
	}

}
