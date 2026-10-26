package dojo.sql.l1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reference solution. User input NEVER becomes part of the SQL text: a PreparedStatement sends
 * the SQL and the values separately, so the value can never be parsed as SQL. LIKE wildcards in
 * the input are escaped so they match literally.
 */
public final class ProductSearch {

	private ProductSearch() {
	}

	public static List<String> skusMatching(Connection db, String text) throws SQLException {
		String literal = text.toLowerCase()
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		List<String> skus = new ArrayList<>();
		try (PreparedStatement ps = db.prepareStatement(
				"SELECT sku FROM products WHERE lower(name) LIKE ? ESCAPE '\\' ORDER BY sku")) {
			ps.setString(1, "%" + literal + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					skus.add(rs.getString("sku"));
				}
			}
		}
		return skus;
	}

}
