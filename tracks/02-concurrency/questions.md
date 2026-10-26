# Concurrency: question bank

Drill with `java quiz/Quiz.java --track concurrency` (add `--level 2`, `--kind bug,scenario`, ...).

## L1

### CONC-L1-001 · recall
What do threads of the same process share, and what does each thread have for itself?

<details><summary>Answer</summary>

Shared: the **heap** (all objects, static fields), loaded classes, open files and sockets. Private: the **stack** (frames with local variables and parameters), the program counter, and ThreadLocal values. So local variables are thread-safe; fields of shared objects are not.

</details>

### CONC-L1-002 · predict
What is printed, and on which thread does the lambda run in each case?

```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()), "worker");
t.run();
t.start();
```

<details><summary>Answer</summary>

`main` then `worker`. `run()` is an ordinary method call on the current thread; only `start()` creates a new thread that executes `run()`. Calling `start()` a second time would throw `IllegalThreadStateException`.

</details>

### CONC-L1-003 · why
Why can `count++` lose updates when two threads call it? Draw the interleaving.

<details><summary>Answer</summary>

`count++` is read, add, write. A reads 41; B reads 41; A writes 42; B writes 42. Two increments, one effect. Nothing makes the three steps atomic. Fix with `synchronized`, `AtomicLong.incrementAndGet()`, or `LongAdder.increment()`.

</details>

### CONC-L1-004 · bug
This class locks its writes. Is it thread-safe?

```java
class Stats {
    private long total;
    synchronized void add(long x) { total += x; }
    long total() { return total; }
}
```

<details><summary>Answer</summary>

No. `total()` reads without the lock, so it may see a **stale** value (no happens-before with the writer), and on some 32-bit JVMs a `long` read can even be torn (half old, half new). Every access to shared mutable state must use the same lock: make `total()` synchronized too, or use an `AtomicLong`/`LongAdder`.

</details>

### CONC-L1-005 · recall
What does `volatile` guarantee, and what does it not guarantee?

<details><summary>Answer</summary>

Guarantees: **visibility** (a read sees the latest write to that field) and **ordering** (writes before a volatile write are visible to a thread after it reads that volatile; no reordering across it). Does **not** guarantee atomicity of compound actions: `volatileCounter++` still loses updates. Use it for flags and for publishing immutable or fully built objects.

</details>

### CONC-L1-006 · scenario
A background thread loops `while (!stopped) { ... }`. Another thread sets `stopped = true`, but the loop never ends in production, though it does in the debugger. Why?

<details><summary>Answer</summary>

`stopped` is not `volatile` and nothing else synchronises the threads, so the JIT may read the field once and hoist it out of the loop (or the value stays in a register/cache). In the debugger, code runs interpreted and the read happens each time. Fix: `volatile boolean stopped`, or use interruption (`while (!Thread.currentThread().isInterrupted())`).

</details>

### CONC-L1-007 · recall
`synchronized` vs `AtomicLong` vs `LongAdder` for a counter: how does each work, and when do you pick which?

<details><summary>Answer</summary>

- `synchronized`: a monitor lock; other threads block. Use when the counter is part of a larger invariant updated together.
- `AtomicLong`: lock-free compare-and-swap, retried on conflict. Good general-purpose counter and needed for `compareAndSet`.
- `LongAdder`: per-thread cells summed on read; far less contention when many threads increment (metrics). `sum()` is only exact when no increments are in flight, and there is no CAS.

</details>

### CONC-L1-008 · bug
What is wrong with this catch block?

```java
while (true) {
    try {
        Message m = queue.take();
        handle(m);
    } catch (InterruptedException e) {
        log.warn("interrupted", e);
    }
}
```

<details><summary>Answer</summary>

It **swallows the interrupt**: catching `InterruptedException` clears the thread's interrupt flag, and the loop carries on. Whoever wanted the thread to stop (executor `shutdownNow`, application shutdown) is ignored and shutdown hangs. Restore and leave: `Thread.currentThread().interrupt(); return;` (or rethrow).

