package dojo.web.l2;

import dojo.Kata;
import java.util.function.Supplier;

/**
 * KATA web L2 "IdempotencyStore" (tracks/05-web-http/L2-practical.md, section 4).
 *
 * POST /orders is not idempotent: a retry after a timeout can create a second order. With an
 * Idempotency-Key header, the client marks "this is the same attempt"; the server must then:
 * - first time for a key: run the action, store its response, return it;
 * - same key, same request (same fingerprint, e.g. a hash of the body): return the STORED
 *   response without running the action again, marked replayed (header Idempotent-Replayed: true);
 * - same key, DIFFERENT request: 422 Unprocessable Content (the client has a bug);
 * - same key while the first request is still running: 409 Conflict (try again shortly);
 *   do not block waiting for it;
 * - if the action throws, or its response is a 5xx, nothing is stored: a retry may run it again.
 * Thread-safe: requests with the same key arrive concurrently. (In production this lives in the
 * database, like the shop's idempotency_key column; here, in memory.)
 */
public final class IdempotencyStore {

	public record Response(int status, String body, boolean replayed) {
	}

	public Response handle(String key, String fingerprint, Supplier<Response> action) {
		return Kata.todo("tracks/05-web-http/L2-practical.md, section 4");
	}

}
