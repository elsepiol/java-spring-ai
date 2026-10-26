package dojo.ops.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("ops")
@Tag("L1")
class CidrTest {

	@Test
	void parsesAndNormalises() {
		assertThat(Cidr.parse("10.20.0.0/16")).hasToString("10.20.0.0/16");
		assertThat(Cidr.parse("10.20.1.5/16")).hasToString("10.20.0.0/16");
		assertThat(Cidr.parse("192.168.1.77/32")).hasToString("192.168.1.77/32");
		assertThat(Cidr.parse("255.255.255.255/0")).hasToString("0.0.0.0/0");
	}

	@Test
	void computesSizes() {
		assertThat(Cidr.parse("10.0.0.0/24").size()).isEqualTo(256);
		assertThat(Cidr.parse("10.0.0.0/16").size()).isEqualTo(65_536);
		assertThat(Cidr.parse("10.0.0.1/32").size()).isEqualTo(1);
		assertThat(Cidr.parse("0.0.0.0/0").size()).isEqualTo(4_294_967_296L);
	}

	@Test
	void checksMembership() {
		Cidr block = Cidr.parse("172.16.0.0/12");
		assertThat(block.contains("172.16.0.1")).isTrue();
		assertThat(block.contains("172.31.255.255")).isTrue();
		assertThat(block.contains("172.32.0.0")).isFalse();
		assertThat(Cidr.parse("0.0.0.0/0").contains("8.8.8.8")).isTrue();
		assertThat(Cidr.parse("200.1.2.3/32").contains("200.1.2.3")).isTrue();
	}

	@Test
	void detectsOverlaps() {
		assertThat(Cidr.parse("10.0.0.0/16").overlaps(Cidr.parse("10.0.128.0/17"))).isTrue();
		assertThat(Cidr.parse("10.0.128.0/17").overlaps(Cidr.parse("10.0.0.0/16"))).isTrue();
		assertThat(Cidr.parse("10.0.0.0/17").overlaps(Cidr.parse("10.0.128.0/17"))).isFalse();
		assertThat(Cidr.parse("10.1.0.0/16").overlaps(Cidr.parse("10.2.0.0/16"))).isFalse();
	}

	@Test
	void knowsPrivateRanges() {
		assertThat(Cidr.parse("10.42.0.0/16").isPrivate()).isTrue();
		assertThat(Cidr.parse("172.20.0.0/16").isPrivate()).isTrue();
		assertThat(Cidr.parse("192.168.0.0/24").isPrivate()).isTrue();
		assertThat(Cidr.parse("172.32.0.0/16").isPrivate()).isFalse();
		assertThat(Cidr.parse("8.0.0.0/8").isPrivate()).isFalse();
		assertThat(Cidr.parse("10.0.0.0/7").isPrivate()).as("bigger than 10/8").isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "10.0.0.0", "10.0.0.0/33", "10.0.0/8", "256.0.0.0/8", "10.0.0.0/-1", "a.b.c.d/8", "10.0.0.0/8/8", "10..0.0/8" })
	void rejectsGarbage(String text) {
		assertThatIllegalArgumentException().isThrownBy(() -> Cidr.parse(text));
	}

}
