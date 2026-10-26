package dojo.jpa.l1.queries;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KATA jpa L1 "Queries" (tracks/08-jpa-transactions/L1-basics.md, section 5).
 *
 * findBySku works already: Spring Data derives the query from the method NAME. The other methods
 * have names Spring Data cannot derive, so startup fails until you give each one a query:
 * - cheapIn(category, maxCents): products of that category cheaper than maxCents, cheapest first (JPQL)
 * - totalsByCategory(): per category: the number of products and the average price, by category
 *   name; an interface projection (CategoryTotal) filled by JPQL aliases
 * - raisePrices(category, percent): ONE UPDATE statement raising the prices of a category by
 *   percent (rounded down), returning the number of rows changed (bulk update: @Modifying)
 * - skusMatching(pattern): a NATIVE PostgreSQL query using ILIKE (case-insensitive), sorted by sku
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	interface CategoryTotal {
		String getCategory();

		long getProducts();

		double getAverageCents();
	}

	Optional<Product> findBySku(String sku);

	List<Product> cheapIn(String category, long maxCents);

	List<CategoryTotal> totalsByCategory();

	int raisePrices(String category, int percent);

	List<String> skusMatching(String pattern);

}
