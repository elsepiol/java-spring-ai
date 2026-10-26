# Concurrency, L3: concurrency in production

**Goal:** reason precisely about memory visibility (the Java Memory Model), prevent and diagnose deadlocks, protect shared resources under load (stampedes, saturation, backpressure), use virtual threads correctly, and know where in-JVM concurrency ends and distributed coordination begins. This is the level senior interviews and real incidents live at.

Prerequisite: L1, L2. Katas: `cd katas && ./mvnw test -Dgroups="concurrency & L3"`. Drill: `java quiz/Quiz.java --track concurrency --level 3`.

---

## 1. The Java Memory Model in one page

Compilers, the JIT and CPUs reorder reads and writes and keep values in registers and caches, as long as a *single* thread cannot tell. Another thread can tell. The Java Memory Model (JMM) defines when a write by one thread is guaranteed to be visible to a read in another: when the write **happens-before** the read.

Happens-before edges you need to know:
1. **Program order**: within one thread, each action happens-before the next.
2. **Monitor lock**: unlocking a lock happens-before every later lock of the *same* lock.
3. **Volatile**: a write to a volatile field happens-before every later read of that field.
4. **Thread start**: `t.start()` happens-before anything in `t`.
5. **Thread termination**: everything in `t` happens-before another thread returns from `t.join()`.
6. **Concurrent library hand-offs**: putting an object into a `BlockingQueue`/`ConcurrentHashMap`, completing a `Future`, `countDown` on a latch: happens-before the consumer's corresponding read.
7. **Transitivity**: if A hb B and B hb C, then A hb C.
8. **Final fields**: after a constructor finishes, other threads that get the reference (even through a data race) see the final fields' values as set in the constructor (as long as `this` did not escape during construction).

A program in which every conflicting access is ordered by happens-before is **data-race free**, and then it behaves as if all operations were interleaved in some order (sequential consistency). Write code at that level: use locks, volatiles, and concurrent classes; do not reason about "which CPU flushes when".

---

## 2. Safe publication and lazy initialisation

**Publishing** an object = making it reachable by other threads. **Unsafe publication**: another thread sees the reference but not (all of) the writes inside the constructor, e.g. through a plain field set by one thread and read by another.

Safe ways to publish: store it in a `volatile` field or an `AtomicReference`; store it under a lock that readers also take; put it in a concurrent collection; initialise it in a static initialiser; or make the object immutable with final fields.

**Double-checked locking**, the classic interview question:

```java
private volatile Client client;            // without volatile: BROKEN

Client client() {
    Client c = client;                     // 1st check, no lock
    if (c == null) {
        synchronized (this) {
            c = client;
            if (c == null) {               // 2nd check, under the lock
                client = c = new Client(); // volatile write publishes the fully built object
            }
        }
    }
    return c;
}
```

Without `volatile`, the write of the reference may become visible before the writes inside `Client`'s constructor; a second thread passes the first check and uses a half-built object. For static singletons, the **holder idiom** is simpler and lazy for free:

```java
class Clients {
    private static class Holder { static final Client INSTANCE = new Client(); }   // initialised on first access
    static Client get() { return Holder.INSTANCE; }                               // class init is thread-safe
}
```

In Spring you rarely need either: singleton beans are created once at startup, before requests arrive, and published safely by the container.

> **Kata `Lazy`**: double-checked locking, exactly once under 64 threads, retry on failure.

---

## 3. Deadlock, livelock, starvation

A **deadlock** needs all four (Coffman) conditions:
1. **Mutual exclusion**: resources are held exclusively (locks).
2. **Hold and wait**: a thread holds one lock while waiting for another.
3. **No preemption**: locks cannot be taken away.
4. **Circular wait**: T1 waits for T2's lock, T2 waits for T1's.

Break any one and deadlock is impossible. In practice:
- **Global lock ordering** (breaks circular wait): always acquire locks in the same order, e.g. by account id. The standard fix for "transfer between two accounts".
- **`tryLock` with timeout** (breaks hold-and-wait): back off, release what you hold, retry with jitter. Can cause livelock if everyone retries in step.
- **Open calls**: never call unknown code (listeners, callbacks, other services) while holding a lock.
- **One lock instead of two** when contention allows; or no locks (immutable data, single-writer designs, an atomic database statement).

**Diagnosing:** `jcmd <pid> Thread.print` (or `jstack`) ends with "Found one Java-level deadlock" and shows which thread holds which lock and waits for which. Symptoms in production: requests to some endpoints hang forever, the request thread pool fills up, then the whole service stops answering while CPU is near zero. Run `examples/Deadlock.java`.

Related failures: **livelock** (threads keep reacting to each other and make no progress, like two people stepping aside in a corridor), **starvation** (a thread never gets the lock or CPU, e.g. low priority or an unfair lock under constant contention).

Databases deadlock too (two transactions updating the same rows in opposite order); PostgreSQL detects it and aborts one transaction, so the application must retry. Same fix: touch rows in a consistent order.

