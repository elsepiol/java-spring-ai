package dojo.boot.l3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reference solution (alternative: @ConfigurationPropertiesScan on the application class). */
@Configuration
@EnableConfigurationProperties(ShippingProperties.class)
public class ShippingConfig {
}
