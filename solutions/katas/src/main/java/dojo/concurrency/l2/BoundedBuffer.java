package dojo.concurrency.l2;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Reference solution: essentially how java.util.concurrent.ArrayBlockingQueue works. */
public class BoundedBuffer<T> {

	private final Object[] items;
	private int head; // next slot to take from
	private int tail; // next slot to put into
	private int count;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition notFull = lock.newCondition();
	private final Condition notEmpty = lock.newCondition();

	public BoundedBuffer(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be at least 1");
		}
		items = new Object[capacity];
	}

	public void put(T item) throws InterruptedException {
		Objects.requireNonNull(item);
		lock.lockInterruptibly();
		try {
			while (count == items.length) { // WHILE, not if: wake-ups can be spurious, or another producer won
				notFull.await();
			}
			enqueue(item);
		} finally {
			lock.unlock();
		}
	}

	public boolean offer(T item, long timeout, TimeUnit unit) throws InterruptedException {
		Objects.requireNonNull(item);
		long nanos = unit.toNanos(timeout);
		lock.lockInterruptibly();
		try {
			while (count == items.length) {
				if (nanos <= 0) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos); // returns the time still left
			}
			enqueue(item);
			return true;
		} finally {
			lock.unlock();
		}
	}

	public T take() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			while (count == 0) {
				notEmpty.await();
			}
			@SuppressWarnings("unchecked")
			T item = (T) items[head];
			items[head] = null; // let the GC have it
			head = (head + 1) % items.length;
			count--;
			notFull.signal(); // one slot freed: wake one producer
			return item;
		} finally {
			lock.unlock();
		}
	}

	public int size() {
		lock.lock();
		try {
			return count;
		} finally {
			lock.unlock();
		}
	}

	private void enqueue(T item) {
		items[tail] = item;
		tail = (tail + 1) % items.length;
		count++;
		notEmpty.signal(); // one item added: wake one consumer
	}

}
