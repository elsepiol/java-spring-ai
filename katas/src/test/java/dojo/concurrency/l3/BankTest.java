package dojo.concurrency.l3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("concurrency")
@Tag("L3")
class BankTest {

	@Test
	void movesMoney() {
		Bank bank = new Bank();
		bank.open(1, 10_000);
		bank.open(2, 0);
		bank.transfer(1, 2, 2_500);
		assertThat(bank.balance(1)).isEqualTo(7_500);
		assertThat(bank.balance(2)).isEqualTo(2_500);
		assertThatThrownBy(() -> bank.transfer(2, 1, 1_000_000)).isInstanceOf(IllegalStateException.class);
		assertThat(bank.balance(2)).as("a refused transfer changes nothing").isEqualTo(2_500);
	}

	/**
	 * The hook makes both threads hold their FIRST lock before either asks for its second: the
	 * exact interleaving that deadlocks. If the locks are ordered, the second thread cannot get
	 * its first lock while the first thread holds it, the barrier times out, and both proceed.
	 */
	@Test
	void twoOppositeTransfersAtTheSameMomentDoNotDeadlock() {
		Bank bank = new Bank();
		bank.open(1, 1_000);
		bank.open(2, 1_000);
		CyclicBarrier bothHoldAFirstLock = new CyclicBarrier(2);
		bank.afterFirstLock = () -> {
			try {
				bothHoldAFirstLock.await(300, TimeUnit.MILLISECONDS);
			} catch (Exception timedOutOrBroken) {
				// fine: it means the other thread could not get here at the same time
			}
		};

		assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
			Thread a = new Thread(() -> bank.transfer(1, 2, 100));
			Thread b = new Thread(() -> bank.transfer(2, 1, 50));
			a.setDaemon(true); // if it deadlocks, do not keep the test JVM alive
			b.setDaemon(true);
			a.start();
			b.start();
			a.join();
			b.join();
		}, "deadlocked: each thread holds one account and waits for the other");
		assertThat(bank.balance(1)).isEqualTo(950);
		assertThat(bank.balance(2)).isEqualTo(1_050);
	}

	@Test
	void manyRandomTransfersKeepTheTotal() {
		Bank bank = new Bank();
		int accounts = 5;
		for (int i = 0; i < accounts; i++) {
			bank.open(i, 1_000_000);
		}
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			List<Thread> threads = new ArrayList<>();
			for (int t = 0; t < 8; t++) {
				Random random = new Random(t);
				Thread thread = new Thread(() -> {
					for (int i = 0; i < 20_000; i++) {
						int from = random.nextInt(accounts);
						int to = (from + 1 + random.nextInt(accounts - 1)) % accounts;
						try {
							bank.transfer(from, to, random.nextInt(100));
						} catch (IllegalStateException insufficient) {
							// allowed
						}
					}
				});
				thread.setDaemon(true);
				thread.start();
				threads.add(thread);
			}
			for (Thread thread : threads) {
				thread.join();
			}
		});
		long total = 0;
		for (int i = 0; i < accounts; i++) {
			total += bank.balance(i);
		}
		assertThat(total).isEqualTo(5_000_000);
	}

}
