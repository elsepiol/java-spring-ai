package dojo.jpa.l1.queries;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reference solution. JPQL names ENTITIES and FIELDS (Product, priceCents), not tables and columns. */
public interface ProductRepository extends JpaRepository<Product, Long> {

	interface CategoryTotal {
		String getCategory();

		long getProducts();

		double getAverageCents();
	}

	Optional<Product> findBySku(String sku);

	@Query("select p from Product p where p.category = :category and p.priceCents < :maxCents order by p.priceCents")
	List<Product> cheapIn(@Param("category") String category, @Param("maxCents") long maxCents);

	@Query("select p.category as category, count(p) as products, avg(p.priceCents) as averageCents "
			+ "from Product p group by p.category order by p.category")
	List<CategoryTotal> totalsByCategory();

	// clearAutomatically: entities already loaded in the persistence context would otherwise keep the old price
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Product p set p.priceCents = p.priceCents * (100 + :percent) / 100 where p.category = :category")
	int raisePrices(@Param("category") String category, @Param("percent") int percent);

	@Query(value = "select sku from jpa_product where name ilike :pattern order by sku", nativeQuery = true)
	List<String> skusMatching(@Param("pattern") String pattern);

}
