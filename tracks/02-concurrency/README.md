# Track 02: Concurrency

Every request in a Spring service runs on its own thread against shared singleton beans. This track is how not to corrupt data, hang, or fall over under load.

| Level | Lesson | Katas (`katas/src/main/java/dojo/concurrency/`) | Examples |
|---|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): threads, race conditions, synchronized, atomics, LongAdder, volatile and visibility, interruption | `Counters`, `ParallelSum`, `GracefulWorker` | RaceCondition, Visibility |
| L2 Practical | [L2-practical.md](L2-practical.md): thread pools and ThreadPoolExecutor, locks, ConcurrentHashMap traps, CompletableFuture, producer-consumer and backpressure, latches/barriers/semaphores, rate limiting | `BoundedBuffer` (DSA: ring buffer), `PriceQuotes`, `TokenBucket` (DSA: rate limiter) | ExecutorBasics, CompletableFutureTour |
| L3 Production | [L3-production.md](L3-production.md): Java Memory Model, safe publication, deadlocks, contention, cache stampedes, virtual threads, ThreadLocal leaks, pools in production, coordination across instances | `Bank` (deadlock), `Memoizer` (single-flight), `FanOut` (virtual threads), `Lazy` (double-checked locking) | Deadlock, VirtualThreads, ThreadLocalLeak |

```
java tracks/02-concurrency/examples/RaceCondition.java
cd katas && ./mvnw test -Dgroups="concurrency & L2"
java quiz/Quiz.java --track concurrency --level 3
```

Question bank: [questions.md](questions.md) (60 questions).

The tests are worth reading: they show how to make concurrency bugs reproducible (start gates with `CountDownLatch`, forced interleavings with `CyclicBarrier`, fake clocks, timeouts that turn a hang into a failure).