</details>

### CONC-L1-009 · recall
How do you stop a thread in Java? Why is there no `kill`?

<details><summary>Answer</summary>

Cooperatively: set a volatile flag the thread checks, and/or call `thread.interrupt()`, which sets its interrupt flag and makes blocking calls (`sleep`, `wait`, `join`, `take`, `lockInterruptibly`) throw `InterruptedException`. The thread then cleans up and exits. Forcibly stopping a thread (`Thread.stop`, now removed) released its locks at an arbitrary point, leaving shared objects half-updated.

</details>

### CONC-L1-010 · recall
Name the thread states you see in a thread dump and what puts a thread in each.

<details><summary>Answer</summary>

`NEW` (created, not started), `RUNNABLE` (running or ready, also during most native I/O), `BLOCKED` (waiting to enter a `synchronized` block held by another thread), `WAITING` (`wait()`, `join()`, `LockSupport.park()` without timeout, e.g. waiting on a `ReentrantLock` or a queue), `TIMED_WAITING` (`sleep`, waits with timeouts), `TERMINATED`.

</details>

### CONC-L1-011 · scenario
Your Spring `@Service` has a field `private Order lastOrder;` that a method sets and then reads during the request. What goes wrong in production?

<details><summary>Answer</summary>

The service is a **singleton** shared by all request threads. Two concurrent requests overwrite each other's `lastOrder`; request A can read request B's order (a data leak between users, plus visibility issues). Never keep per-request state in fields of singleton beans; use local variables and parameters, or a request-scoped object.

</details>

### CONC-L1-012 · why
Why does `ParallelSum` (each thread writes its own slot of a `long[]`, then `join`) need no locks?

<details><summary>Answer</summary>

No two threads write the same memory location (thread confinement of each slot), so there is no race between writers. And `join()` creates a happens-before edge: everything a worker wrote is visible to the main thread after `join` returns. Partitioning work so threads do not share mutable data is the most scalable form of concurrency.

</details>

### CONC-L1-013 · recall
What is a check-then-act race? Give two backend examples.

<details><summary>Answer</summary>

A decision based on a check that is no longer true when acting, because another thread changed state in between. Examples: `if (stock > 0) stock--` (two buyers both see 1: oversold); `if (!usersByEmail.containsKey(email)) usersByEmail.put(email, user)` (duplicate registration); "if no order with this idempotency key exists, create one". Fix by making check and act one atomic operation (lock, `putIfAbsent`, a conditional SQL update, a unique constraint).

</details>

### CONC-L1-014 · predict
Two threads each run `for (int i = 0; i < 1000; i++) list.add(i);` on the same `ArrayList`. What can happen?

<details><summary>Answer</summary>

Anything from correct-looking results to **lost elements** (size < 2000), `null` entries, or `ArrayIndexOutOfBoundsException` during an internal resize, because `ArrayList` is not thread-safe (size update and array writes race). Use a concurrent or synchronized collection, or give each thread its own list and merge them.

</details>

### CONC-L1-015 · why
`sleep` vs `wait`: which releases the lock? Why does that matter?

<details><summary>Answer</summary>

`Thread.sleep` keeps every lock the thread holds; `obj.wait()` must be called while holding `obj`'s monitor and **releases it** while waiting (re-acquiring it before returning). If you sleep inside a `synchronized` block, every other thread needing that lock stalls for the whole sleep.

</details>

### CONC-L1-016 · recall
What is the difference between concurrency and parallelism?

<details><summary>Answer</summary>

**Concurrency**: dealing with many tasks in progress at once (they may interleave on one core). **Parallelism**: executing several tasks at the same instant on multiple cores. A web server is concurrent (many requests in flight); it is parallel when it has several cores to run them simultaneously.

</details>

