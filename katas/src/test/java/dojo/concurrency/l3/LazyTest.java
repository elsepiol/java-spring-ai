package dojo.concurrency.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L3")
class LazyTest {

	static final class ExpensiveClient {
		final String config;

		ExpensiveClient(String config) {
			this.config = config;
		}
	}

	@Test
	void createsNothingUntilAsked() {
		AtomicInteger created = new AtomicInteger();
		Lazy<String> lazy = new Lazy<>(() -> "v" + created.incrementAndGet());
		assertThat(lazy.isInitialized()).isFalse();
		assertThat(created).hasValue(0);
		assertThat(lazy.get()).isEqualTo("v1");
		assertThat(lazy.get()).isEqualTo("v1");
		assertThat(lazy.isInitialized()).isTrue();
	}

	@Test
	void sixtyFourThreadsAtOnceCreateItOnce() throws Exception {
		AtomicInteger created = new AtomicInteger();
		Lazy<ExpensiveClient> lazy = new Lazy<>(() -> {
			created.incrementAndGet();
			try {
				Thread.sleep(50); // slow construction widens the race window
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return new ExpensiveClient("pool=10");
		});
		CountDownLatch gate = new CountDownLatch(1);
		Set<ExpensiveClient> seen = ConcurrentHashMap.newKeySet();
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			Thread t = new Thread(() -> {
				try {
					gate.await();
				} catch (InterruptedException e) {
					return;
				}
				ExpensiveClient c = lazy.get();
				assertThat(c.config).isEqualTo("pool=10");
				seen.add(c);
			});
			t.start();
			threads.add(t);
		}
		gate.countDown();
		for (Thread t : threads) {
			t.join();
		}
		assertThat(created).hasValue(1);
		assertThat(seen).hasSize(1);
	}

	@Test
	void aFailedCreationIsRetried() {
		AtomicInteger attempts = new AtomicInteger();
		Lazy<String> lazy = new Lazy<>(() -> {
			if (attempts.incrementAndGet() == 1) {
				throw new IllegalStateException("config server down");
			}
			return "ok";
		});
		assertThatThrownBy(lazy::get).hasMessage("config server down");
		assertThat(lazy.isInitialized()).isFalse();
		assertThat(lazy.get()).isEqualTo("ok");
	}

}
