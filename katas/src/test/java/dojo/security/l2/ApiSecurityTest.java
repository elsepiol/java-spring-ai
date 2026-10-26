package dojo.security.l2;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import dojo.security.l2.api.ApiController;
import dojo.security.l2.api.ApiSecurityConfig;

/** Plain Spring MVC + Spring Security (no Boot auto-configuration): the filter chain is all yours. */
@Tag("security")
@Tag("L2")
@SpringJUnitWebConfig({ ApiSecurityTest.Mvc.class, ApiController.class, ApiSecurityConfig.class })
class ApiSecurityTest {

	@Configuration
	@EnableWebMvc
	static class Mvc {

	}

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void publicEndpointsNeedNoLogin() throws Exception {
		mvc.perform(get("/api/public/ping")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("up"));
	}

	@Test
	void anonymousCallersGet401WithAChallenge() throws Exception {
		mvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Basic")));
	}

	@Test
	void wrongPasswordsGet401() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("alice", "nope"))).andExpect(status().isUnauthorized());
	}

	@Test
	void loggedInUsersSeeThemselves() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("alice", "alice-pw")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("alice"))
			.andExpect(jsonPath("$.authorities", org.hamcrest.Matchers.hasItem("ROLE_USER")))
			.andExpect(jsonPath("$.authorities", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("ROLE_ADMIN"))));
	}

	@Test
	void adminEndpointsNeedTheAdminRole() throws Exception {
		mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/admin/stats").with(httpBasic("alice", "alice-pw"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/admin/stats").with(httpBasic("root", "root-pw"))).andExpect(status().isOk());
	}

	@Test
	void unknownUrlsAreDeniedByDefault() throws Exception {
		mvc.perform(get("/internal/debug")).andExpect(status().isUnauthorized());
		mvc.perform(get("/internal/debug").with(httpBasic("alice", "alice-pw"))).andExpect(status().isNotFound());
	}

	@Test
	void postsWorkWithoutACsrfTokenBecauseNoCookiesAreUsed() throws Exception {
		mvc.perform(post("/api/notes").with(httpBasic("alice", "alice-pw"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"text\":\"hi\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.owner").value("alice"));
	}

	@Test
	void isStateless() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("alice", "alice-pw")))
			.andExpect(status().isOk())
			.andExpect(cookie().doesNotExist("JSESSIONID"))
			.andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
		// the next request must authenticate again: nothing was remembered
		mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

}
