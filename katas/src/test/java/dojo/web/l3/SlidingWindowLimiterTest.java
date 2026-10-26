package dojo.web.l3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L3")
class SlidingWindowLimiterTest {

	private final AtomicLong now = new AtomicLong();
	private final SlidingWindowLimiter limiter = new SlidingWindowLimiter(100, Duration.ofSeconds(60), now::get);

	private void at(double seconds) {
		now.set((long) (seconds * TimeUnit.SECONDS.toNanos(1)));
	}

	private int burst(String client, int n) {
		int allowed = 0;
		for (int i = 0; i < n; i++) {
			if (limiter.tryAcquire(client)) {
				allowed++;
			}
		}
		return allowed;
	}

	@Test
	void allowsUpToTheLimitInOneWindow() {
		at(10);
		assertThat(burst("ann", 150)).isEqualTo(100);
	}

	@Test
	void theBoundaryBurstOfAFixedWindowIsPrevented() {
		at(59.9);
		assertThat(burst("ann", 100)).isEqualTo(100);
		at(60.1); // a new fixed window, but 99.8% of the previous one still counts
		assertThat(burst("ann", 100)).as("a fixed window would allow 100 more").isLessThanOrEqualTo(1);
	}

	@Test
	void capacityReturnsGraduallyAsTheWindowSlides() {
		at(30);
		burst("ann", 80);
		at(75); // 15 s into the next window: 80 * 45/60 = 60 still count
		assertThat(burst("ann", 100)).isEqualTo(40);
	}

	@Test
	void afterTwoQuietWindowsEverythingIsForgotten() {
		at(10);
		burst("ann", 100);
		at(130);
		assertThat(burst("ann", 100)).isEqualTo(100);
	}

	@Test
	void clientsAreIndependent() {
		at(10);
		burst("ann", 100);
		assertThat(burst("bob", 5)).isEqualTo(5);
	}

}
