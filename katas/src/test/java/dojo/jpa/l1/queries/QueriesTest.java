package dojo.jpa.l1.queries;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.jpa.support.JpaKataTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("jpa")
@Tag("L1")
class QueriesTest extends JpaKataTest {

	@Autowired
	ProductRepository products;

	@BeforeEach
	void catalog() {
		products.saveAll(List.of(
				new Product("MUG-1", "Blue Mug", "kitchen", 1200),
				new Product("PAN-1", "Frying Pan", "kitchen", 3500),
				new Product("KNF-1", "Chef Knife", "kitchen", 2500),
				new Product("LMP-1", "Desk Lamp", "living", 4900),
				new Product("RUG-1", "Big Rug", "living", 8900)));
	}

	@Test
	void derivedQuery() {
		assertThat(products.findBySku("LMP-1")).map(Product::getName).contains("Desk Lamp");
	}

	@Test
	void cheapInACategory() {
		assertThat(products.cheapIn("kitchen", 3000)).extracting(Product::getSku).containsExactly("MUG-1", "KNF-1");
	}

	@Test
	void totalsByCategory() {
		List<ProductRepository.CategoryTotal> totals = products.totalsByCategory();
		assertThat(totals).extracting(ProductRepository.CategoryTotal::getCategory).containsExactly("kitchen", "living");
		assertThat(totals.get(0).getProducts()).isEqualTo(3);
		assertThat(totals.get(1).getAverageCents()).isEqualTo(6900.0);
	}

	@Test
	void bulkPriceRaise() {
		products.findBySku("MUG-1").orElseThrow(); // loaded into the persistence context BEFORE the update
		assertThat(products.raisePrices("kitchen", 10)).isEqualTo(3);
		assertThat(products.findBySku("MUG-1").orElseThrow().getPriceCents()).as("not the stale cached entity").isEqualTo(1320);
		assertThat(products.findBySku("LMP-1").orElseThrow().getPriceCents()).isEqualTo(4900);
	}

	@Test
	void nativeCaseInsensitiveSearch() {
		assertThat(products.skusMatching("%MUG%")).containsExactly("MUG-1");
		assertThat(products.skusMatching("%e%")).containsExactly("KNF-1", "LMP-1", "MUG-1");
	}

}
