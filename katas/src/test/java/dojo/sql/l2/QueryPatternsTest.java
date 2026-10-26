package dojo.sql.l2;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.ShopDatabase;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L2")
class QueryPatternsTest extends ShopDatabase {

	@Test
	void keysetPaginationWalksThroughAllProducts() throws Exception {
		assertThat(rows("l2/keyset_page.sql", -1L, 0L)).containsExactly(row("PEN-1", 150), row("MUG-1", 1200), row("KNF-1", 2500));
		assertThat(rows("l2/keyset_page.sql", 2500L, 3L)).containsExactly(row("PAN-1", 3500), row("LMP-1", 4900), row("RUG-1", 8900));
		assertThat(rows("l2/keyset_page.sql", 8900L, 5L)).containsExactly(row("DSK-1", 19900));
		assertThat(rows("l2/keyset_page.sql", 19900L, 7L)).isEmpty();
	}

	@Test
	void keysetPaginationHandlesEqualPrices() throws Exception {
		execute("INSERT INTO products (id, sku, name, category, price_cents) VALUES (8, 'CUP-1', 'Cup', 'kitchen', 1200)");
		try {
			// MUG-1 (id 1) and CUP-1 (id 8) both cost 1200: the id decides the order, nothing is skipped or repeated
			assertThat(rows("l2/keyset_page.sql", 150L, 6L)).containsExactly(row("MUG-1", 1200), row("CUP-1", 1200), row("KNF-1", 2500));
			assertThat(rows("l2/keyset_page.sql", 1200L, 1L)).containsExactly(row("CUP-1", 1200), row("KNF-1", 2500), row("PAN-1", 3500));
		} finally {
			execute("DELETE FROM products WHERE id = 8");
		}
	}

	@Test
	void upsertCreatesThenAdds() throws Exception {
		update(db, "l2/add_stock.sql", "LMP-1", 5);
		update(db, "l2/add_stock.sql", "LMP-1", 3);
		update(db, "l2/add_stock.sql", "RUG-1", 2);
		assertThat(query("SELECT sku, quantity FROM stock_counters WHERE sku IN ('LMP-1', 'RUG-1') ORDER BY sku"))
				.containsExactly(row("LMP-1", 8), row("RUG-1", 2));
	}

	@Test
	void upsertIsSafeUnderConcurrency() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());
		for (int t = 0; t < 10; t++) {
			Thread thread = new Thread(() -> {
				try (Connection c = connect()) {
					gate.await();
					for (int i = 0; i < 50; i++) {
						update(c, "l2/add_stock.sql", "PEN-1", 1);
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			});
			thread.start();
			threads.add(thread);
		}
		gate.countDown();
		for (Thread thread : threads) {
			thread.join();
		}
		assertThat(errors).as("no statement may fail (e.g. a duplicate key)").isEmpty();
		assertThat(query("SELECT quantity FROM stock_counters WHERE sku = 'PEN-1'")).containsExactly(row(500));
	}

}
