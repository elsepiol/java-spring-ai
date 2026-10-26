package dojo.spring.l2;

import java.util.List;

/**
 * KATA spring L2 "ConnectionPool" (tracks/06-spring-core/L2-practical.md, section 2: lifecycle).
 *
 * A bean that owns resources: it must open them AFTER its dependencies are injected and its
 * configuration is set, and close them when the application shuts down (or connections leak on
 * every redeploy and test). Make Spring call:
 *   open()  once, after construction and injection: it records "open " + size
 *   close() once, when the context closes: it records "close"
 * The constructor must NOT open anything (record nothing there). Use the standard callbacks.
 * `events` is a shared list the test reads.
 */
public class ConnectionPool {

	private final List<String> events;
	private final int size;

	public ConnectionPool(List<String> events, int size) {
		this.events = events;
		this.size = size;
	}

	public void open() {
		events.add("open " + size);
	}

	public void close() {
		events.add("close");
	}

}
