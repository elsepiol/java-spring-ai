package dojo.sql.l3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Reference solution: compare-and-set on the version column. */
public final class OptimisticPrices {

	public record Snapshot(long priceCents, int version) {
	}

	private OptimisticPrices() {
	}

	public static Optional<Snapshot> load(Connection db, long productId) throws SQLException {
		try (PreparedStatement ps = db.prepareStatement("SELECT price_cents, version FROM products WHERE id = ?")) {
			ps.setLong(1, productId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? Optional.of(new Snapshot(rs.getLong(1), rs.getInt(2))) : Optional.empty();
			}
		}
	}

	public static boolean changePrice(Connection db, long productId, int expectedVersion, long newPriceCents) throws SQLException {
		try (PreparedStatement ps = db.prepareStatement(
				"UPDATE products SET price_cents = ?, version = version + 1 WHERE id = ? AND version = ?")) {
			ps.setLong(1, newPriceCents);
			ps.setLong(2, productId);
			ps.setInt(3, expectedVersion);
			return ps.executeUpdate() == 1; // 0 rows: the version moved on, someone else saved first
		}
	}

}
