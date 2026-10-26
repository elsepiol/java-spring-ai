package dojo.testing.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.sql.support.Db;

@Tag("testing")
@Tag("L3")
class DatabaseCleanerTest {

	private Connection c;

	@BeforeEach
	void setUp() throws SQLException {
		c = DriverManager.getConnection(Db.freshDatabase());
		try (Statement s = c.createStatement()) {
			s.execute("""
					create table customer (id bigint generated always as identity primary key, name text not null);
					create table purchase (id bigint generated always as identity primary key,
					                       customer_id bigint not null references customer(id));
					create table "Weird Name" (x int);
					create view big_customers as select * from customer;
					create table flyway_schema_history (installed_rank int primary key, version text);
					insert into flyway_schema_history values (1, '1');
					insert into customer (name) values ('ada'), ('bob');
					insert into purchase (customer_id) values (1), (2), (2);
					insert into "Weird Name" values (1);
					create schema other;
					create table other.keep_me (x int);
					insert into other.keep_me values (1);
					""");
		}
	}

	@AfterEach
	void tearDown() throws SQLException {
		c.close();
	}

	@Test
	void listsOrdinaryTablesExceptFlywaysHistory() throws SQLException {
		assertThat(DatabaseCleaner.tablesToClean(c, "public")).containsExactlyInAnyOrder("Weird Name", "customer", "purchase");
	}

	@Test
	void emptiesEveryTableDespiteForeignKeys() throws SQLException {
		DatabaseCleaner.clean(c, "public");
		assertThat(count("customer")).isZero();
		assertThat(count("purchase")).isZero();
		assertThat(count("\"Weird Name\"")).isZero();
	}

	@Test
	void keepsFlywaysHistoryAndOtherSchemas() throws SQLException {
		DatabaseCleaner.clean(c, "public");
		assertThat(count("flyway_schema_history")).isEqualTo(1);
		assertThat(count("other.keep_me")).isEqualTo(1);
	}

	@Test
	void restartsIdentities() throws SQLException {
		DatabaseCleaner.clean(c, "public");
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("insert into customer (name) values ('cy') returning id")) {
			rs.next();
			assertThat(rs.getLong(1)).isEqualTo(1);
		}
	}

	@Test
	void anEmptySchemaIsFine() throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute("create schema empty");
		}
		DatabaseCleaner.clean(c, "empty");
		assertThat(DatabaseCleaner.tablesToClean(c, "empty")).isEmpty();
	}

	private long count(String table) throws SQLException {
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
			rs.next();
			return rs.getLong(1);
		}
	}

}