> **Kata `Bank`**: a transfer that deadlocks when two customers pay each other at the same moment. The test forces that interleaving with a barrier.

---

## 4. Contention and scalability

- **Amdahl's law:** if 10% of the work is serialised (under one lock), the speed-up can never exceed 10x, however many cores you add. Find and shrink serial sections.
- **Lock granularity:** one lock for the whole map serialises everything; `ConcurrentHashMap` locks per bin. Smaller lock scopes, or **lock striping** (N locks, pick by hash), raise parallelism.
- **Hold locks briefly:** compute outside the lock, then lock only to publish the result. Never do I/O under a lock.
- **Contended atomics** spin and retry; `LongAdder` spreads updates over cells.
- **False sharing:** two hot variables on the same 64-byte cache line ping-pong between cores even though threads touch different variables. The JDK pads its hot fields (`@Contended` internally); you rarely need to, but it explains why `LongAdder`'s cells are padded.
- **Measure** with JFR's lock profiling ("Java Monitor Blocked" events) and thread dumps showing many threads `BLOCKED` on the same monitor.

---

## 5. Caching under concurrency: stampedes and single-flight

A popular cache entry expires; 500 requests miss at the same moment and all run the expensive query (a **cache stampede** or thundering herd). The database, already the bottleneck, receives 500x the load at the worst moment.

