package dojo.javacore.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L2")
class TopKTest {

	@Test
	void mostFrequentFirst() {
		assertThat(TopK.mostFrequentWords("the cat and the hat and the bat", 2)).containsExactly("the", "and");
	}

	@Test
	void tiesAreAlphabetical() {
		assertThat(TopK.mostFrequentWords("b a c b a c d", 3)).containsExactly("a", "b", "c");
	}

	@Test
	void ignoresCaseAndPunctuation() {
		assertThat(TopK.mostFrequentWords("Java! java, JAVA? Spring... spring; react", 2)).containsExactly("java", "spring");
	}

	@Test
	void kLargerThanTheNumberOfWords() {
		assertThat(TopK.mostFrequentWords("one two two", 10)).containsExactly("two", "one");
		assertThat(TopK.mostFrequentWords("", 3)).isEmpty();
		assertThat(TopK.mostFrequentWords("a b", 0)).isEmpty();
	}

	@Test
	void handlesALargeText() {
		String text = IntStream.range(0, 300_000).mapToObj(i -> "w" + toLetters(i % 50_000))
				.collect(Collectors.joining(" ")) + " zzz zzz zzz zzz zzz zzz zzz";
		assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertThat(TopK.mostFrequentWords(text, 1)).containsExactly("zzz"));
	}

	private static String toLetters(int n) {
		StringBuilder sb = new StringBuilder();
		do {
			sb.append((char) ('a' + n % 26));
			n /= 26;
		} while (n > 0);
		return sb.toString();
	}

}
