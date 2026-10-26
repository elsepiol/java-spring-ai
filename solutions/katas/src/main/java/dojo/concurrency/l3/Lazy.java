package dojo.concurrency.l3;

import java.util.function.Supplier;

/**
 * Reference solution: double-checked locking. Without VOLATILE it is broken: the write of the
 * reference can become visible to another thread before the writes inside the constructor, so
 * that thread sees a non-null but half-built object. volatile forbids that reordering
 * (a volatile write happens-before every later read of it).
 * For a static singleton, the "holder class" idiom is simpler: the JVM's class initialisation
 * already guarantees exactly-once and safe publication.
 */
public final class Lazy<T> {

	private Supplier<? extends T> supplier; // guarded by this; dropped after use so it can be collected
	private volatile T value;

	public Lazy(Supplier<? extends T> supplier) {
		this.supplier = supplier;
	}

	public T get() {
		T v = value; // one volatile read on the fast path
		if (v == null) {
			synchronized (this) {
				v = value;
				if (v == null) { // second check: another thread may have created it while we waited
					v = supplier.get();
					value = v;
					supplier = null;
				}
			}
		}
		return v;
	}

	public boolean isInitialized() {
		return value != null;
	}

}
