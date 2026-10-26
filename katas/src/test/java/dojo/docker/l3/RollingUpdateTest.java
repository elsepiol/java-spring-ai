package dojo.docker.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dojo.docker.l3.RollingUpdate.State;

@Tag("docker")
@Tag("L3")
class RollingUpdateTest {

	@Test
	void surgeOneUnavailableZeroReplacesOnePodAtATime() {
		assertThat(RollingUpdate.plan(4, 1, 0)).containsExactly(
				new State(3, 1), new State(2, 2), new State(1, 3), new State(0, 4));
	}

	@Test
	void withoutSurgeOldPodsGoFirst() {
		assertThat(RollingUpdate.plan(4, 0, 1)).containsExactly(
				new State(3, 0), new State(2, 1), new State(1, 2), new State(0, 3), new State(0, 4));
	}

	@Test
	void moreRoomMeansFewerSteps() {
		assertThat(RollingUpdate.plan(4, 2, 1)).containsExactly(new State(1, 2), new State(0, 4));
		assertThat(RollingUpdate.plan(4, 4, 0)).as("blue-green-like").containsExactly(new State(0, 4));
	}

	@ParameterizedTest(name = "replicas={0} surge={1} unavailable={2}")
	@CsvSource({ "10, 3, 2", "5, 1, 0", "5, 0, 1", "3, 2, 2", "1, 1, 0", "7, 25, 0" })
	void neverBreaksTheLimits(int replicas, int surge, int unavailable) {
		List<State> plan = RollingUpdate.plan(replicas, surge, unavailable);
		assertThat(plan).isNotEmpty();
		assertThat(plan.getLast()).isEqualTo(new State(0, replicas));
		for (State s : plan) {
			assertThat(s.oldPods() + s.newPods()).as("available").isGreaterThanOrEqualTo(replicas - unavailable);
			assertThat(s.newPods()).isLessThanOrEqualTo(replicas);
		}
	}

	@Test
	void refusesSettingsThatCannotProgress() {
		assertThatIllegalArgumentException().isThrownBy(() -> RollingUpdate.plan(3, 0, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> RollingUpdate.plan(3, -1, 1));
	}

	@Test
	void resolvesPercentagesTheKubernetesWay() {
		assertThat(RollingUpdate.resolve(10, "25%", "25%")).containsExactly(3, 2);
		assertThat(RollingUpdate.resolve(4, "25%", "25%")).containsExactly(1, 1);
		assertThat(RollingUpdate.resolve(3, "25%", "25%")).as("never 0 surge AND 0 unavailable by rounding").containsExactly(1, 0);
		assertThat(RollingUpdate.resolve(10, "2", "0")).containsExactly(2, 0);
	}

}
