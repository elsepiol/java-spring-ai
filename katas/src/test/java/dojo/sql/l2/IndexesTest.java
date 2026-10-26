package dojo.sql.l2;

import static org.assertj.core.api.Assertions.assertThat;

import dojo.sql.support.Db;
import dojo.sql.support.ShopDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Reads the query plans PostgreSQL chooses after your indexes (src/main/resources/sql/l2/indexes.sql). */
@Tag("sql")
@Tag("L2")
class IndexesTest extends ShopDatabase {

	@BeforeAll
	void createEventsAndYourIndexes() throws Exception {
		execute("""
				CREATE TABLE events (
				    id           bigint PRIMARY KEY,
				    customer_id  bigint      NOT NULL,
				    type         text        NOT NULL,
				    status       text        NOT NULL,
				    external_ref text        NOT NULL,
				    created_at   timestamptz NOT NULL
				);
				INSERT INTO events
				SELECT g, g % 5000, (ARRAY['VIEW','CART','ORDER','ERROR'])[g % 4 + 1],
				       CASE WHEN g % 1000 = 0 THEN 'PENDING' ELSE 'DONE' END,
				       'REF-' || g, timestamptz '2026-01-01 00:00Z' + g * interval '1 minute'
				FROM generate_series(1, 200000) g;
				""");
		execute(Db.read("sql/l2/indexes.sql"));
		execute("ANALYZE events");
	}

	private String plan(String sql) throws Exception {
		return String.join("\n", query("EXPLAIN (FORMAT JSON) " + sql).stream().map(r -> r.get(0)).toList());
	}

	@Test
	void latestEventsOfOneCustomerNeedNoScanAndNoSort() throws Exception {
		String plan = plan("SELECT id FROM events WHERE customer_id = 42 ORDER BY created_at DESC LIMIT 20");
		assertThat(plan).as(plan).doesNotContain("\"Seq Scan\"").doesNotContain("\"Sort\"");
	}

	@Test
	void caseInsensitiveLookupUsesAnIndex() throws Exception {
		String plan = plan("SELECT id FROM events WHERE lower(external_ref) = 'ref-123'");
		assertThat(plan).as(plan).doesNotContain("\"Seq Scan\"");
	}

	@Test
	void pendingEventsUseASmallIndex() throws Exception {
		String plan = plan("SELECT id FROM events WHERE status = 'PENDING' ORDER BY id LIMIT 10");
		assertThat(plan).as(plan).doesNotContain("\"Seq Scan\"").doesNotContain("events_pkey");
		String index = plan.replaceAll("(?s).*\"Index Name\": \"([^\"]+)\".*", "$1");
		List<List<String>> size = query("SELECT pg_relation_size(?::regclass)", index);
		assertThat(Long.parseLong(size.get(0).get(0))).as("size of %s in bytes", index).isLessThan(100_000);
	}

	@Test
	void atMostThreeExtraIndexes() throws Exception {
		// every index makes every INSERT and UPDATE slower and uses disk: add only what queries need
		assertThat(query("SELECT count(*) FROM pg_indexes WHERE tablename = 'events' AND indexname <> 'events_pkey'").get(0).get(0))
				.isIn("1", "2", "3");
	}

}
