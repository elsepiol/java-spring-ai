package dojo.web.l1;

import static dojo.web.l1.HttpSemantics.Outcome.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L1")
class HttpSemanticsTest {

	@Test
	void statusCodes() {
		Map<HttpSemantics.Outcome, Integer> expected = Map.ofEntries(
				Map.entry(FOUND, 200), Map.entry(CREATED, 201), Map.entry(ACCEPTED_FOR_LATER, 202), Map.entry(DELETED, 204),
				Map.entry(MOVED_FOREVER, 301), Map.entry(CLIENT_COPY_IS_CURRENT, 304), Map.entry(INVALID_INPUT, 400),
				Map.entry(NOT_LOGGED_IN, 401), Map.entry(NOT_ALLOWED, 403), Map.entry(NO_SUCH_RESOURCE, 404),
				Map.entry(WRONG_METHOD, 405), Map.entry(CONFLICT_WITH_CURRENT_STATE, 409), Map.entry(STALE_VERSION, 412),
				Map.entry(UNSUPPORTED_FORMAT, 415), Map.entry(TOO_MANY, 429), Map.entry(OUR_BUG, 500),
				Map.entry(TEMPORARILY_UNAVAILABLE, 503), Map.entry(UPSTREAM_TIMEOUT, 504));
		expected.forEach((outcome, code) -> assertThat(HttpSemantics.status(outcome)).as(outcome.name()).isEqualTo(code));
	}

	@Test
	void safeMethods() {
		assertThat(HttpSemantics.isSafe("GET")).isTrue();
		assertThat(HttpSemantics.isSafe("HEAD")).isTrue();
		assertThat(HttpSemantics.isSafe("OPTIONS")).isTrue();
		assertThat(HttpSemantics.isSafe("PUT")).isFalse();
		assertThat(HttpSemantics.isSafe("DELETE")).isFalse();
		assertThat(HttpSemantics.isSafe("POST")).isFalse();
	}

	@Test
	void idempotentMethods() {
		assertThat(HttpSemantics.isIdempotent("GET")).isTrue();
		assertThat(HttpSemantics.isIdempotent("PUT")).as("PUT replaces: twice = once").isTrue();
		assertThat(HttpSemantics.isIdempotent("DELETE")).as("deleting twice leaves it deleted").isTrue();
		assertThat(HttpSemantics.isIdempotent("POST")).as("two POSTs, two orders").isFalse();
		assertThat(HttpSemantics.isIdempotent("PATCH")).as("PATCH {\"stock\": \"+1\"} twice adds 2").isFalse();
	}

}
