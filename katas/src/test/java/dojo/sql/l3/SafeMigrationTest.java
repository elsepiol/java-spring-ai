package dojo.sql.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.sql.support.Db;
import dojo.sql.support.ShopDatabase;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sql")
@Tag("L3")
class SafeMigrationTest extends ShopDatabase {

	@BeforeAll
	void migrate() throws Exception {
		execute(Db.read("sql/l3/add_customer_status.sql"));
	}

	@Test
	void existingCustomersAreActive() throws Exception {
		assertThat(query("SELECT DISTINCT status FROM customers")).containsExactly(row("ACTIVE"));
		assertThat(query("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'customers' AND column_name = 'status'"))
				.containsExactly(row("NO"));
	}

	@Test
	void theOldVersionsInsertStillWorks() throws Exception {
		execute("INSERT INTO customers (id, name, email, city, created_at) VALUES (100, 'Old App', 'old@example.com', 'Rome', '2026-04-01')");
		assertThat(query("SELECT status FROM customers WHERE id = 100")).containsExactly(row("ACTIVE"));
	}

	@Test
	void onlyKnownStatusesAreAllowedByAValidatedConstraint() throws Exception {
		assertThatThrownBy(() -> execute("UPDATE customers SET status = 'DELETED' WHERE id = 1")).isInstanceOf(SQLException.class);
		assertThat(query("SELECT convalidated FROM pg_constraint WHERE conname = 'customers_status_check'")).containsExactly(row("t"));
	}

	@Test
	void blockedCustomersHaveAPartialIndex() throws Exception {
		assertThat(query("SELECT indexdef FROM pg_indexes WHERE indexname = 'customers_blocked_idx'").get(0).get(0))
				.contains("WHERE (status = 'BLOCKED'");
	}

}
