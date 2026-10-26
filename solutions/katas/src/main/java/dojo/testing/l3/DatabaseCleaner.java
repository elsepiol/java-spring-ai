package dojo.testing.l3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Reference solution for the DatabaseCleaner kata. */
public final class DatabaseCleaner {

	private DatabaseCleaner() {
	}

	public static List<String> tablesToClean(Connection c, String schema) throws SQLException {
		String sql = """
				select table_name from information_schema.tables
				where table_schema = ? and table_type = 'BASE TABLE' and table_name <> 'flyway_schema_history'
				order by table_name""";
		List<String> tables = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, schema);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					tables.add(rs.getString(1));
				}
			}
		}
		return tables;
	}

	public static void clean(Connection c, String schema) throws SQLException {
		List<String> tables = tablesToClean(c, schema);
		if (tables.isEmpty()) {
			return;
		}
		String names = tables.stream().map(t -> quote(schema) + "." + quote(t)).collect(Collectors.joining(", "));
		try (Statement s = c.createStatement()) {
			s.execute("truncate table " + names + " restart identity cascade");
		}
	}

	private static String quote(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

}