### CONC-L1-017 · design
How do you write a unit test that reliably provokes a race condition instead of passing by luck?

<details><summary>Answer</summary>

Maximise overlap: many threads, many iterations, and a **start gate** (`CountDownLatch(1)`) so all threads begin at the same moment; assert an invariant afterwards (count == threads × iterations). For a specific interleaving, add a test hook (a barrier) inside the code under test to force threads to the critical point together, as in the `Bank` kata. Use timeouts so a deadlock fails the test instead of hanging it. Tools: jcstress for low-level JMM tests.

</details>

### CONC-L1-018 · recall
What happens to an exception thrown inside a thread you started with `new Thread(...).start()`?

<details><summary>Answer</summary>

It terminates **only that thread**. It goes to the thread's `UncaughtExceptionHandler` (by default printed to stderr) and the rest of the program never learns about it. In background workers that is how errors disappear: catch and log inside the loop, set a handler, or run tasks on an executor and check their `Future`s.

</details>

### CONC-L1-019 · recall
What is a reentrant lock, and is `synchronized` reentrant?

<details><summary>Answer</summary>

A thread already holding the lock can acquire it again without blocking (the lock counts holds and is released when the count returns to zero). `synchronized` is reentrant, as is `ReentrantLock`. Without reentrancy, a synchronized method calling another synchronized method on the same object would deadlock on itself.

</details>

### CONC-L1-020 · why
Why is an immutable object thread-safe without any locks?

<details><summary>Answer</summary>

Races need a write to shared state. After construction an immutable object is never written, so concurrent reads cannot interfere. With final fields, the JMM also guarantees other threads see the constructed values once they have the reference. Records with immutable components are the easiest way to get this.

</details>

## L2

### CONC-L2-001 · recall
Walk through what a `ThreadPoolExecutor` does with a newly submitted task.

<details><summary>Answer</summary>

1. Fewer than `corePoolSize` threads running → start a new thread for it.
2. Otherwise → offer it to the **queue**.
3. Queue full → start a new thread if below `maximumPoolSize`.
4. Still impossible → the **RejectedExecutionHandler** (Abort throws, CallerRuns runs it on the submitter, Discard drops, DiscardOldest drops the oldest queued).

Consequence: with an unbounded queue, the pool never grows past core size and never rejects.

</details>

### CONC-L2-002 · bug
Why is this risky in a production service?

```java
private final ExecutorService pool = Executors.newFixedThreadPool(10);
// every request: pool.submit(() -> sendEmail(order));
```

<details><summary>Answer</summary>

`newFixedThreadPool` uses an **unbounded** `LinkedBlockingQueue`. If the mail server is slow, tasks pile up in memory without limit (growing latency, then `OutOfMemoryError`), and they are all lost on restart. Also unnamed threads, no metrics, no shutdown. Use a bounded queue with a rejection policy (or `CallerRunsPolicy` for backpressure), name the threads, shut it down with the application, and for must-not-lose work use a persistent queue (an outbox table, a message broker).

</details>

### CONC-L2-003 · recall
`submit` vs `execute`: what happens to an exception thrown by the task?

<details><summary>Answer</summary>

`submit` captures it in the returned `Future`; it is rethrown, wrapped in `ExecutionException`, by `future.get()`. If nobody calls `get()`, it is **silently lost**. `execute` lets it propagate to the worker thread's uncaught-exception handler (printed by default), and the pool replaces the dead thread.

</details>

### CONC-L2-004 · interview
How do you size a thread pool?

<details><summary>Answer</summary>

CPU-bound: about the number of cores (maybe +1). I/O-bound: more, roughly `cores × (1 + wait/compute)`; e.g. tasks that wait 90 ms and compute 10 ms on 8 cores → ~80 threads. Then check what is behind the pool: there is no point in 200 threads hitting a database pool of 10 connections. Validate with a load test, watching latency, queue length and the downstream pools. For heavily blocking work, consider virtual threads plus a semaphore for the real limit.

