package dojo.sql.l2;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.ShopDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L2")
class WindowFunctionsTest extends ShopDatabase {

	@Test
	void topTwoProductsPerCategory() throws Exception {
		assertThat(rows("l2/top_products_per_category.sql")).containsExactly(
				row("kitchen", "MUG-1", 3, 1), row("kitchen", "PAN-1", 2, 2),
				row("living", "LMP-1", 3, 1), row("living", "RUG-1", 1, 2),
				row("office", "PEN-1", 15, 1));
	}

	@Test
	void runningTotalPerCustomer() throws Exception {
		assertThat(rows("l2/running_total.sql")).containsExactly(
				row(1, 1, 3900, 3900), row(1, 2, 8400, 12300),
				row(2, 3, 3700, 3700),
				row(3, 5, 10550, 10550), row(3, 7, 12400, 22950));
	}

	@Test
	void monthOverMonthGrowth() throws Exception {
		assertThat(rows("l2/monthly_growth.sql")).containsExactly(
				row("2026-01", 7600, null, null),
				row("2026-02", 18950, 7600, 11350),
				row("2026-03", 12400, 18950, -6550));
	}

	@Test
	void removeDuplicateSignupsKeepingTheFirst() throws Exception {
		execute("""
				CREATE TABLE newsletter (id bigint PRIMARY KEY, email text NOT NULL);
				INSERT INTO newsletter VALUES (1, 'ann@x.com'), (2, 'bob@x.com'), (3, 'Ann@X.com'),
				                              (4, 'cat@x.com'), (5, 'BOB@x.com'), (6, 'ann@x.com');
				""");
		update(db, "l2/remove_duplicate_signups.sql");
		assertThat(query("SELECT id FROM newsletter ORDER BY id")).containsExactly(row(1), row(2), row(4));
	}

}
