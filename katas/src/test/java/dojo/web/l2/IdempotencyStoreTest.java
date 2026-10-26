package dojo.web.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.web.l2.IdempotencyStore.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("web")
@Tag("L2")
class IdempotencyStoreTest {

	private final IdempotencyStore store = new IdempotencyStore();
	private final AtomicInteger ordersCreated = new AtomicInteger();

	private Response placeOrder() {
		return new Response(201, "{\"orderId\":" + ordersCreated.incrementAndGet() + "}", false);
	}

	@Test
	void aRetryReplaysTheFirstResponse() {
		Response first = store.handle("k1", "hash-a", this::placeOrder);
		Response retry = store.handle("k1", "hash-a", this::placeOrder);
		assertThat(first).isEqualTo(new Response(201, "{\"orderId\":1}", false));
		assertThat(retry).isEqualTo(new Response(201, "{\"orderId\":1}", true));
		assertThat(ordersCreated).hasValue(1);
	}

	@Test
	void differentKeysAreDifferentAttempts() {
		store.handle("k1", "hash-a", this::placeOrder);
		store.handle("k2", "hash-a", this::placeOrder);
		assertThat(ordersCreated).hasValue(2);
	}

	@Test
	void reusingAKeyForADifferentRequestIs422() {
		store.handle("k1", "hash-a", this::placeOrder);
		assertThat(store.handle("k1", "hash-b", this::placeOrder).status()).isEqualTo(422);
		assertThat(ordersCreated).hasValue(1);
	}

	@Test
	void aConcurrentDuplicateGets409WithoutWaiting() throws Exception {
		CountDownLatch actionStarted = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CompletableFuture<Response> slow = CompletableFuture.supplyAsync(() -> store.handle("k1", "hash-a", () -> {
			actionStarted.countDown();
			try {
				release.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return placeOrder();
		}));
		assertThat(actionStarted.await(5, TimeUnit.SECONDS)).isTrue();

		long start = System.nanoTime();
		Response duplicate = store.handle("k1", "hash-a", this::placeOrder);
		assertThat(duplicate.status()).isEqualTo(409);
		assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1000);

		release.countDown();
		assertThat(slow.get(5, TimeUnit.SECONDS).status()).isEqualTo(201);
		assertThat(store.handle("k1", "hash-a", this::placeOrder).replayed()).isTrue();
		assertThat(ordersCreated).hasValue(1);
	}

	@Test
	void failuresAreNotStored() {
		assertThatThrownBy(() -> store.handle("k1", "hash-a", () -> {
			throw new IllegalStateException("database down");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(store.handle("k1", "hash-a", () -> new Response(503, "busy", false)).status()).isEqualTo(503);
		Response success = store.handle("k1", "hash-a", this::placeOrder);
		assertThat(success.status()).isEqualTo(201);
		assertThat(success.replayed()).isFalse();
	}

	@Test
	void clientErrorsAreStoredLikeSuccesses() {
		// a 4xx is a final answer for this request: replaying it is correct
		store.handle("k1", "hash-a", () -> new Response(409, "out of stock", false));
		assertThat(store.handle("k1", "hash-a", this::placeOrder)).isEqualTo(new Response(409, "out of stock", true));
		assertThat(ordersCreated).hasValue(0);
	}

}
