package dojo.design.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dojo.design.l3.Saga.Outcome;
import dojo.design.l3.Saga.Step;

@Tag("design")
@Tag("L3")
class SagaTest {

	private final List<String> effects = new ArrayList<>();

	private Step ok(String name) {
		return new Step(name, () -> effects.add("+" + name), () -> effects.add("-" + name));
	}

	private Step failing(String name) {
		return new Step(name, () -> {
			throw new IllegalStateException(name + " failed");
		}, () -> effects.add("-" + name));
	}

	@Test
	void completesWhenEveryStepSucceeds() {
		Saga saga = new Saga(3);
		assertThat(saga.run(List.of(ok("charge"), ok("reserve"), ok("ship")))).isEqualTo(Outcome.COMPLETED);
		assertThat(effects).containsExactly("+charge", "+reserve", "+ship");
		assertThat(saga.log()).containsExactly("do:charge", "do:reserve", "do:ship");
	}

	@Test
	void compensatesCompletedStepsInReverseOrder() {
		Saga saga = new Saga(3);
		Outcome outcome = saga.run(List.of(ok("charge"), ok("reserve"), failing("ship"), ok("notify")));
		assertThat(outcome).isEqualTo(Outcome.COMPENSATED);
		assertThat(effects).containsExactly("+charge", "+reserve", "-reserve", "-charge");
		assertThat(saga.log()).containsExactly("do:charge", "do:reserve", "do:ship", "failed:ship", "undo:reserve", "undo:charge");
	}

	@Test
	void aFirstStepFailureNeedsNoCompensation() {
		Saga saga = new Saga(3);
		assertThat(saga.run(List.of(failing("charge"), ok("reserve")))).isEqualTo(Outcome.COMPENSATED);
		assertThat(effects).isEmpty();
	}

	@Test
	void retriesFlakyCompensations() {
		AtomicInteger attempts = new AtomicInteger();
		Step refund = new Step("charge", () -> effects.add("+charge"), () -> {
			if (attempts.incrementAndGet() < 3) {
				throw new IllegalStateException("payment provider timeout");
			}
			effects.add("-charge");
		});
		Saga saga = new Saga(3);
		assertThat(saga.run(List.of(refund, failing("reserve")))).isEqualTo(Outcome.COMPENSATED);
		assertThat(attempts).hasValue(3);
		assertThat(effects).containsExactly("+charge", "-charge");
	}

	@Test
	void keepsCompensatingAndAsksForHelpWhenACompensationKeepsFailing() {
		Step stuck = new Step("reserve", () -> effects.add("+reserve"), () -> {
			throw new IllegalStateException("inventory down");
		});
		Saga saga = new Saga(2);
		Outcome outcome = saga.run(List.of(ok("charge"), stuck, failing("ship")));
		assertThat(outcome).isEqualTo(Outcome.NEEDS_ATTENTION);
		assertThat(effects).containsExactly("+charge", "+reserve", "-charge");
		assertThat(saga.log()).containsExactly("do:charge", "do:reserve", "do:ship", "failed:ship",
				"undo:reserve", "undo:reserve", "undo-failed:reserve", "undo:charge");
	}

}
