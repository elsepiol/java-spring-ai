package dojo.boot.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Tag("boot")
@Tag("L3")
class GreeterAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GreeterAutoConfiguration.class));

	@Configuration
	static class MyOwnGreeter {
		@Bean
		Greeter custom() {
			return name -> "Yo " + name;
		}
	}

	@Test
	void providesAGreeterByDefault() {
		runner.run(context -> assertThat(context.getBean(Greeter.class).greet("Ann")).isEqualTo("Hello, Ann!"));
	}

	@Test
	void thePrefixIsConfigurable() {
		runner.withPropertyValues("dojo.greeter.prefix=Welcome").run(context -> assertThat(context.getBean(Greeter.class).greet("Bob")).isEqualTo("Welcome, Bob!"));
	}

	@Test
	void backsOffWhenTheApplicationHasItsOwn() {
		runner.withUserConfiguration(MyOwnGreeter.class).run(context -> {
			assertThat(context).hasSingleBean(Greeter.class);
			assertThat(context.getBean(Greeter.class).greet("Cat")).isEqualTo("Yo Cat");
		});
	}

	@Test
	void canBeSwitchedOff() {
		runner.withPropertyValues("dojo.greeter.enabled=false").run(context -> assertThat(context).doesNotHaveBean(Greeter.class));
	}

	@Test
	void isRegisteredForSpringBoot() throws Exception {
		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
			assertThat(in).as("the imports file exists").isNotNull();
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).contains("dojo.boot.l3.GreeterAutoConfiguration");
		}
	}

}
