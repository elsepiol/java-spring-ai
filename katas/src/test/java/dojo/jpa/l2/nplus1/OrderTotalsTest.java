package dojo.jpa.l2.nplus1;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.jpa.support.JpaKataTest;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Tag("jpa")
@Tag("L2")
@Import(OrderTotals.class)
class OrderTotalsTest extends JpaKataTest {

	@Autowired
	OrderHeaderRepository orders;

	@Autowired
	OrderTotals totals;

	@Autowired
	EntityManager em;

	@BeforeEach
	void thirtyOrders() {
		for (int i = 1; i <= 30; i++) {
			OrderHeader order = new OrderHeader("customer-" + i).add("MUG-1", i, 1200).add("PEN-1", 2, 150);
			orders.save(order);
		}
		orders.save(new OrderHeader("empty")); // an order without items
		em.flush();
		em.clear(); // nothing cached: every read goes to the database
		statistics().clear();
	}

	@Test
	void computesTheTotals() {
		Map<Long, Long> result = totals.totalsPerOrder();
		assertThat(result).hasSize(31);
		assertThat(result.values()).startsWith(1500L, 2700L, 3900L).endsWith(36300L, 0L);
	}

	@Test
	void usesAtMostTwoStatements() {
		totals.totalsPerOrder();
		assertThat(statistics().getPrepareStatementCount()).as("SQL statements for 31 orders").isLessThanOrEqualTo(2);
	}

}
