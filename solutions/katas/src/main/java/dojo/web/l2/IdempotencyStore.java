package dojo.web.l2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Reference solution: one entry per key, claimed atomically with putIfAbsent. */
public final class IdempotencyStore {

	public record Response(int status, String body, boolean replayed) {
	}

	private record Entry(String fingerprint, Response response) { // response == null: still running
	}

	private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

	public Response handle(String key, String fingerprint, Supplier<Response> action) {
		Entry claim = new Entry(fingerprint, null);
		Entry existing = entries.putIfAbsent(key, claim);
		if (existing != null) {
			if (!existing.fingerprint().equals(fingerprint)) {
				return new Response(422, "Idempotency-Key reused for a different request", false);
			}
			if (existing.response() == null) {
				return new Response(409, "A request with this Idempotency-Key is in progress", false);
			}
			Response stored = existing.response();
			return new Response(stored.status(), stored.body(), true);
		}
		Response response;
		try {
			response = action.get();
		} catch (RuntimeException e) {
			entries.remove(key, claim); // not stored: a retry may try again
			throw e;
		}
		if (response.status() >= 500) {
			entries.remove(key, claim);
		} else {
			entries.replace(key, claim, new Entry(fingerprint, response));
		}
		return response;
	}

}
