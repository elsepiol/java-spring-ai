# Concurrency, L1: threads, races and the first fixes

**Goal:** understand why code that is correct with one thread breaks with two, and fix the three basic problems: lost updates (atomicity), stale reads (visibility), and threads that will not stop (interruption).

**Why a backend developer must know this:** a Spring Boot service runs every HTTP request on its own thread, all at the same time, against the same singleton beans. Any field in a `@Service` that changes is shared by every request. Most "it fails once a week in production" bugs are concurrency bugs.

Prerequisite: Java core L1. Katas: `cd katas && ./mvnw test -Dgroups="concurrency & L1"`. Drill: `java quiz/Quiz.java --track concurrency --level 1`.

---

## 1. Processes, threads, cores

- A **process** is a running program with its own memory (the JVM is one process).
- A **thread** is a path of execution *inside* a process. All threads of a process **share the heap** (objects, static fields); each has its own **stack** (local variables).
- The CPU has a few **cores** (4, 8, 64). The OS scheduler gives threads turns on the cores, switching between them thousands of times per second. With more runnable threads than cores, they take turns; the switch points are unpredictable.
- **Concurrency** = several tasks in progress at once (interleaved). **Parallelism** = several tasks executing at the same instant on different cores. A web server is concurrent by nature; it is parallel when it has several cores.

Shared heap + unpredictable switching = every non-trivial interaction between threads must be designed.

---

## 2. Creating and joining threads

```java
Thread worker = new Thread(() -> System.out.println("hello from " + Thread.currentThread().getName()), "worker-1");
worker.start();   // a NEW thread runs the lambda. Calling worker.run() would run it on THIS thread
worker.join();    // wait until it has finished
```

- `start()` may be called once. `run()` called directly is just a method call: a classic interview trick question.
- `join()` waits for the thread to finish, and (important, section 5) makes everything that thread wrote **visible** to the joining thread.
- Thread states: `NEW` → `RUNNABLE` (running or ready) → `BLOCKED` (waiting for a lock) / `WAITING` / `TIMED_WAITING` (in `join`, `wait`, `sleep`, `park`) → `TERMINATED`. You will read these in thread dumps (L3).
- An uncaught exception ends that thread only; the rest of the program carries on. That is why exceptions in background threads are easily lost.
- **Daemon** threads do not keep the JVM alive (`setDaemon(true)` before `start`).
- In real code you rarely create threads by hand: you submit tasks to a pool (L2). L1 uses raw threads so you can see what pools do for you.

> **Kata `ParallelSum`**: split an array into slices, sum each slice on its own thread into its own slot, join, add up. No locks needed because no two threads write the same memory.

---

## 3. The race condition

```java
class Counter {
    private long count;
    void increment() { count++; }   // looks like one step
}
```

`count++` is three steps: **read** count, **add** 1, **write** it back. Two threads can interleave:

```
Thread A: read 41                         write 42
Thread B:          read 41      write 42
Result: 42. One increment is LOST.
```

Run `examples/RaceCondition.java`: 8 threads × 1,000,000 increments lose tens of thousands. The bug:
- does not show in unit tests (one thread);
- appears under load, on machines with more cores, and differently every run;
- causes oversold stock, double-spent balances, wrong counters.

This is a **check-then-act** or **read-modify-write** race. Other examples: `if (!map.containsKey(k)) map.put(k, v)` (two threads both see "absent"); `if (stock > 0) stock--` (both see 1, stock goes to -1). The shop project fought the database version of exactly this in Level 3.

A **data race** is the specific case where two threads access the same variable, at least one writes, and nothing orders the accesses. In Java, a data race does not only lose updates: it can also show stale or reordered values (section 5).

---

## 4. Making operations atomic

**Atomic** = the operation happens all at once from the point of view of other threads. Three tools at L1:

**1. `synchronized` (a lock).** Every object has a monitor lock. Only one thread can hold it; others **block** until it is released.

```java
class Counter {
    private long count;                              // guarded by "this"
    synchronized void increment() { count++; }       // locks this object for the whole method
    synchronized long get() { return count; }        // reads must lock too (section 5)
}
// Or a block: synchronized (lock) { ... }  with a private final Object lock = new Object();
```

- The lock is released when the block exits, also on exceptions.
- It is **reentrant**: a thread holding a lock can enter other `synchronized` code on the same lock.
- Every access to the shared variable must use **the same lock**. Locking in `increment` but not in `get` is still broken.
- Keep the locked section short; never call slow I/O or unknown code while holding a lock.

**2. Atomic classes (compare-and-swap).** `AtomicLong`, `AtomicInteger`, `AtomicReference` use a CPU instruction that updates a value only if it still holds what you expected (CAS). No blocking; on conflict, retry.

