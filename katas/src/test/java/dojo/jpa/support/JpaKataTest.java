package dojo.jpa.support;

import dojo.sql.support.Db;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class of the JPA katas: a @DataJpaTest (JPA, Spring Data, Flyway, transactions; no web)
 * against a fresh PostgreSQL database (the embedded one from track 04, or DOJO_DB_URL), with
 * Flyway applying src/main/resources/db/migration and Hibernate VALIDATING your mappings against it.
 *
 * Note: @DataJpaTest runs every test method in a transaction that is rolled back at the end.
 * Some katas switch that off, because it hides real bugs (LazyInitializationException, propagation).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // a real PostgreSQL, not an in-memory H2
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=validate",                        // the schema comes from Flyway; Hibernate only checks it
		"spring.jpa.properties.hibernate.generate_statistics=true",      // count the SQL statements
		"logging.level.org.hibernate.SQL=debug" })                       // see every statement in the test output
public abstract class JpaKataTest {

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		String url = Db.freshDatabase();
		registry.add("spring.datasource.url", () -> url);
	}

	@Autowired
	protected EntityManagerFactory entityManagerFactory;

	protected Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

}
