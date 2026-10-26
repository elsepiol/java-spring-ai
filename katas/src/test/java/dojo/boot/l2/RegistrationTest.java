package dojo.boot.l2;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Tag("boot")
@Tag("L2")
@WebMvcTest(RegistrationController.class)
@Import({ UserDirectory.class, ApiErrors.class })
class RegistrationTest {

	@Autowired
	MockMvc mvc;

	private ResultActions register(String json) throws Exception {
		return mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void validInputRegisters() throws Exception {
		register("{\"email\":\"ann@example.com\",\"password\":\"correct horse battery\",\"age\":30}")
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void invalidFieldsAreAllReportedAsProblemDetails() throws Exception {
		register("{\"email\":\"not-an-email\",\"password\":\"short\",\"age\":9}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.age").exists());
	}

	@Test
	void aMissingEmailIsInvalid() throws Exception {
		register("{\"password\":\"correct horse battery\",\"age\":30}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void aDuplicateEmailIs409() throws Exception {
		register("{\"email\":\"bob@example.com\",\"password\":\"correct horse battery\",\"age\":30}").andExpect(status().isCreated());
		register("{\"email\":\"BOB@example.com\",\"password\":\"correct horse battery\",\"age\":40}")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Email already registered"))
				.andExpect(content().string(not(containsString("Exception"))));
	}

	@Test
	void brokenJsonIsAProblemToo() throws Exception {
		register("{\"email\": ")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