```java
AtomicLong count = new AtomicLong();
count.incrementAndGet();
count.updateAndGet(x -> Math.max(x, observed));   // any pure function, retried on conflict
```

**3. `LongAdder`.** For counters hit by many threads (metrics, request counts): each thread increments its own cell and `sum()` adds them. Much less contention than one `AtomicLong`; but `sum()` is only exact when increments have stopped, and there is no `compareAndSet`.

**Or: do not share.** The best fix is often to have no shared mutable state: confine data to one thread (`ParallelSum`'s per-thread slots, local variables), or make it immutable (records). A Spring `@Service` with only `final` dependencies and no mutable fields is thread-safe by construction.

> **Kata `Counters`**: three correct counters, three tools.

---

## 5. Visibility: when a thread never sees a write

Each core has caches, and both the JIT compiler and the CPU reorder and cache memory operations for speed. Without synchronisation, Java does **not** guarantee that a write by one thread is ever seen by another.

```java
static boolean running = true;
// worker:  while (running) { work++; }   // the JIT may read `running` once and loop forever
// main:    running = false;              // the worker may NEVER see this
```

Run `examples/Visibility.java`: on most JVMs the plain worker spins forever. The fix is a **`volatile`** field:
- every read of a volatile goes to main memory and sees the latest write;
- writes made **before** a volatile write are visible to a thread that reads the volatile afterwards (it is a "happens-before" edge, L3).

`volatile` gives visibility and ordering, **not atomicity**: `volatileCount++` is still a lost-update race. Use it for flags and for publishing a reference to an object that is fully built (and not changed afterwards).

Other things that make writes visible (all are happens-before edges): releasing and later acquiring the same lock; `Thread.start()` (the new thread sees what the starter did before); `Thread.join()` (the joiner sees everything the finished thread did); handing an object over through a concurrent collection or a `Future`.

---

## 6. Stopping a thread properly: interruption

Java has no safe way to kill a thread (`Thread.stop` is removed). Threads stop **cooperatively**:
- **A volatile flag** checked in the loop: works for busy loops.
- **Interruption** for threads that are blocked: `thread.interrupt()` sets the thread's interrupt flag, and methods that block (`sleep`, `wait`, `join`, `BlockingQueue.take`, `lockInterruptibly`) wake up by throwing `InterruptedException`.

The rule every code review checks: **never swallow `InterruptedException`.**

```java
try {
    queue.take();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // restore the flag: catching it CLEARED it
    return;                               // and stop what you are doing (or throw)
}
```

If you catch it and carry on, whoever asked the thread to stop (an executor's `shutdownNow`, Spring during shutdown) is ignored, and the application hangs on exit.

- `Thread.sleep` in production code is a smell (polling, waiting for something): prefer blocking on a queue, a latch or a future with a timeout.
- `sleep` does not release locks; `wait` does (L2).

> **Kata `GracefulWorker`**: a background loop that stops within a second when asked, even if it is sleeping, and survives failing steps. The shop's outbox poller has the same shape.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `Counters` | `synchronized`, `AtomicLong`, `LongAdder`, start gates for testing races |
| `ParallelSum` | `Thread.start/join`, thread confinement, restoring the interrupt flag |
| `GracefulWorker` | `volatile`, interruption, keeping a worker alive through failures |

```
cd katas && ./mvnw test -Dgroups="concurrency & L1"
```

Read the tests too: `CountersTest.hammer` shows how to write a test that *provokes* a race (all threads wait at a `CountDownLatch` gate and start together).

## 8. Seen in the shop project

- `ratelimit/TokenBucket.java`: `synchronized tryTake` with a comment about the check-then-act race on the last token.
- `inventory/InventoryRepository.java`: the same check-then-act race in the database, fixed with one atomic `update ... set quantity = quantity - :qty where ... and quantity >= :qty` (Level 3). In-memory locks cannot protect data shared by several instances (L3, section 9).
- `outbox/OutboxPoller.java`: a background task (`@Scheduled`) that catches failures, so one bad tick (database down) does not stop the polling. Spring's scheduler threads are stopped by interruption on shutdown.

## 9. Interview checklist

1. Process vs thread. What do threads share, and what is private to each?
2. `start()` vs `run()`.
3. Why is `count++` not thread-safe? Draw the interleaving.
4. `synchronized` vs `AtomicLong` vs `LongAdder`: how does each work, when do you use which?
5. What does `volatile` guarantee, and what does it not?
6. Why can a thread loop forever on a non-volatile flag?
7. How do you stop a thread? What do you do when you catch `InterruptedException`?
8. Is a Spring singleton service thread-safe? What makes it so (or not)?
9. What is a check-then-act race? Give two examples from a web shop.
