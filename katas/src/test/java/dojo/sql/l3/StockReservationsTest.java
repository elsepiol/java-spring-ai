package dojo.sql.l3;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.ShopDatabase;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L3")
class StockReservationsTest extends ShopDatabase {

	@Test
	void reservesWhileThereIsEnough() throws Exception {
		execute("UPDATE products SET stock = 5 WHERE id = 4");
		assertThat(StockReservations.reserve(db, 4, 3)).isTrue();
		assertThat(StockReservations.reserve(db, 4, 3)).isFalse();
		assertThat(StockReservations.reserve(db, 4, 2)).isTrue();
		assertThat(query("SELECT stock FROM products WHERE id = 4")).containsExactly(row(0));
	}

	@Test
	void twentyBuyersForTenUnitsNeverOversell() throws Exception {
		execute("UPDATE products SET stock = 10 WHERE id = 1");
		AtomicInteger sold = new AtomicInteger();
		List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch gate = new CountDownLatch(1);
		List<Thread> buyers = new ArrayList<>();
		for (int b = 0; b < 20; b++) {
			Thread t = new Thread(() -> {
				try (Connection c = connect()) {
					gate.await();
					if (StockReservations.reserve(c, 1, 1)) {
						sold.incrementAndGet();
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			});
			t.start();
			buyers.add(t);
		}
		gate.countDown();
		for (Thread t : buyers) {
			t.join();
		}
		assertThat(errors).isEmpty();
		assertThat(sold).hasValue(10);
		assertThat(query("SELECT stock FROM products WHERE id = 1")).containsExactly(row(0));
	}

}