</details>

### CONC-L2-005 · bug
Thread-safe word counting? Fix it.

```java
ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
void count(String w) {
    Integer c = counts.get(w);
    counts.put(w, c == null ? 1 : c + 1);
}
```

<details><summary>Answer</summary>

Each call is atomic, but **get then put is not**: two threads read the same count and both write count+1, losing an update. Use one atomic operation: `counts.merge(w, 1, Integer::sum)` (or `compute`). For high-contention counters: `ConcurrentHashMap<String, LongAdder>` with `computeIfAbsent(w, k -> new LongAdder()).increment()`.

</details>

### CONC-L2-006 · recall
Why does `ConcurrentHashMap` not allow `null` keys or values?

<details><summary>Answer</summary>

Ambiguity under concurrency: if `get(k)` returned `null`, you could not tell "absent" from "mapped to null", and you cannot check `containsKey` first because another thread may change the map in between. Disallowing nulls makes `get` returning null mean absent, always. (`HashMap` allows nulls because single-threaded code can check.)

</details>

### CONC-L2-007 · recall
`thenApply` vs `thenCompose` vs `thenCombine`.

<details><summary>Answer</summary>

`thenApply(f)`: transform the result with a plain function (`T -> U`), like `map`. `thenCompose(f)`: the function itself returns a `CompletableFuture` (`T -> CompletableFuture<U>`), flattened, like `flatMap`, for chaining dependent async calls. `thenCombine(other, f)`: wait for two independent futures and combine both results.

</details>

### CONC-L2-008 · why
Why should you pass your own executor to `CompletableFuture.supplyAsync` for HTTP or database calls?

<details><summary>Answer</summary>

Without one it uses the ForkJoin **common pool**, sized to (cores - 1) threads and shared by the entire JVM, including parallel streams and other libraries. Blocking calls tie up its few threads, and everything else using it starves. Use a dedicated, bounded executor (or virtual threads) for blocking I/O.

</details>

### CONC-L2-009 · scenario
A page aggregates data from three services with `CompletableFuture`s and `join()`. One service starts hanging. What happens to your service, and how should the code look?

<details><summary>Answer</summary>

