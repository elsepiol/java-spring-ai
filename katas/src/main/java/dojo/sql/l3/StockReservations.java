package dojo.sql.l3;

import dojo.Kata;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * KATA sql L3 "StockReservations" (tracks/04-sql/L3-production.md, section 2: lost updates).
 *
 * Reserve `quantity` units of a product (products.stock) for an order. 20 customers click "buy"
 * on the last 10 units at the same moment, each request on its own connection, on any of the
 * service's instances. Exactly 10 must succeed; stock must never go below 0 or be wrong.
 *
 * The obvious version (SELECT stock; if enough, UPDATE stock = newValue) oversells. Do it in a way
 * that is correct across connections and instances. Return true if reserved, false if there is
 * not enough stock. The connection is in auto-commit mode.
 */
public final class StockReservations {

	private StockReservations() {
	}

	public static boolean reserve(Connection db, long productId, int quantity) throws SQLException {
		return Kata.todo("tracks/04-sql/L3-production.md, section 2");
	}

}
