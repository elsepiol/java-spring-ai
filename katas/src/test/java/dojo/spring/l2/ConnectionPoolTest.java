package dojo.spring.l2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Tag("spring")
@Tag("L2")
class ConnectionPoolTest {

	static final List<String> EVENTS = new ArrayList<>();

	@Configuration
	static class Config {
		@Bean(destroyMethod = "") // switch off Spring's "a public close() is the destroy method" inference: only YOUR callbacks count
		ConnectionPool pool() {
			EVENTS.add("created");
			return new ConnectionPool(EVENTS, 10);
		}
	}

	@Test
	void opensAfterCreationAndClosesOnShutdown() {
		EVENTS.clear();
		var context = new AnnotationConfigApplicationContext(Config.class);
		assertThat(EVENTS).containsExactly("created", "open 10");

		context.close();
		assertThat(EVENTS).containsExactly("created", "open 10", "close");
	}

	@Test
	void prototypeBeansAreNotClosedByTheContainer() {
		EVENTS.clear();
		var context = new AnnotationConfigApplicationContext();
		context.registerBean("pool", ConnectionPool.class, () -> new ConnectionPool(EVENTS, 3), bd -> bd.setScope(BeanDefinition.SCOPE_PROTOTYPE));
		context.refresh();
		context.getBean("pool");
		context.close();
		// Spring initialises prototypes but does not manage their end of life: whoever asked for one must close it
		assertThat(EVENTS).containsExactly("open 3");
	}

}
