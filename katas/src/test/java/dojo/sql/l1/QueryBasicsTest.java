package dojo.sql.l1;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.ShopDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Your queries live in katas/src/main/resources/sql/l1/. The data: katas/src/test/resources/sql/shop.sql. */
@Tag("sql")
@Tag("L1")
class QueryBasicsTest extends ShopDatabase {

	@Test
	void cheapInStock() throws Exception {
		assertThat(rows("l1/cheap_in_stock.sql")).containsExactly(row("PEN-1", 150), row("MUG-1", 1200));
	}

	@Test
	void ordersPerCustomerIncludesCustomersWithoutOrders() throws Exception {
		assertThat(rows("l1/orders_per_customer.sql")).containsExactly(
				row("Ann", 2), row("Bob", 2), row("Cat", 2), row("Dan", 1), row("Eve", 0));
	}

	@Test
	void revenueByCategory() throws Exception {
		assertThat(rows("l1/revenue_by_category.sql")).containsExactly(row("living", 23600), row("kitchen", 13100));
	}

	@Test
	void neverOrdered() throws Exception {
		assertThat(rows("l1/never_ordered.sql")).containsExactly(row("DSK-1"));
	}

	@Test
	void revenuePerCityIncludingCitiesWithoutSales() throws Exception {
		assertThat(rows("l1/best_customer_per_city.sql")).containsExactly(
				row("Berlin", 35250), row("Paris", 3700), row("Madrid", 0));
	}

	@Test
	void notInBerlinKeepsCustomersWithAnUnknownCity() throws Exception {
		assertThat(rows("l1/not_in_berlin.sql")).containsExactly(row("Bob"), row("Dan"), row("Eve"));
	}

}
