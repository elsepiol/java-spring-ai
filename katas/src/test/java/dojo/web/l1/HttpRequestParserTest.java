package dojo.web.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.web.l1.HttpRequestParser.HttpRequest;
import dojo.web.l1.HttpRequestParser.MalformedRequestException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L1")
class HttpRequestParserTest {

	private static final String POST = "POST /api/v1/orders?coupon=SPRING%2026&tag=a&tag=b&note=hello+world&flag HTTP/1.1\r\n"
			+ "Host: shop.example.com\r\n"
			+ "content-type:  application/json \r\n"
			+ "Accept: application/json\r\n"
			+ "Accept: text/plain\r\n"
			+ "Content-Length: 13\r\n"
			+ "\r\n"
			+ "{\"items\":[1]}";

	@Test
	void parsesTheRequestLine() {
		HttpRequest r = HttpRequestParser.parse(POST);
		assertThat(r.method()).isEqualTo("POST");
		assertThat(r.path()).isEqualTo("/api/v1/orders");
	}

	@Test
	void decodesQueryParameters() {
		HttpRequest r = HttpRequestParser.parse(POST);
		assertThat(r.query().get("coupon")).containsExactly("SPRING 26");
		assertThat(r.query().get("tag")).containsExactly("a", "b");
		assertThat(r.query().get("note")).containsExactly("hello world");
		assertThat(r.query().get("flag")).containsExactly("");
	}

	@Test
	void headersAreCaseInsensitiveTrimmedAndJoined() {
		HttpRequest r = HttpRequestParser.parse(POST);
		assertThat(r.header("Content-Type")).contains("application/json");
		assertThat(r.header("HOST")).contains("shop.example.com");
		assertThat(r.header("accept")).contains("application/json, text/plain");
		assertThat(r.header("Authorization")).isEmpty();
	}

	@Test
	void readsExactlyContentLengthCharacters() {
		assertThat(HttpRequestParser.parse(POST).body()).isEqualTo("{\"items\":[1]}");
		String pipelined = "GET / HTTP/1.1\r\nHost: x\r\nContent-Length: 2\r\n\r\nokGET /next HTTP/1.1";
		assertThat(HttpRequestParser.parse(pipelined).body()).isEqualTo("ok");
	}

	@Test
	void aGetWithoutBodyOrQuery() {
		HttpRequest r = HttpRequestParser.parse("GET /products/7 HTTP/1.1\r\nHost: shop\r\n\r\n");
		assertThat(r.body()).isEmpty();
		assertThat(r.query()).isEmpty();
		assertThat(r.path()).isEqualTo("/products/7");
	}

	@Test
	void rejectsMalformedRequests() {
		for (String bad : List.of(
				"GET /x HTTP/1.1\r\n\r\n",                                    // no Host
				"GET /x HTTP/2\r\nHost: a\r\n\r\n",                           // not HTTP/1.1 text
				"GET /x\r\nHost: a\r\n\r\n",                                  // no version
				"GET /x HTTP/1.1\r\nHost a\r\n\r\n",                          // header without colon
				"POST /x HTTP/1.1\r\nHost: a\r\nContent-Length: 10\r\n\r\nshort", // truncated body
				"GET /x HTTP/1.1\r\nHost: a\r\n")) {                          // no empty line
			assertThatThrownBy(() -> HttpRequestParser.parse(bad)).as(bad).isInstanceOf(MalformedRequestException.class);
		}
	}

}
