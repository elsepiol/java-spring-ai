package dojo.sql.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * Base class for SQL katas: a fresh database per test class, loaded with tracks/04's shop data
 * (src/test/resources/sql/shop.sql), and helpers to run YOUR query files from
 * src/main/resources/sql/... and compare the rows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ShopDatabase {

	protected String url;
	protected Connection db;

	@BeforeAll
	void createDatabase() throws SQLException {
		url = Db.freshDatabase();
		db = connect();
		Db.runScript(db, "sql/shop.sql");
	}

	@AfterAll
	void closeDatabase() throws SQLException {
		if (db != null) {
			db.close();
		}
	}

	/** A new connection to this class's database (for tests with several concurrent transactions). */
	protected Connection connect() throws SQLException {
		return DriverManager.getConnection(url);
	}

	/** Runs the query in src/main/resources/sql/{file} with the given parameters; every value as text, NULL as "null". */
	protected List<List<String>> rows(String file, Object... params) throws SQLException {
		return query(Db.read("sql/" + file), params);
	}

	protected List<List<String>> query(String sql, Object... params) throws SQLException {
		try (PreparedStatement ps = db.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData meta = rs.getMetaData();
				List<List<String>> out = new ArrayList<>();
				while (rs.next()) {
					List<String> row = new ArrayList<>();
					for (int c = 1; c <= meta.getColumnCount(); c++) {
						row.add(String.valueOf(rs.getString(c)));
					}
					out.add(row);
				}
				return out;
			}
		}
	}

	/** Runs the statement in src/main/resources/sql/{file} (INSERT/UPDATE/DELETE/DDL); returns the row count. */
	protected int update(Connection c, String file, Object... params) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(Db.read("sql/" + file))) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			return ps.executeUpdate();
		}
	}

	protected void execute(String sql) throws SQLException {
		try (var s = db.createStatement()) {
			s.execute(sql);
		}
	}

	protected static List<String> row(Object... values) {
		List<String> r = new ArrayList<>();
		for (Object v : values) {
			r.add(String.valueOf(v));
		}
		return r;
	}

}