`join()` waits forever; each request holds a thread; the request pool fills up and **your** service stops answering: a cascading failure. Fix: a timeout on each call (`orTimeout`, `completeOnTimeout` with a fallback, plus the HTTP client's own connect/read timeouts, since `orTimeout` does not cancel the underlying call), a fallback or degraded response, a bulkhead (separate bounded executor or semaphore per dependency), and ideally a circuit breaker so the failing service is not called at all for a while.

</details>

### CONC-L2-010 · recall
What does `CompletableFuture.orTimeout` do, and what does it not do?

<details><summary>Answer</summary>

It completes the future exceptionally with `TimeoutException` if it is not done in time, so callers stop waiting. It does **not** stop the underlying work: the thread running the supplier keeps going (an HTTP call keeps its connection). Configure timeouts in the client too, and use `cancel(true)` on futures you created from an executor if the task is interruptible.

</details>

### CONC-L2-011 · why
Why must a producer-consumer queue be bounded, and what is backpressure?

<details><summary>Answer</summary>

If producers are faster than consumers (always, during a spike or a slow downstream), an unbounded queue grows until the process runs out of memory, and latency grows with it. A bounded queue makes producers block, slow down or get rejected when it is full: **backpressure**, the signal "slow down" travelling back to the source. The system degrades predictably instead of collapsing.

</details>

### CONC-L2-012 · bug
Why is `if` wrong here, and what can happen?

```java
synchronized T take() throws InterruptedException {
    if (queue.isEmpty()) wait();
    return queue.remove();
}
```

<details><summary>Answer</summary>

Wake-ups can be **spurious**, and with several consumers another thread may take the item between `notifyAll` and this thread re-acquiring the lock. After waking, the queue may still be empty and `remove()` throws `NoSuchElementException`. Always re-check the condition in a loop: `while (queue.isEmpty()) wait();`.

</details>

### CONC-L2-013 · recall
`CountDownLatch` vs `CyclicBarrier` vs `Semaphore`: one use case each.

<details><summary>Answer</summary>

- `CountDownLatch(n)`: one-shot; threads wait until n events happened. Wait for 3 caches to warm up before accepting traffic; a start gate in tests.
- `CyclicBarrier(n)`: n threads wait for each other, then all continue; reusable. Phased parallel computations; forcing an interleaving in a test.
- `Semaphore(n)`: at most n holders at a time. A bulkhead: max 10 concurrent calls to a fragile service.

</details>

### CONC-L2-014 · recall
`synchronized` vs `ReentrantLock`: what can `ReentrantLock` do that `synchronized` cannot?

<details><summary>Answer</summary>

`tryLock()` and `tryLock(timeout)` (give up instead of blocking forever), `lockInterruptibly()` (waiting can be interrupted), multiple `Condition`s per lock, optional fairness, and querying the lock state. Costs: you must `unlock()` in `finally` yourself. Use `synchronized` by default, `ReentrantLock` when you need one of these.

</details>

### CONC-L2-015 · dsa
Explain the token bucket algorithm, and how you implement it without a background thread.

<details><summary>Answer</summary>

A bucket holds up to `capacity` tokens and starts full; each request takes one, and is rejected if none is left; tokens are added at `rate` per second, up to capacity. So bursts up to capacity pass while the long-run rate is capped. No timer needed: store `tokens` and `lastRefill`; on each call add `(now - lastRefill) × rate` (capped), update `lastRefill`, then try to take. Keep fractional tokens. Make refill-check-take atomic (a lock or CAS on an immutable state). Time from an injectable clock for tests.

</details>

### CONC-L2-016 · interview
Compare fixed window, sliding window and token bucket rate limiting.

<details><summary>Answer</summary>

- **Fixed window** (N per calendar minute): one counter, trivial; but a client can send N at 12:00:59 and N at 12:01:00, 2N in two seconds.
- **Sliding window log**: store each request's timestamp, count those in the last minute; exact, but memory per request.
- **Sliding window counter**: weight the previous window's count by the overlap; good approximation, two counters.
- **Token bucket**: allows controlled bursts, smooth refill, O(1) state; the most common for APIs. (**Leaky bucket** instead smooths the *output* to a constant rate.)

</details>

### CONC-L2-017 · recall
What is compare-and-swap, and what is the ABA problem?

<details><summary>Answer</summary>

CAS atomically sets a variable to a new value **only if** it currently holds the expected value, reporting success; lock-free algorithms read, compute, CAS, and retry on failure. ABA: a value changes A → B → A between your read and your CAS, so the CAS succeeds although the state changed (dangerous in lock-free linked structures). Fix: a version stamp with the value (`AtomicStampedReference`), or immutable nodes plus GC.

</details>

### CONC-L2-018 · scenario
After a release, a service sometimes throws `RejectedExecutionException`. What does it mean, and what are your options?

<details><summary>Answer</summary>

The executor's queue is full and it has reached max threads (or it was shut down). Work arrives faster than it is done. Options: find why tasks are slower (a downstream got slow? a new blocking call?), check sizing; decide the overload behaviour deliberately: reject and return 503 with `Retry-After` (fail fast), `CallerRunsPolicy` (backpressure onto the caller), or a larger bounded queue (more latency). Do not just make the queue unbounded. Add metrics on queue size and rejections.

</details>

### CONC-L2-019 · why
Why is `Collections.synchronizedMap` worse than `ConcurrentHashMap` under load, and what trap does it have for iteration?

<details><summary>Answer</summary>

It wraps every method in one lock on the whole map, so all threads serialise, readers included. `ConcurrentHashMap` locks per bin for writes and reads mostly lock-free. Trap: iterating a synchronized map is not atomic; you must hold `synchronized (map) { for (...) }` yourself, or risk `ConcurrentModificationException`. `ConcurrentHashMap` iterators are weakly consistent and never throw it.

</details>

### CONC-L2-020 · recall
When is `CopyOnWriteArrayList` a good choice, and when a bad one?

<details><summary>Answer</summary>

Good: read-mostly lists that many threads iterate and that rarely change: event listeners, configuration, routing tables. Iterators see a snapshot and never throw `ConcurrentModificationException`. Bad: lists written often or large, because **every write copies the whole array**.

</details>

## L3

### CONC-L3-001 · recall
What is the happens-before relation? List five rules that create it.

<details><summary>Answer</summary>

If action A happens-before B, then B is guaranteed to see A's effects (and A's preceding writes). Rules: program order within a thread; unlock → later lock of the same monitor; volatile write → later read of that variable; `Thread.start()` → actions of the started thread; a thread's actions → another thread's return from `join()`; hand-offs via concurrent collections, futures, latches; and transitivity.

