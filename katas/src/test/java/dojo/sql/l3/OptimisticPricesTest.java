package dojo.sql.l3;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.l3.OptimisticPrices.Snapshot;
import dojo.sql.support.ShopDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L3")
class OptimisticPricesTest extends ShopDatabase {

	@Test
	void loadsPriceAndVersion() throws Exception {
		assertThat(OptimisticPrices.load(db, 2)).contains(new Snapshot(3500, 0));
		assertThat(OptimisticPrices.load(db, 999)).isEmpty();
	}

	@Test
	void theSecondOfTwoEditorsWithTheSameVersionIsRejected() throws Exception {
		Snapshot formOfA = OptimisticPrices.load(db, 4).orElseThrow();
		Snapshot formOfB = OptimisticPrices.load(db, 4).orElseThrow();

		assertThat(OptimisticPrices.changePrice(db, 4, formOfA.version(), 4500)).isTrue();
		assertThat(OptimisticPrices.changePrice(db, 4, formOfB.version(), 5500)).as("B saw an old version").isFalse();

		assertThat(OptimisticPrices.load(db, 4)).contains(new Snapshot(4500, formOfA.version() + 1));
	}

	@Test
	void afterReloadingTheEditCanBeSaved() throws Exception {
		Snapshot first = OptimisticPrices.load(db, 5).orElseThrow();
		assertThat(OptimisticPrices.changePrice(db, 5, first.version(), 9900)).isTrue();
		Snapshot reloaded = OptimisticPrices.load(db, 5).orElseThrow();
		assertThat(OptimisticPrices.changePrice(db, 5, reloaded.version(), 9500)).isTrue();
		assertThat(OptimisticPrices.load(db, 5)).contains(new Snapshot(9500, first.version() + 2));
	}

}
