package dojo.sql.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gives every test class its own fresh PostgreSQL database.
 *
 * By default a real PostgreSQL 17 is started INSIDE the test JVM (embedded-postgres: it unpacks
 * the PostgreSQL binaries into a temp folder and runs them; no Docker, no installation). It starts
 * once per test run and stops when the JVM exits.
 *
 * To use your own server instead (e.g. `docker run -p 5432:5432 -e POSTGRES_PASSWORD=pg postgres:17`),
 * set DOJO_DB_URL to a JDBC URL of a user that may create databases:
 *   DOJO_DB_URL='jdbc:postgresql://localhost:5432/postgres?user=postgres&password=pg' ./mvnw test -Dgroups=sql
 * (Embedded PostgreSQL refuses to run as root; inside a root-only container, use DOJO_DB_URL.)
 */
public final class Db {

	private static final AtomicInteger COUNTER = new AtomicInteger();
	private static String adminUrl;

	private Db() {
	}

	private static synchronized String adminUrl() {
		if (adminUrl == null) {
			String external = System.getenv("DOJO_DB_URL");
			if (external != null && !external.isBlank()) {
				adminUrl = external;
			} else {
				try {
					EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						try {
							pg.close();
						} catch (IOException ignored) {
						}
					}));
					adminUrl = pg.getJdbcUrl("postgres", "postgres");
				} catch (IOException e) {
					throw new UncheckedIOException("Could not start embedded PostgreSQL. Set DOJO_DB_URL to use your own server.", e);
				}
			}
		}
		return adminUrl;
	}

	/** Creates a new, empty database and returns a JDBC URL for it. */
	public static String freshDatabase() {
		String name = "kata_" + ProcessHandle.current().pid() + "_" + COUNTER.incrementAndGet();
		try (Connection c = DriverManager.getConnection(adminUrl()); Statement s = c.createStatement()) {
			s.execute("CREATE DATABASE " + name);
		} catch (SQLException e) {
			throw new IllegalStateException("cannot create database " + name, e);
		}
		return withDatabase(adminUrl(), name);
	}

	static String withDatabase(String url, String name) {
		int q = url.indexOf('?');
		String base = q < 0 ? url : url.substring(0, q);
		String params = q < 0 ? "" : url.substring(q);
		return base.substring(0, base.lastIndexOf('/') + 1) + name + params;
	}

	/** Runs a script from src/test/resources or src/main/resources (classpath), statement by statement. */
	public static void runScript(Connection c, String classpathResource) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute(read(classpathResource));
		}
	}

	public static String read(String classpathResource) {
		try (InputStream in = Db.class.getClassLoader().getResourceAsStream(classpathResource)) {
			if (in == null) {
				throw new IllegalArgumentException("not on the classpath: " + classpathResource);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
