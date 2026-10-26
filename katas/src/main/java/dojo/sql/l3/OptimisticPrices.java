package dojo.sql.l3;

import dojo.Kata;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * KATA sql L3 "OptimisticPrices" (tracks/04-sql/L3-production.md, section 3: optimistic locking).
 *
 * Two admins open the edit form of the same product. Both see price 49.00, version 0. Admin A saves
 * 45.00; admin B, still looking at the old form, saves 55.00 and silently overwrites A's change
 * (a "lost update" across two HTTP requests, where no database lock can help: nobody holds a
 * transaction open while a human edits).
 *
 * products.version solves it. load() returns the current price and version; changePrice() updates
 * only if the version is still the one the caller loaded, and increments it. It returns false if
 * someone else changed the product in the meantime (the API then answers 409 Conflict and the
 * user reloads). The JPA track shows @Version doing exactly this.
 */
public final class OptimisticPrices {

	public record Snapshot(long priceCents, int version) {
	}

	private OptimisticPrices() {
	}

	public static Optional<Snapshot> load(Connection db, long productId) throws SQLException {
		return Kata.todo("tracks/04-sql/L3-production.md, section 3");
	}

	public static boolean changePrice(Connection db, long productId, int expectedVersion, long newPriceCents) throws SQLException {
		return Kata.todo("tracks/04-sql/L3-production.md, section 3");
	}

}
