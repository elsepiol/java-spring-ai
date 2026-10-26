# Concurrency, L2: the java.util.concurrent toolkit

**Goal:** write concurrent code the way production code does it: tasks on thread pools, futures composed with `CompletableFuture`, the right lock or atomic or concurrent collection for each job, producer-consumer queues with backpressure, and coordination primitives. And know the traps in each.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="concurrency & L2"`. Drill: `java quiz/Quiz.java --track concurrency --level 2`.

---

## 1. Why thread pools

Creating a platform thread costs an OS thread, about 1 MB of reserved stack, and tens of microseconds. A server that starts a thread per request falls over under load: too many threads exhaust memory and spend the CPU switching between them.

A **thread pool** keeps a fixed set of worker threads and a **queue** of tasks. You submit tasks; workers take them from the queue. That gives you:
- reuse (no creation cost per task);
- a **bound on concurrency** (at most N tasks run at once), which protects the database and other services behind you;
- one place to configure names, error handling, and shutdown.

Tomcat (inside Spring Boot) is exactly this: a pool of request threads (200 by default, `server.tomcat.threads.max`) in front of a queue of connections.

---

## 2. `ExecutorService`

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
Future<Report> f = pool.submit(() -> buildReport(month));   // Callable: returns a value, may throw
Report r = f.get(2, TimeUnit.SECONDS);                       // wait for it, with a timeout

pool.shutdown();                                             // no new tasks; running ones finish
if (!pool.awaitTermination(10, TimeUnit.SECONDS)) pool.shutdownNow();   // then interrupt the rest
```

- `submit` returns a `Future`. A task's exception is stored in it and rethrown by `get()` wrapped in `ExecutionException`. If nobody calls `get()`, the failure is **silent**.
- `execute(runnable)` has no Future; an exception goes to the thread's uncaught-exception handler (printed, then forgotten).
- `invokeAll(tasks)` runs them all and waits; `invokeAny` returns the first success.
- **Always shut pools down**, or their non-daemon threads keep the JVM alive. Since Java 19 `ExecutorService` is `AutoCloseable`: `try (var pool = ...) { ... }` waits for the tasks on close.

Run `examples/ExecutorBasics.java`.

**`ThreadPoolExecutor`, the real thing behind the factories:**

```java
new ThreadPoolExecutor(
    10,                                  // corePoolSize: threads kept alive
    50,                                  // maximumPoolSize: used only when the queue is FULL
    60, TimeUnit.SECONDS,                // idle time before extra threads die
    new ArrayBlockingQueue<>(1_000),     // BOUNDED queue
    namedThreadFactory("report-"),       // names show up in thread dumps and logs
    new ThreadPoolExecutor.CallerRunsPolicy());   // what to do when queue and threads are full
```

Order of events for a new task: fewer than core threads → start a thread; otherwise → queue it; queue full → start threads up to max; still full → the **rejection policy** (`AbortPolicy` throws `RejectedExecutionException`, `CallerRunsPolicy` runs it on the submitting thread, which slows the producer down: natural backpressure).