Defences:
- **Single-flight (request coalescing):** the first caller computes; concurrent callers for the same key wait for that one result. Implement with a map of *futures*, not values (Goetz's `Memoizer`): `putIfAbsent(key, future)`; the winner computes, the others `join()` the future. Remove failed futures so errors are not cached.
- `ConcurrentHashMap.computeIfAbsent` is also single-flight, but it holds a lock on the bin while computing: a slow remote call blocks unrelated keys in the same bin, and the function must not modify the same map. Fine for fast, local computations.
- **Caffeine** does single-flight in `cache.get(key, loader)`, plus `refreshAfterWrite` (serve the stale value while one thread refreshes in the background) and jittered expiry.
- Across many instances, each instance still computes once: N instances = N loads, often acceptable; otherwise a distributed lock or a shared cache with its own coalescing.

> **Kata `Memoizer`**: single-flight per key, parallel across keys, failures not cached.

---

## 6. Virtual threads (Java 21)

A **platform thread** is an OS thread: expensive, a few thousand at most. A **virtual thread** is a lightweight thread managed by the JVM: it runs on a small pool of carrier (platform) threads, and when it **blocks** on I/O, a lock, `sleep` or a queue, the JVM unmounts it and runs another virtual thread on that carrier. Millions can exist.

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Price>> prices = shops.stream().map(s -> executor.submit(() -> client.price(s))).toList();
    ...
}   // close() waits for all of them
// Spring Boot 3.2+: spring.threads.virtual.enabled=true runs request handling on virtual threads
```

What they change:
- **Thread-per-request scales again** for I/O-bound services: blocking code (JDBC, `RestClient`) with the throughput that previously needed reactive code. Run `examples/VirtualThreads.java`: 10,000 blocking tasks in ~0.2 s vs ~5 s on 200 platform threads.
- **They do not make CPU-bound work faster.** There are still only N cores.
- **Do not pool them.** They are cheap; create one per task. Limit concurrency against a resource with a `Semaphore` (or the resource's own pool), not with a small pool of threads.
- **Your bottleneck moves.** 10,000 concurrent requests now reach the database pool of 10 connections at once: size and time out the pools, add bulkheads.
- **Pinning:** in Java 21-23, a virtual thread that blocks *inside a `synchronized` block* (or a native call) cannot unmount and pins its carrier; enough of them stall the whole scheduler. Prefer `ReentrantLock` around blocking calls on those versions. (Java 24 removed the `synchronized` limitation.)
- **ThreadLocals** work, but with millions of threads, large per-thread caches in ThreadLocals multiply memory. `ScopedValue` is the newer alternative.
- **Structured concurrency** (`StructuredTaskScope`, preview) treats a group of subtasks as one unit: if one fails, the others are cancelled, and none outlives the scope.

> **Kata `FanOut`**: 10,000 calls in about a second, results in task order, fail fast and cancel the rest, overall timeout.

---

## 7. `ThreadLocal` and context propagation

`ThreadLocal<T>` gives each thread its own value: the current user, a transaction, a logging context (SLF4J's MDC), a non-thread-safe formatter. Spring uses it for `SecurityContextHolder` and `@Transactional`.

Two production traps:
- **Pooled threads keep values.** A value set during one request and not removed is still there for the next request on the same thread: a **data leak between users** and a memory leak. Always `remove()` in `finally`. Run `examples/ThreadLocalLeak.java`.
- **Async boundaries lose it.** Work handed to another thread (an executor, `CompletableFuture`, `@Async`) does not see the caller's ThreadLocals: logs lose their trace id, security context is empty. Propagate explicitly (capture and restore, Micrometer context propagation, Spring's `TaskDecorator`).

---

## 8. Thread pools in production

- **Bound everything:** threads, queues, connection pools, in-flight requests. Unbounded anything becomes an outage under a traffic spike.
- **Timeouts on every remote call** (connect and read), shorter than the caller's own timeout. A hung dependency otherwise holds threads until the pool is exhausted, and the failure spreads upstream (cascading failure).
- **Bulkheads:** separate pools (or semaphores) per dependency, so a slow payment service cannot use up the threads that serve product pages.
- **Rejection is a feature:** failing fast with 503 and `Retry-After` under overload beats accepting everything and timing out everything.
- **Monitor:** active threads, queue size, rejected tasks, task latency (Micrometer binds `ThreadPoolTaskExecutor` and Tomcat's pool).
- **Graceful shutdown:** stop accepting work, let in-flight tasks finish within a deadline, then interrupt (`shutdown` → `awaitTermination` → `shutdownNow`). Spring Boot does this for its own executors with `server.shutdown=graceful`.
- **Name your threads.** `pool-7-thread-3` in a thread dump tells you nothing; `payment-client-3` does.

---

## 9. Beyond one JVM

`synchronized`, `ReentrantLock` and `AtomicLong` protect memory **inside one process**. A service runs as several instances (pods); a lock in instance 1 does nothing for a request on instance 2. For shared state:
- Let the **database** do it: one atomic statement (`UPDATE inventory SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?`), row locks (`SELECT ... FOR UPDATE`), or **optimistic locking** (a `version` column; the update fails if someone else changed the row; retry). See the SQL and JPA tracks.
- **Idempotency keys** make retries of the same request safe across instances (shop Level 3).
- **Unique constraints** turn "check then insert" races into a clean error.
- **Distributed locks** (Redis, ZooKeeper, database advisory locks) exist, but a lock holder can pause (GC, network partition) past its lease while another holder proceeds; correctness then needs fencing tokens. Prefer designs that do not need them.
- **Per-instance in-memory state** (rate-limit buckets, caches) is approximate across instances: N instances allow N × the limit. Use a shared store if exactness matters.

---

## 10. Katas for this level

| Kata | Practises |
|---|---|
| `Bank` | deadlock, lock ordering, testing an interleaving with a barrier |
| `Memoizer` | single-flight, futures in a concurrent map, not caching failures |
| `FanOut` | virtual threads, CompletionService, fail-fast cancellation, timeouts |
| `Lazy` | JMM, volatile, double-checked locking |

```
cd katas && ./mvnw test -Dgroups="concurrency & L3"
```

## 11. Seen in the shop project

- Database-level concurrency control instead of JVM locks: `InventoryRepository` (atomic conditional update), `Product.version` (optimistic locking, Level 4), idempotency keys with a unique constraint (`OrderService`, Level 3).
- Approximate per-instance limits, documented as such: `ratelimit/RateLimiter.java` (Level 5 lesson discusses N instances).
- Bounded pools with timeouts: Hikari `maximum-pool-size` and `connection-timeout` in `application.properties`.

## 12. Interview talk track

"Inside one JVM I make shared state either immutable, confined to one thread, or guarded: a lock for compound invariants, atomics or LongAdder for counters, concurrent collections with their atomic compute and merge operations, never get-then-put. Visibility follows the happens-before rules, so flags are volatile and lazily created singletons use the holder idiom or volatile double-checked locking. I avoid deadlocks with a global lock order and by never calling out while holding a lock, and diagnose them from thread dumps. In production everything is bounded and has a timeout: thread pools with bounded queues and a rejection policy, connection pools, per-dependency bulkheads, and timeouts shorter than the caller's. Hot cache keys get single-flight loading to prevent stampedes. For I/O-heavy services I'd use virtual threads in Java 21, keeping in mind pinning in synchronized blocks and that the bottleneck moves to the connection pools. And for correctness across instances I rely on the database: atomic conditional updates, optimistic locking, unique constraints and idempotency keys, because a JVM lock means nothing to the other pods."

**Questions you should be able to answer:**
1. What is happens-before? List five rules that create it.
2. What is a data race, and why can it show values that were "never written" in that order?
3. Why is double-checked locking broken without `volatile`? What is the holder idiom?
4. What makes an object safely published?
5. The four conditions for deadlock; how lock ordering breaks one of them.
6. How do you find a deadlock in a running service? What are the symptoms?
7. Deadlock vs livelock vs starvation.
8. What is a cache stampede, and how does single-flight prevent it? Why is a map of futures better than `computeIfAbsent` for slow loads?
9. What are virtual threads? When do they help, when not? What is pinning?
10. Why should you not pool virtual threads? How do you limit concurrency instead?
11. How do ThreadLocals leak data between requests? How do you propagate context to async code?
12. How do you keep one slow dependency from taking down the whole service?
13. Why does `synchronized` not prevent overselling when the service runs on three pods? What do you use instead?
