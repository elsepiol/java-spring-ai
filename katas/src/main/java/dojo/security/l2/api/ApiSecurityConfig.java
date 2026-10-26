package dojo.security.l2.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import dojo.Kata;

/**
 * KATA security L2 "ApiSecurity" (tracks/09-security/L2-practical.md, section 3).
 *
 * Write the SecurityFilterChain for this JSON API (see ApiController):
 * - /api/public/** is open to everybody;
 * - /api/admin/** needs the role ADMIN (403 for other logged-in users);
 * - every other request needs an authenticated user: deny by default, even for URLs that do not
 *   exist yet (anonymous -> 401, so unknown URLs do not leak which ones exist);
 * - authentication with HTTP Basic (a 401 carries a WWW-Authenticate header);
 * - stateless: no HTTP session is created, so no JSESSIONID cookie is ever sent;
 * - CSRF protection disabled, which is safe ONLY because nothing authenticates through cookies
 *   (lesson section 4 explains why).
 * The users and the password encoder below are given.
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

	@Bean
	SecurityFilterChain api(HttpSecurity http) throws Exception {
		return Kata.todo("tracks/09-security/L2-practical.md section 3");
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt, with a {bcrypt} prefix
	}

	@Bean
	UserDetailsService users(PasswordEncoder encoder) {
		return new InMemoryUserDetailsManager(
				User.withUsername("alice").password(encoder.encode("alice-pw")).roles("USER").build(),
				User.withUsername("root").password(encoder.encode("root-pw")).roles("USER", "ADMIN").build());
	}

}
