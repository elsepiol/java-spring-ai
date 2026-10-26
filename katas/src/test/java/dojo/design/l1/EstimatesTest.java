package dojo.design.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("design")
@Tag("L1")
class EstimatesTest {

	@Test
	void turnsDailyVolumeIntoQps() {
		// 10 million users x 10 requests a day
		assertThat(Estimates.averageQps(100_000_000)).isCloseTo(1157.4, within(0.1));
		assertThat(Estimates.peakQps(100_000_000, 3)).isEqualTo(3473);
		assertThat(Estimates.peakQps(86_400, 1)).isEqualTo(1);
	}

	@Test
	void computesStorageWithoutOverflow() {
		// 5 million new orders a day, 2 KB each, kept 5 years, 3 replicas
		long bytes = Estimates.storageBytes(5_000_000, 2_000, 5 * 365, 3);
		assertThat(bytes).isEqualTo(54_750_000_000_000L);
		assertThat(Estimates.humanBytes(bytes)).isEqualTo("54.8 TB");
	}

	@Test
	void sizesTheFleetWithHeadroomAndASpare() {
		// 3,473 peak QPS, 500 QPS per server at 60% target -> 12 for the load + 1 spare
		assertThat(Estimates.serversNeeded(3_473, 500, 0.6)).isEqualTo(13);
		assertThat(Estimates.serversNeeded(10, 1_000, 0.6)).as("never fewer than two").isEqualTo(2);
		assertThat(Estimates.serversNeeded(600, 1_000, 0.6)).isEqualTo(2);
		assertThat(Estimates.serversNeeded(601, 1_000, 0.6)).isEqualTo(3);
	}

	@Test
	void printsHumanReadableSizes() {
		assertThat(Estimates.humanBytes(999)).isEqualTo("999 B");
		assertThat(Estimates.humanBytes(1_500)).isEqualTo("1.5 KB");
		assertThat(Estimates.humanBytes(2_500_000_000L)).isEqualTo("2.5 GB");
		assertThat(Estimates.humanBytes(3_000_000_000_000_000L)).isEqualTo("3.0 PB");
	}

}