</details>

### CONC-L3-002 · why
Why is double-checked locking broken without `volatile`?

<details><summary>Answer</summary>

`instance = new Client()` is: allocate, run the constructor, write the reference. Without a happens-before edge, the reference write can become visible to another thread **before** the constructor's writes. That thread passes the unsynchronised first check (`instance != null`) and uses a partially constructed object. `volatile` on the field orders the constructor's writes before the reference publication.

</details>

### CONC-L3-003 · recall
Name four ways to safely publish an object to other threads.

<details><summary>Answer</summary>

(1) Initialise it in a static initialiser (class init is thread-safe). (2) Store its reference in a `volatile` field or an `AtomicReference`. (3) Store it in a field guarded by a lock that readers also take. (4) Put it into a thread-safe collection or hand it over via a `BlockingQueue`/`Future`. Plus: immutable objects with final fields are visible correctly even when published through a race.

</details>

### CONC-L3-004 · recall
What are the four conditions for deadlock? How does lock ordering prevent it?

<details><summary>Answer</summary>

Mutual exclusion, hold and wait, no preemption, circular wait. All four are needed. A global lock order (e.g. always lock the lower account id first) makes a **cycle** of waiting impossible: a thread holding a higher lock never waits for a lower one, so no thread can wait for a lock held by someone waiting on it.

</details>

### CONC-L3-005 · scenario
A service suddenly stops answering some endpoints. CPU is near 0%, the Tomcat thread pool is exhausted. How do you diagnose it?

<details><summary>Answer</summary>

Low CPU + exhausted threads = threads **waiting**, not working. Take 2-3 thread dumps a few seconds apart (`jcmd <pid> Thread.print`). Look for: "Found one Java-level deadlock" (lock cycle); many threads `BLOCKED` on the same monitor (one slow lock holder); many threads `WAITING`/`TIMED_WAITING` in the same frame, e.g. in a socket read to one dependency (missing timeout) or `HikariPool.getConnection` (pool exhausted by slow queries or a leak). Fix the cause; add timeouts and bulkheads so one dependency cannot hold all threads.

</details>

### CONC-L3-006 · recall
Deadlock vs livelock vs starvation.

<details><summary>Answer</summary>

**Deadlock**: threads block forever waiting for each other; no progress, no CPU. **Livelock**: threads keep running and reacting to each other (e.g. all back off and retry in sync) but make no progress; CPU is busy. **Starvation**: some thread never gets the resource (lock, CPU) because others keep getting it first; the system progresses, that thread does not. Random jitter fixes livelock; fair locks or queueing fix starvation.

</details>

### CONC-L3-007 · design
Design a cache-loading component that prevents a cache stampede. Explain the data structure.

<details><summary>Answer</summary>

