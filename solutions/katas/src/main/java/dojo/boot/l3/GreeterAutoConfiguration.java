package dojo.boot.l3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Reference solution: the whole idea of Spring Boot, in a dozen lines. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dojo.greeter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreeterAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean // the user's own Greeter wins: auto-configuration only fills gaps
	Greeter greeter(@Value("${dojo.greeter.prefix:Hello}") String prefix) {
		return name -> prefix + ", " + name + "!";
	}

}
