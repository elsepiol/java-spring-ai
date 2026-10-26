package dojo.sql.l3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Reference solution: ONE conditional UPDATE. The check (stock >= ?) and the change happen in the
 * same statement, under the row lock PostgreSQL takes for the update; a second transaction waits
 * for the first, then re-evaluates the WHERE against the new row. The row count says if it worked.
 * (Alternatives: SELECT ... FOR UPDATE then UPDATE in one transaction; or optimistic locking.)
 */
public final class StockReservations {

	private StockReservations() {
	}

	public static boolean reserve(Connection db, long productId, int quantity) throws SQLException {
		try (PreparedStatement ps = db.prepareStatement(
				"UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
			ps.setInt(1, quantity);
			ps.setLong(2, productId);
			ps.setInt(3, quantity);
			return ps.executeUpdate() == 1;
		}
	}

}