**Traps in the convenient factories:**
- `newFixedThreadPool(n)` uses an **unbounded** queue: under overload tasks pile up in memory until `OutOfMemoryError`, while latency grows without limit.
- `newCachedThreadPool()` creates **unbounded threads**: a traffic spike creates thousands of threads.
- Production pools should have a bounded queue, a rejection policy you chose, named threads, and metrics (Spring's `ThreadPoolTaskExecutor` exposes them via Micrometer).

**Sizing:** CPU-bound work: about the number of cores (`Runtime.getRuntime().availableProcessors()`). I/O-bound work: more, roughly `cores × (1 + wait time / compute time)`. But the real limit is usually what is *behind* the pool: 200 threads in front of a database pool of 10 connections means 190 threads waiting for a connection. (For massively I/O-bound work, virtual threads, L3.)

---

## 3. Locks, atomics and concurrent collections

**`ReentrantLock`**: what `synchronized` does, plus:

```java
private final ReentrantLock lock = new ReentrantLock();

if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {   // give up instead of waiting forever (avoids deadlock)
    try {
        // ...
    } finally {
        lock.unlock();                             // ALWAYS in finally
    }
}
```
`lockInterruptibly()` (a waiting thread can be interrupted), multiple `Condition`s per lock (section 5), optional fairness. Use `synchronized` by default (shorter, cannot forget to unlock); use `ReentrantLock` when you need one of these features.

**`ReadWriteLock`**: many readers at once OR one writer. Helps only when reads dominate and are not tiny. `StampedLock` adds optimistic reads (advanced; not reentrant).

**Atomics and CAS:** `compareAndSet(expected, new)` succeeds only if nobody changed the value in between; otherwise you re-read and retry. Lock-free, but under heavy contention many threads retry in vain. `AtomicReference<State>` with an immutable `State` record lets you update several fields atomically. (The ABA problem: a value changed A→B→A looks unchanged; `AtomicStampedReference` adds a version.)

**Concurrent collections:**

| Instead of | Use | Notes |
|---|---|---|
| `HashMap` shared by threads | `ConcurrentHashMap` | fine-grained locking; reads mostly lock-free; **no null keys or values** |
| `Collections.synchronizedMap` | `ConcurrentHashMap` | one global lock, and iteration still needs external locking |
| `ArrayList` read by many, rarely changed (listeners, config) | `CopyOnWriteArrayList` | every write copies the array |
| a queue between threads | `ArrayBlockingQueue`, `LinkedBlockingQueue` | section 5 |
| a sorted concurrent map | `ConcurrentSkipListMap` | O(log n) |

**The `ConcurrentHashMap` trap: each call is atomic, a sequence of calls is not.**

```java
// BROKEN: two threads both read 5, both write 6
map.put(word, map.getOrDefault(word, 0) + 1);

// CORRECT: one atomic operation per key
map.merge(word, 1, Integer::sum);
map.compute(key, (k, v) -> v == null ? 1 : v + 1);
map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);   // the add is NOT under the map's lock: use a concurrent list if shared
```

Also: `size()` is an estimate while others write; the function given to `compute*` runs while holding a lock on that part of the map, so it must be short and must not touch the same map.

---

## 4. `CompletableFuture`: composing async work

A `Future` can only be waited on. A `CompletableFuture` can be **chained**: "when this is done, do that", without blocking a thread in between.

```java
ExecutorService io = Executors.newFixedThreadPool(20);              // for blocking calls

CompletableFuture<User> user = supplyAsync(() -> userClient.get(id), io);
CompletableFuture<Cart> cart = supplyAsync(() -> cartClient.get(id), io);

CompletableFuture<Page> page = user.thenCombine(cart, Page::new)    // both in parallel, then combine
        .orTimeout(2, TimeUnit.SECONDS)                             // fail with TimeoutException
        .exceptionally(e -> Page.degraded());                       // recover
```

| Method | Meaning |
|---|---|
| `supplyAsync(supplier, executor)` / `runAsync` | start work on that executor |
| `thenApply(f)` | transform the result (like `map`) |
| `thenCompose(f)` | the next step is itself async (like `flatMap`): avoids `CompletableFuture<CompletableFuture<T>>` |
| `thenCombine(other, f)` | wait for both, combine |
| `thenAccept` / `thenRun` | consume the result / run after |
| `allOf(...)` / `anyOf(...)` | wait for all / the first |
| `exceptionally(f)` / `handle((v, e) -> ...)` / `whenComplete` | recover / see both outcomes / side effect |
| `orTimeout(t)` / `completeOnTimeout(value, t)` | fail / substitute a value after a timeout |
| `join()` / `get()` | block for the result (`join` throws unchecked `CompletionException`) |

Rules:
- **Always pass an executor for blocking work.** Without one, `supplyAsync` uses the ForkJoin **common pool**, sized for CPU work and shared by the whole JVM (parallel streams use it too). A few slow HTTP calls can starve everything else.
- **Every remote call gets a timeout.** A future that never completes holds its thread and every caller waiting on it.
- Exceptions travel down the chain wrapped in `CompletionException`; look at `getCause()`.
- `orTimeout` does **not** cancel the underlying work: the HTTP call keeps running in its thread. Give the client its own timeout too.
- Methods without `Async` run on whichever thread completed the previous stage; `thenApplyAsync(f, executor)` chooses the executor explicitly.

Run `examples/CompletableFutureTour.java`.

> **Kata `PriceQuotes`**: ask several shipping providers in parallel, take the cheapest answer within a deadline, ignore failures and late answers; plus a primary-with-fallback call.

---

## 5. Producer-consumer and backpressure

A **producer-consumer** design decouples the thread that creates work from the threads that do it, through a queue: request threads enqueue emails, a few sender threads send them. The queue must be **bounded**: when consumers fall behind, producers must slow down (block, or be rejected) instead of filling memory. That is **backpressure**, and it is how a system degrades gracefully under overload instead of crashing.

`BlockingQueue` gives you this: `put` blocks when full, `take` blocks when empty, `offer`/`poll` with timeouts. `ArrayBlockingQueue` (a fixed array, one lock) or `LinkedBlockingQueue` (pass a capacity! the default is unbounded).

How it works inside (the kata): a **ring buffer** (array + head/tail indexes that wrap around) guarded by one lock, with two **condition variables**, `notFull` and `notEmpty`:

```java
lock.lock();
try {
    while (count == items.length) notFull.await();   // releases the lock while waiting
    // insert...
    notEmpty.signal();                                // wake one waiting consumer
} finally { lock.unlock(); }
```

- **Always wait in a `while` loop**, never an `if`: wake-ups can be spurious, and another thread may have taken the slot before you got the lock back.
- `await` (and `Object.wait`) **releases the lock** while waiting; `sleep` does not.
- `Object.wait/notify/notifyAll` are the older form of the same thing (one condition per object; `notifyAll` is safer than `notify` with mixed waiters). Prefer `java.util.concurrent` classes in real code.

> **Kata `BoundedBuffer`**: build the ring buffer with a lock and two conditions, then prove with 8 threads that every item is delivered exactly once.

---

## 6. Coordination primitives and rate limiting

| Tool | What it does | Typical use |
|---|---|---|
| `CountDownLatch(n)` | `await` blocks until `countDown` was called n times; one-shot | wait for N services to start; a start gate in tests |
| `CyclicBarrier(n)` | n threads wait for each other, then all continue; reusable | phases of a parallel algorithm; forcing interleavings in tests (the `Bank` kata) |
| `Semaphore(n)` | at most n permits held at once | **bulkhead**: at most 10 concurrent calls to a fragile service |
| `Phaser`, `Exchanger` | more flexible barriers, pair handoff | rare |

**Rate limiting** limits *how often*, not *how many at once* (that is a semaphore). The **token bucket** algorithm: a bucket of `capacity` tokens refilled at a steady rate; each request takes a token; an empty bucket means "429 Too Many Requests". It allows short bursts but caps the long-run rate. Implementation detail that matters: compute the refill lazily from the elapsed time on each call (no background thread), take time from an injectable clock (testable), and make the read-refill-take sequence atomic (it is a check-then-act).

Alternatives you should be able to compare: **fixed window** counters (simple, but allow 2x the limit across a window boundary), **sliding window log** (exact, memory per request), **sliding window counter** (approximation), **leaky bucket** (smooths output to a constant rate).

> **Kata `TokenBucket`**: thread-safe, testable with a fake clock, and exact under 16 threads.

---

## 7. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `BoundedBuffer` | `ReentrantLock`, `Condition`, blocking, interruption | ring buffer |
| `PriceQuotes` | `CompletableFuture`, executors, timeouts, fallbacks | |
| `TokenBucket` | atomic check-then-act, injectable clock | token bucket rate limiter |

```
cd katas && ./mvnw test -Dgroups="concurrency & L2"
```

## 8. Seen in the shop project

- `ratelimit/TokenBucket.java` + `RateLimiter.java`: a synchronized token bucket per client, stored in a bounded Caffeine cache, with `Retry-After` computed from the bucket.
- `outbox/OutboxWorker.java`: a producer-consumer design where the "queue" is a database table, which survives restarts, unlike an in-memory queue.
- `application.properties`: Hikari's connection pool is the bound behind Tomcat's request threads (Level 4: pool sizing).

## 9. Interview checklist

1. Why use a thread pool? What happens with one thread per request under load?
2. Walk through what `ThreadPoolExecutor` does with a new task (core, queue, max, rejection).
3. What is wrong with `Executors.newFixedThreadPool` and `newCachedThreadPool` in production?
4. How do you size a pool for CPU-bound vs I/O-bound work?
5. `submit` vs `execute`: what happens to exceptions?
6. `synchronized` vs `ReentrantLock`: when do you need the latter?
7. Why is `map.put(k, map.get(k) + 1)` broken on a `ConcurrentHashMap`, and what is the fix?
8. `thenApply` vs `thenCompose`. Why pass an executor to `supplyAsync`?
9. Why must a producer-consumer queue be bounded? What is backpressure?
10. Why must `wait`/`await` be in a `while` loop?
11. `CountDownLatch` vs `CyclicBarrier` vs `Semaphore`.
12. Explain the token bucket algorithm and compare it with a fixed window counter.