A `ConcurrentHashMap<K, CompletableFuture<V>>`. `get(k)`: look up the future; if absent, create one and `putIfAbsent`. The winner runs the load **outside any lock** and completes the future; losers find the existing future and `join()` it. So each key loads once, different keys load in parallel. On failure: remove the future (so the error is not cached) and complete it exceptionally so current waiters see the error. Add expiry (or refresh-ahead serving the stale value), and metrics. Caffeine's `get(key, loader)` and `refreshAfterWrite` provide this.

</details>

### CONC-L3-008 · why
Why can `ConcurrentHashMap.computeIfAbsent` be a poor choice for loading values from a remote service?

<details><summary>Answer</summary>

The mapping function runs **while holding the lock for that bin**: other keys hashing to the same bin block for the whole remote call; long calls reduce the map's parallelism; and the function must not modify the same map (it can throw `IllegalStateException` for recursive updates or deadlock). It is single-flight, which is good, but only for short, local computations. Use a map of futures or Caffeine for slow loads.

</details>

### CONC-L3-009 · recall
What is a virtual thread, and how does it differ from a platform thread?

<details><summary>Answer</summary>

A platform thread is a thin wrapper over an OS thread: ~1 MB stack reserved, expensive to create, thousands at most. A virtual thread (Java 21) is scheduled by the JVM on a small pool of carrier threads; when it blocks on I/O, `sleep`, locks or queues, it is **unmounted** and its stack saved on the heap, freeing the carrier. Millions can exist; creation is cheap. Same `Thread` API, so blocking code works unchanged.

</details>

### CONC-L3-010 · interview
When do virtual threads help, and when do they not? Name two pitfalls.

<details><summary>Answer</summary>

Help: many concurrent **I/O-bound** tasks (HTTP calls, JDBC, queues), thread-per-request servers with lots of waiting; they replace reactive code for throughput. Do not help: CPU-bound work (still N cores), or when the bottleneck is a downstream pool. Pitfalls: **pinning** (in Java 21-23, blocking inside `synchronized` or native code keeps the carrier busy; use `ReentrantLock`); **pooling them** (pointless; limit concurrency with a `Semaphore`); unlimited concurrency overwhelming the database pool; large ThreadLocal caches multiplied by millions of threads.

</details>

### CONC-L3-011 · scenario
You switch a Spring Boot service to virtual threads (`spring.threads.virtual.enabled=true`). Throughput goes up, then the database starts timing out under load. Why?

<details><summary>Answer</summary>

Before, Tomcat's 200 platform threads limited how many requests ran at once, which implicitly limited database concurrency. With virtual threads, thousands of requests run concurrently and all compete for the Hikari pool (say 10 connections); they wait on `getConnection`, hit `connection-timeout`, or overload the database. The bottleneck moved. Fix: explicit limits (a semaphore or rate limiter in front of DB-heavy paths), correctly sized pool and timeouts, and load-test.

</details>

### CONC-L3-012 · bug
What goes wrong in a pooled server?

```java
static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

void handle(Request r) {
    CURRENT.set(authenticate(r));
    process(r);
}
```

<details><summary>Answer</summary>

`CURRENT` is never cleared. Tomcat reuses threads, so the next request handled by this thread (maybe anonymous, or one where `authenticate` throws) sees the **previous user**: a security bug and a memory leak (the User object stays referenced by the pooled thread). Always `try { set; process } finally { CURRENT.remove(); }`.

</details>

### CONC-L3-013 · scenario
Logs from code inside a `CompletableFuture` or `@Async` method lack the trace id and the current user. Why, and how do you fix it?

<details><summary>Answer</summary>

The trace id (MDC) and security context live in **ThreadLocals of the request thread**; the async work runs on another thread where they are empty. Fix: propagate context explicitly: capture before submitting and restore in the task (and clear after), use Spring's `TaskDecorator` on the executor, Micrometer's context-propagation library, or `DelegatingSecurityContextExecutor`.

