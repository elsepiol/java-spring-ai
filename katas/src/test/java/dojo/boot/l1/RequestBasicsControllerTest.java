package dojo.boot.l1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Tag("boot")
@Tag("L1")
@WebMvcTest(RequestBasicsController.class)
class RequestBasicsControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void queryParameterWithADefault() throws Exception {
		mvc.perform(get("/api/hello").param("name", "Ann")).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Hello, Ann!"));
		mvc.perform(get("/api/hello")).andExpect(jsonPath("$.message").value("Hello, world!"));
	}

	@Test
	void pathVariableAndRequiredParameter() throws Exception {
		mvc.perform(get("/api/convert/1050").param("to", "USD")).andExpect(status().isOk())
				.andExpect(jsonPath("$.cents").value(1050))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.amount").value("10.50"));
		mvc.perform(get("/api/convert/1050")).andExpect(status().isBadRequest());
	}

	@Test
	void headers() throws Exception {
		mvc.perform(get("/api/whoami").header("X-Client", "mobile-app").header("User-Agent", "curl/8"))
				.andExpect(jsonPath("$.client").value("mobile-app"))
				.andExpect(jsonPath("$.agent").value("curl/8"));
		mvc.perform(get("/api/whoami").header("X-Client", "web")).andExpect(jsonPath("$.agent").value("unknown"));
		mvc.perform(get("/api/whoami")).andExpect(status().isBadRequest());
	}

}
