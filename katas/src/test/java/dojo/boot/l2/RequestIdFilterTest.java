package dojo.boot.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag("boot")
@Tag("L2")
@WebMvcTest(RequestIdFilterTest.EchoController.class)
@Import({ RequestIdFilterTest.EchoController.class, RequestIdFilter.class })
class RequestIdFilterTest {

	@RestController
	static class EchoController {
		@GetMapping("/echo")
		Map<String, String> echo() {
			return Map.of("mdc", String.valueOf(MDC.get("requestId"))); // what a log line would show
		}

		@GetMapping("/boom")
		String boom() {
			throw new IllegalStateException("handler failed");
		}
	}

	@Autowired
	MockMvc mvc;

	@Test
	void reusesASafeIncomingId() throws Exception {
		mvc.perform(get("/echo").header("X-Request-Id", "abc-123"))
				.andExpect(header().string("X-Request-Id", "abc-123"))
				.andExpect(jsonPath("$.mdc").value("abc-123"));
	}

	@Test
	void generatesOneWhenMissing() throws Exception {
		MvcResult result = mvc.perform(get("/echo")).andReturn();
		String id = result.getResponse().getHeader("X-Request-Id");
		assertThat(id).matches("[0-9a-f-]{36}");
		assertThat(result.getResponse().getContentAsString()).contains(id);
	}

	@Test
	void replacesUnsafeIds() throws Exception {
		for (String bad : new String[] { "x".repeat(65), "evil\r\nSet-Cookie: a=b", "<script>" }) {
			String id = mvc.perform(get("/echo").header("X-Request-Id", bad)).andReturn().getResponse().getHeader("X-Request-Id");
			assertThat(id).as(bad).matches("[0-9a-f-]{36}");
		}
	}

	@Test
	void theMdcIsClearedAfterTheRequestEvenOnFailure() throws Exception {
		mvc.perform(get("/echo"));
		assertThat(MDC.get("requestId")).isNull();
		try {
			mvc.perform(get("/boom"));
		} catch (Exception expected) {
			// MockMvc rethrows unhandled exceptions
		}
		assertThat(MDC.get("requestId")).isNull();
	}

}
