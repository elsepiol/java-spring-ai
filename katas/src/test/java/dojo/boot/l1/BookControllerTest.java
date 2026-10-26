package dojo.boot.l1;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** A web slice: only MVC infrastructure + this controller, no server; MockMvc calls it in-process. */
@Tag("boot")
@Tag("L1")
@WebMvcTest(BookController.class)
@Import(BookStore.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // fresh BookStore per test
class BookControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	BookStore store;

	@Test
	void listsBooksInIdOrder() throws Exception {
		store.add("Effective Java", "Bloch", 4500);
		store.add("Java Concurrency in Practice", "Goetz", 5200);
		mvc.perform(get("/api/books"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].title").value("Effective Java"))
				.andExpect(jsonPath("$[1].author").value("Goetz"));
	}

	@Test
	void getsOneOr404() throws Exception {
		long id = store.add("Refactoring", "Fowler", 3900).id();
		mvc.perform(get("/api/books/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.priceCents").value(3900));
		mvc.perform(get("/api/books/999")).andExpect(status().isNotFound());
	}

	@Test
	void createsWith201AndLocation() throws Exception {
		mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Domain-Driven Design\",\"author\":\"Evans\",\"priceCents\":6100}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/books/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Domain-Driven Design"));
		mvc.perform(get("/api/books/1")).andExpect(status().isOk());
	}

	@Test
	void deletesWith204Or404() throws Exception {
		long id = store.add("Clean Code", "Martin", 3000).id();
		mvc.perform(delete("/api/books/" + id)).andExpect(status().isNoContent());
		mvc.perform(delete("/api/books/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void aNonNumericIdIsABadRequest() throws Exception {
		mvc.perform(get("/api/books/abc")).andExpect(status().isBadRequest());
	}

}
