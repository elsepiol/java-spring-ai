package dojo.concurrency.l3;

import dojo.Kata;
import java.util.function.Supplier;

/**
 * KATA concurrency L3 "Lazy" (tracks/02-concurrency/L3-production.md, section 2: safe publication).
 *
 * A value created on first use, at most once, even when 64 threads ask at the same instant:
 * an expensive client, a parsed configuration. Requirements:
 * - the supplier runs at most once (unless it throws: then the exception propagates and the next
 *   get() tries again);
 * - every thread sees the fully constructed value;
 * - after the value exists, get() takes no lock (hint: double-checked locking needs ONE keyword
 *   to be correct; the lesson explains which and why).
 */
public final class Lazy<T> {

	public Lazy(Supplier<? extends T> supplier) {
	}

	public T get() {
		return Kata.todo("tracks/02-concurrency/L3-production.md, section 2");
	}

	public boolean isInitialized() {
		return Kata.todo("tracks/02-concurrency/L3-production.md, section 2");
	}

}
