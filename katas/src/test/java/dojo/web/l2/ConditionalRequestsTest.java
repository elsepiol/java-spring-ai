package dojo.web.l2;

import static dojo.web.l2.ConditionalRequests.Decision.NOT_MODIFIED_304;
import static dojo.web.l2.ConditionalRequests.Decision.PRECONDITION_FAILED_412;
import static dojo.web.l2.ConditionalRequests.Decision.PROCEED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L2")
class ConditionalRequestsTest {

	private static ConditionalRequests.Decision eval(String method, String ifMatch, String ifNoneMatch, String current) {
		return ConditionalRequests.evaluate(method, Optional.ofNullable(ifMatch), Optional.ofNullable(ifNoneMatch), Optional.ofNullable(current));
	}

	@Test
	void unconditionalRequestsProceed() {
		assertThat(eval("GET", null, null, "\"v1\"")).isEqualTo(PROCEED);
	}

	@Test
	void cachedCopyStillCurrentGives304() {
		assertThat(eval("GET", null, "\"v7\"", "\"v7\"")).isEqualTo(NOT_MODIFIED_304);
		assertThat(eval("HEAD", null, "\"v1\", \"v7\"", "\"v7\"")).isEqualTo(NOT_MODIFIED_304);
		assertThat(eval("GET", null, "W/\"v7\"", "\"v7\"")).as("weak comparison for If-None-Match").isEqualTo(NOT_MODIFIED_304);
	}

	@Test
	void changedResourceIsSentAgain() {
		assertThat(eval("GET", null, "\"v6\"", "\"v7\"")).isEqualTo(PROCEED);
	}

	@Test
	void updateWithTheCurrentVersionProceeds() {
		assertThat(eval("PUT", "\"v7\"", null, "\"v7\"")).isEqualTo(PROCEED);
		assertThat(eval("PUT", "\"v1\", \"v7\"", null, "\"v7\"")).isEqualTo(PROCEED);
		assertThat(eval("DELETE", "*", null, "\"v7\"")).isEqualTo(PROCEED);
	}

	@Test
	void updateWithAStaleVersionIsRefused() {
		assertThat(eval("PUT", "\"v6\"", null, "\"v7\"")).isEqualTo(PRECONDITION_FAILED_412);
		assertThat(eval("PUT", "W/\"v7\"", null, "W/\"v7\"")).as("strong comparison: weak tags never match").isEqualTo(PRECONDITION_FAILED_412);
		assertThat(eval("PUT", "*", null, null)).as("resource does not exist").isEqualTo(PRECONDITION_FAILED_412);
	}

	@Test
	void ifNoneMatchStarPreventsOverwritingOnCreate() {
		// PUT /users/ann with If-None-Match: * means "create only if it does not exist yet"
		assertThat(eval("PUT", null, "*", null)).isEqualTo(PROCEED);
		assertThat(eval("PUT", null, "*", "\"v1\"")).isEqualTo(PRECONDITION_FAILED_412);
	}

	@Test
	void ifMatchTakesPrecedenceOverIfNoneMatch() {
		assertThat(eval("PUT", "\"v7\"", "\"v7\"", "\"v7\"")).isEqualTo(PROCEED);
	}

}
