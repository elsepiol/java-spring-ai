package dojo.sql.l1;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * KATA sql L1 "ProductSearch" (find the bug) (tracks/04-sql/L1-basics.md, section 8: JDBC and SQL injection).
 *
 * The search box of the shop: products whose name contains the text, case-insensitively, as SKUs
 * in SKU order. It works for every normal search, and it is the most dangerous code in this
 * repository. The tests show why. Fix it without losing any feature; the search text must be
 * matched literally (a user typing % or _ means those characters, not wildcards).
 */
public final class ProductSearch {

	private ProductSearch() {
	}

	public static List<String> skusMatching(Connection db, String text) throws SQLException {
		String sql = "SELECT sku FROM products WHERE lower(name) LIKE '%" + text.toLowerCase() + "%' ORDER BY sku";
		List<String> skus = new ArrayList<>();
		try (Statement s = db.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			while (rs.next()) {
				skus.add(rs.getString("sku"));
			}
		}
		return skus;
	}

}
