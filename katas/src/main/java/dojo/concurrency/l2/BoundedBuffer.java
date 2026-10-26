package dojo.concurrency.l2;

import dojo.Kata;
import java.util.concurrent.TimeUnit;

/**
 * KATA concurrency L2 "BoundedBuffer" + DSA (tracks/02-concurrency/L2-practical.md, section 5).
 *
 * The producer-consumer queue behind every thread pool, every log appender and every Kafka client:
 * a FIXED-size FIFO buffer. Producers block when it is full (backpressure: a fast producer is slowed
 * down instead of eating all memory); consumers block when it is empty.
 *
 * Build it yourself (do not wrap an ArrayBlockingQueue): a circular array (ring buffer) with head
 * and tail indexes, one ReentrantLock, and two Conditions, notFull and notEmpty.
 * Null elements are rejected with NullPointerException.
 */
public class BoundedBuffer<T> {

	public BoundedBuffer(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be at least 1");
		}
	}

	/** Adds, waiting while the buffer is full. */
	public void put(T item) throws InterruptedException {
		Kata.todo("tracks/02-concurrency/L2-practical.md, section 5");
	}

	/** Adds, waiting at most the timeout; false if still full. */
	public boolean offer(T item, long timeout, TimeUnit unit) throws InterruptedException {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 5 (Condition.awaitNanos)");
	}

	/** Removes the oldest item, waiting while the buffer is empty. */
	public T take() throws InterruptedException {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 5");
	}

	public int size() {
		return Kata.todo("tracks/02-concurrency/L2-practical.md, section 5");
	}

}
