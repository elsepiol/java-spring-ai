package dojo.testing.l3;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import dojo.Kata;

/**
 * KATA testing L3 "DatabaseCleaner" (tracks/10-testing/L3-production.md, section 2).
 *
 * Integration tests that commit (they test real transactions, so they cannot roll back) must
 * leave an empty database for the next test. Write the cleaner:
 * - tablesToClean: the names of the ordinary tables in {@code schema} (information_schema.tables,
 *   table_type 'BASE TABLE'), in any order, EXCLUDING Flyway's history table "flyway_schema_history"
 *   (wiping it would make Flyway think no migration ever ran);
 * - clean: empty all of them with ONE statement that also resets identity columns (ids start
 *   at 1 again, so tests can rely on them) and copes with foreign keys between the tables.
 *   Quote the names (schema-qualified) so odd names cannot break the statement. Do nothing when
 *   there is no table.
 */
public final class DatabaseCleaner {

	private DatabaseCleaner() {
	}

	public static List<String> tablesToClean(Connection c, String schema) throws SQLException {
		return Kata.todo("tracks/10-testing/L3-production.md section 2");
	}

	public static void clean(Connection c, String schema) throws SQLException {
		Kata.todo("tracks/10-testing/L3-production.md section 2");
	}

}
