package dojo.javacore.l2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reference solution. The map finds a node in O(1); the list keeps nodes in recency order, with
 * the most recent right after `head` and the eviction candidate right before `tail`. Two sentinel
 * nodes mean no null checks when unlinking. Not thread-safe (see the concurrency track).
 */
public class LruCache<K, V> {

	private static final class Node<K, V> {
		K key;
		V value;
		Node<K, V> prev;
		Node<K, V> next;
	}

	private final int capacity;
	private final Map<K, Node<K, V>> index = new HashMap<>();
	private final Node<K, V> head = new Node<>();
	private final Node<K, V> tail = new Node<>();

	public LruCache(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be at least 1");
		}
		this.capacity = capacity;
		head.next = tail;
		tail.prev = head;
	}

	public Optional<V> get(K key) {
		Node<K, V> node = index.get(key);
		if (node == null) {
			return Optional.empty();
		}
		unlink(node);
		linkFirst(node);
		return Optional.ofNullable(node.value);
	}

	public void put(K key, V value) {
		Node<K, V> node = index.get(key);
		if (node != null) {
			node.value = value;
			unlink(node);
			linkFirst(node);
			return;
		}
		if (index.size() == capacity) {
			Node<K, V> eldest = tail.prev;
			unlink(eldest);
			index.remove(eldest.key);
		}
		node = new Node<>();
		node.key = key;
		node.value = value;
		linkFirst(node);
		index.put(key, node);
	}

	public int size() {
		return index.size();
	}

	private void unlink(Node<K, V> node) {
		node.prev.next = node.next;
		node.next.prev = node.prev;
	}

	private void linkFirst(Node<K, V> node) {
		node.next = head.next;
		node.prev = head;
		head.next.prev = node;
		head.next = node;
	}

	public static <K, V> Map<K, V> withLinkedHashMap(int capacity) {
		// accessOrder = true: get() moves an entry to the end; the eldest is the least recently used.
		return new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > capacity;
			}
		};
	}

}