</details>

### CONC-L3-014 · why
Why does `synchronized` not prevent overselling stock when the service runs as three pods? What do you use instead?

<details><summary>Answer</summary>

A JVM lock only coordinates threads **in that JVM**; requests on another pod never see it. The shared state lives in the database, so coordinate there: an atomic conditional update (`UPDATE ... SET qty = qty - ? WHERE id = ? AND qty >= ?`, check rows affected), `SELECT ... FOR UPDATE` row locks, or optimistic locking with a version column and retry; plus unique constraints and idempotency keys for "exactly once" effects.

</details>

### CONC-L3-015 · recall
What is Amdahl's law, and what does it tell you about adding cores?

<details><summary>Answer</summary>

Speed-up ≤ 1 / (s + (1 - s)/N), where s is the fraction of work that is serial and N the number of processors. With 5% serial work, even infinite cores give at most 20x. Scalability is limited by serialised sections (global locks, a single queue, one database row everyone updates), so shrink those rather than adding hardware.

</details>

### CONC-L3-016 · design
How do you stop one slow downstream service from taking your whole service down?

<details><summary>Answer</summary>

- Timeouts (connect + read) on every call, shorter than your callers' timeouts.
- **Bulkhead**: a separate bounded pool or semaphore per dependency, so it can only consume its share of threads.
- **Circuit breaker**: after repeated failures, fail fast for a cooldown instead of calling (Resilience4j).
- Fallbacks or degraded responses (cached data, defaults).
- Bounded retries with exponential backoff and jitter, and only for idempotent operations.
- Load shedding: reject early (503 + Retry-After) when saturated. Monitor pool saturation and latency per dependency.

</details>

### CONC-L3-017 · recall
What is false sharing?

<details><summary>Answer</summary>

Two threads frequently write **different** variables that happen to sit on the **same CPU cache line** (typically 64 bytes). Each write invalidates the line in the other core's cache, so the cores keep bouncing it back and forth, slowing both as if they shared data. Fix: pad or separate hot fields (`@Contended` inside the JDK, `LongAdder`'s padded cells).

</details>

### CONC-L3-018 · why
Why are distributed locks (e.g. in Redis) hard to get right?

<details><summary>Answer</summary>

A lock holder can be paused (GC pause, VM freeze, network partition) longer than the lock's lease; the lock expires, another process acquires it, and now **two** processes believe they hold it. Correctness then needs **fencing tokens** (a monotonically increasing number checked by the protected resource) and careful clock/lease handling. Prefer designs that need no distributed lock: database constraints, conditional updates, single-writer partitioning, idempotency.

</details>

### CONC-L3-019 · recall
What does graceful shutdown of an executor look like, and why does it matter during a Kubernetes rolling deploy?

<details><summary>Answer</summary>

`shutdown()` (stop accepting tasks), `awaitTermination(deadline)` (let running tasks finish), then `shutdownNow()` (interrupt the rest) and log what was abandoned. In a rolling deploy, pods receive SIGTERM and have a grace period (30 s default) before SIGKILL; without graceful shutdown in-flight requests and background tasks are cut off mid-way (half-processed work, failed requests). Spring Boot's `server.shutdown=graceful` drains web requests; your own executors must be closed too (Spring calls `destroy` on executor beans).

</details>

### CONC-L3-020 · interview
What is structured concurrency, and what problem does it solve?

<details><summary>Answer</summary>

Treating concurrent subtasks like a block of code: they are started inside a scope and **cannot outlive it**. If one fails, the scope cancels the others; the parent waits for all to finish before continuing; errors and cancellation propagate predictably; thread dumps show the parent/child relationship. It solves leaked threads, forgotten futures and "one subtask failed but the others keep running". In Java it is `StructuredTaskScope` (a preview API at Java 21); the `FanOut` kata implements the same idea by hand.

</details>
