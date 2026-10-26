package dojo.spring.l2;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * Reference solution. (@Bean(initMethod = "open", destroyMethod = "close") works too, for classes you
 * cannot annotate; and Spring infers a public close() method as the destroy method of @Bean beans.)
 */
public class ConnectionPool {

	private final List<String> events;
	private final int size;

	public ConnectionPool(List<String> events, int size) {
		this.events = events;
		this.size = size;
	}

	@PostConstruct
	public void open() {
		events.add("open " + size);
	}

	@PreDestroy
	public void close() {
		events.add("close");
	}

}
