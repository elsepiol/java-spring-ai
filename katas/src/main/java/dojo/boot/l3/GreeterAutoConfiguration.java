package dojo.boot.l3;

/**
 * KATA boot L3 "GreeterAutoConfiguration" (tracks/07-spring-boot-web/L3-production.md, section 1: auto-configuration).
 *
 * Write the auto-configuration a "dojo-greeter-starter" would ship, exactly like Spring Boot's own:
 * - it provides a Greeter bean that returns  <prefix> + ", " + name + "!"  where the prefix is the
 *   property dojo.greeter.prefix (default "Hello");
 * - it BACKS OFF when the application defines its own Greeter bean;
 * - dojo.greeter.enabled=false switches it off completely (enabled when the property is missing);
 * - it is registered for Boot in src/main/resources/META-INF/spring/
 *   org.springframework.boot.autoconfigure.AutoConfiguration.imports (one class name per line).
 * Use @AutoConfiguration, @ConditionalOnMissingBean, @ConditionalOnProperty.
 */
public class GreeterAutoConfiguration {
}
