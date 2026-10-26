package dojo.javacore.l3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reference solution. The bug: the key was MUTATED while inside the HashMap. The entry stays in
 * the bucket chosen by the OLD hash code, so lookups with the new key search the wrong bucket and
 * lookups with the old key fail equals(): the entry can never be found or removed again. A leak.
 *
 * The fix: keys are immutable (a record), and "renaming" a key is remove + put with a new key.
 */
public class ActiveCarts {

	/** Immutable: equals and hashCode can never change while the key is in a map. */
	public record CartKey(long userId, String sessionId) {
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

	public void renameSession(String oldSessionId, String newSessionId) {
		CartKey oldKey = keyBySession.remove(oldSessionId);
		if (oldKey == null) {
			return;
		}
		Cart cart = carts.remove(oldKey);
		CartKey newKey = new CartKey(oldKey.userId(), newSessionId);
		keyBySession.put(newSessionId, newKey);
		if (cart != null) {
			carts.put(newKey, cart);
		}
	}

	public void close(long userId, String sessionId) {
		carts.remove(new CartKey(userId, sessionId));
		keyBySession.remove(sessionId);
	}

	public int size() {
		return carts.size();
	}

}
