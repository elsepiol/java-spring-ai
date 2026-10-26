package dojo.design.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("design")
@Tag("L2")
class ShortenerTest {

	@ParameterizedTest(name = "{0} <-> {1}")
	@CsvSource({ "0, 0", "9, 9", "10, a", "35, z", "36, A", "61, Z", "62, 10", "3843, ZZ", "3844, 100", "56800235583, ZZZZZZ" })
	void encodesAndDecodesBase62(long id, String code) {
		assertThat(Shortener.encode(id)).isEqualTo(code);
		assertThat(Shortener.decode(code)).isEqualTo(id);
	}

	@Test
	void decodeRejectsOtherCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() -> Shortener.decode("ab_c"));
		assertThatIllegalArgumentException().isThrownBy(() -> Shortener.decode(""));
	}

	@Test
	void shortensAndResolves() {
		Shortener s = new Shortener();
		String a = s.shorten("https://example.com/a");
		String b = s.shorten("https://example.com/b");
		assertThat(a).isEqualTo("1");
		assertThat(b).isEqualTo("2");
		assertThat(s.resolve(a)).contains("https://example.com/a");
		assertThat(s.resolve("zzz")).isEmpty();
	}

	@Test
	void isIdempotentPerUrl() {
		Shortener s = new Shortener();
		String first = s.shorten("https://example.com/sale");
		assertThat(s.shorten("https://example.com/sale")).isEqualTo(first);
		assertThat(s.shorten("https://example.com/other")).isEqualTo("2");
	}

	@Test
	void supportsCustomAliasesWithoutCollisions() {
		Shortener s = new Shortener();
		assertThat(s.shorten("https://example.com/summer", "summer-sale")).isEqualTo("summer-sale");
		assertThat(s.shorten("https://example.com/summer", "summer-sale")).as("same url, same alias: fine").isEqualTo("summer-sale");
		assertThatThrownBy(() -> s.shorten("https://evil.example/", "summer-sale")).isInstanceOf(Shortener.AliasTakenException.class);
		s.shorten("https://example.com/x", "abc");
		// a generated code must never hand out an alias: skip ahead past it
		Shortener t = new Shortener();
		t.shorten("https://example.com/alias", "100");
		for (int i = 0; i < 3843; i++) {
			t.shorten("https://example.com/" + i);
		}
		String next = t.shorten("https://example.com/next");
		assertThat(next).isNotEqualTo("100");
		assertThat(t.resolve("100")).contains("https://example.com/alias");
	}

	@Test
	void validatesInput() {
		Shortener s = new Shortener();
		assertThatIllegalArgumentException().isThrownBy(() -> s.shorten("javascript:alert(1)"));
		assertThatIllegalArgumentException().isThrownBy(() -> s.shorten("data:text/html,hi"));
		assertThatIllegalArgumentException().isThrownBy(() -> s.shorten("not a url"));
		assertThatIllegalArgumentException().isThrownBy(() -> s.shorten("https://example.com", "a"));
		assertThatIllegalArgumentException().isThrownBy(() -> s.shorten("https://example.com", "bad alias!"));
	}

}
