package dojo.design.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("design")
@Tag("L2")
class SnowflakeTest {

	private static final long T0 = Snowflake.EPOCH + 1_000_000;

	/** Returns the scripted times in order, then keeps returning the last one plus 1 per call. */
	private static LongSupplier script(long... times) {
		Deque<Long> queue = new ArrayDeque<>();
		for (long t : times) {
			queue.add(t);
		}
		long[] last = { times[times.length - 1] };
		return () -> queue.isEmpty() ? ++last[0] : (last[0] = queue.poll());
	}

	@Test
	void packsTimeWorkerAndSequence() {
		Snowflake s = new Snowflake(7, script(T0, T0, T0 + 1));
		long a = s.nextId();
		long b = s.nextId();
		long c = s.nextId();
		assertThat(Snowflake.parts(a)).containsExactly(T0, 7, 0);
		assertThat(Snowflake.parts(b)).containsExactly(T0, 7, 1);
		assertThat(Snowflake.parts(c)).containsExactly(T0 + 1, 7, 0);
		assertThat(a).isLessThan(b).isLessThan(c);
	}

	@Test
	void waitsForTheNextMillisecondWhenTheSequenceRunsOut() {
		long[] calls = { 0 };
		LongSupplier clock = () -> calls[0]++ < 4096 + 10 ? T0 : T0 + 1; // stuck at T0 for a while
		Snowflake s = new Snowflake(1, clock);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 4097; i++) {
			ids.add(s.nextId());
		}
		assertThat(ids).hasSize(4097);
		long[] lastParts = Snowflake.parts(ids.stream().max(Long::compare).orElseThrow());
		assertThat(lastParts).containsExactly(T0 + 1, 1, 0);
	}

	@Test
	void toleratesASmallBackwardsJumpWithoutDuplicates() {
		Snowflake s = new Snowflake(2, script(T0 + 10, T0 + 7, T0 + 8, T0 + 9, T0 + 10, T0 + 11));
		long first = s.nextId();
		long second = s.nextId(); // the clock says T0+7: wait until it is back past T0+10
		assertThat(second).isGreaterThan(first);
		assertThat(Snowflake.parts(second)[0]).isGreaterThanOrEqualTo(T0 + 10);
	}

	@Test
	void refusesALargeBackwardsJump() {
		Snowflake s = new Snowflake(2, script(T0 + 100, T0));
		s.nextId();
		assertThatIllegalStateException().isThrownBy(s::nextId);
	}

	@Test
	void differentWorkersNeverCollide() {
		Snowflake a = new Snowflake(1, script(T0));
		Snowflake b = new Snowflake(2, script(T0));
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			ids.addAll(List.of(a.nextId(), b.nextId()));
		}
		assertThat(ids).hasSize(2000);
	}

	@Test
	void validatesTheWorkerId() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Snowflake(-1, System::currentTimeMillis));
		assertThatIllegalArgumentException().isThrownBy(() -> new Snowflake(1024, System::currentTimeMillis));
		new Snowflake(1023, System::currentTimeMillis);
	}

}
