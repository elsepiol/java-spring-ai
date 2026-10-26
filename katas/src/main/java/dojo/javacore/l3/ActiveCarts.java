package dojo.javacore.l3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * KATA javacore L3 "ActiveCarts": a production memory leak (tracks/01-java-core/L3-production.md, section 4).
 *
 * Shopping carts held in memory, keyed by (userId, sessionId). When a user logs in again their
 * session id changes, and renameSession() moves the cart to the new session.
 *
 * In production, the heap grows until the JVM dies with OutOfMemoryError, a few days after each
 * deploy. Heap dumps show millions of Cart objects that nothing should still reference. The tests
 * reproduce it. Find the cause and fix it. Rules: keep the public methods; you may change CartKey.
 */
public class ActiveCarts {

	/** The key. Look closely at what can change after it is put into the map. */
	public static final class CartKey {

		private final long userId;
		private String sessionId;

		public CartKey(long userId, String sessionId) {
			this.userId = userId;
			this.sessionId = sessionId;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof CartKey k && userId == k.userId && sessionId.equals(k.sessionId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, sessionId);
		}

	}

	public static final class Cart {
		final List<String> items = new ArrayList<>();
	}

	private final Map<CartKey, Cart> carts = new HashMap<>();
	private final Map<String, CartKey> keyBySession = new HashMap<>();

	public Cart open(long userId, String sessionId) {
		CartKey key = new CartKey(userId, sessionId);
		keyBySession.put(sessionId, key);
		return carts.computeIfAbsent(key, k -> new Cart());
	}

	public Optional<Cart> find(long userId, String sessionId) {
		return Optional.ofNullable(carts.get(new CartKey(userId, sessionId)));
	}

	/** The user logged in again: same cart, new session id. */
	public void renameSession(String oldSessionId, String newSessionId) {
		CartKey key = keyBySession.remove(oldSessionId);
		if (key != null) {
			key.setSessionId(newSessionId);
			keyBySession.put(newSessionId, key);
		}
	}

	/** Checkout or logout: the cart is gone. */
	public void close(long userId, String sessionId) {
		carts.remove(new CartKey(userId, sessionId));
		keyBySession.remove(sessionId);
	}

	public int size() {
		return carts.size();
	}

}
