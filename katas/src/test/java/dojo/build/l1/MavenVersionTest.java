package dojo.build.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("build")
@Tag("L1")
class MavenVersionTest {

	private static MavenVersion v(String s) {
		return MavenVersion.parse(s);
	}

	@Test
	void numbersCompareNumericallyNotAsText() {
		assertThat(v("1.10")).isGreaterThan(v("1.9"));
		assertThat(v("2.0")).isGreaterThan(v("1.99.99"));
		assertThat(v("10.0")).isGreaterThan(v("9.0"));
	}

	@Test
	void missingSegmentsAreZero() {
		assertThat(v("1")).isEqualByComparingTo(v("1.0.0"));
		assertThat(v("1.0")).isEqualTo(v("1")).hasSameHashCodeAs(v("1.0.0"));
	}

	@Test
	void aSnapshotComesBeforeItsRelease() {
		assertThat(v("2.0-SNAPSHOT")).isLessThan(v("2.0"));
		assertThat(v("2.0-SNAPSHOT")).isGreaterThan(v("1.9"));
		assertThat(v("2.0-SNAPSHOT").isSnapshot()).isTrue();
		assertThat(v("2.0").isSnapshot()).isFalse();
	}

	@Test
	void qualifiersHaveAnOrder() {
		List<String> sorted = Stream.of("1.0-sp", "1.0", "1.0-RC2", "1.0-alpha", "1.0-SNAPSHOT", "1.0-rc1", "1.0-beta-2", "1.0-M1", "1.0-b1")
				.map(MavenVersion::parse).sorted().map(MavenVersion::toString).toList();
		assertThat(sorted).containsExactly("1.0-alpha", "1.0-b1", "1.0-beta-2", "1.0-M1", "1.0-rc1", "1.0-RC2", "1.0-SNAPSHOT", "1.0", "1.0-sp");
	}

	@Test
	void shortQualifiersMeanTheLongOnes() {
		assertThat(v("1.0-cr1")).isEqualByComparingTo(v("1.0-rc1"));
		assertThat(v("1.0-a")).isEqualByComparingTo(v("1.0-alpha"));
	}

	@Test
	void rejectsNonsense() {
		assertThatThrownBy(() -> v("one.two")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> v("1.0-banana")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void keepsTheOriginalText() {
		assertThat(v("1.0.0-RC1")).hasToString("1.0.0-RC1");
	}

}
