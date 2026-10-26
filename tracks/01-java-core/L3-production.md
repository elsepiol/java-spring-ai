# Java core, L3: the JVM in production

**Goal:** explain what the JVM does with memory and CPU while your service runs, recognise the classic production failures (memory leaks, GC pauses, slow starts, OOM in containers), and know which tool to reach for. This is what separates "writes Java" from "runs Java services". It is also the bulk of senior Java interview questions.

Prerequisite: L1 and L2. Katas: `cd katas && ./mvnw test -Dgroups="javacore & L3"`. Drill: `java quiz/Quiz.java --track java-core --level 3`.

---

## 1. How the JVM runs your code

1. **Class loading.** Classes are loaded lazily, the first time they are used, by a hierarchy of class loaders (bootstrap → platform → application). Each loader first asks its parent ("parent delegation"), which is why you cannot replace `java.lang.String`. Frameworks and app servers create their own loaders; a class loaded by two loaders is two different classes (the source of `ClassCastException: Foo cannot be cast to Foo`).
2. **Verification and linking**, then static initialisers run (`static {}` blocks, static fields), once per class.
3. **Interpretation.** Bytecode starts out interpreted, slowly.
4. **JIT compilation (tiered).** The JVM counts calls and loop iterations. Warm methods are compiled quickly by C1; hot ones are recompiled by C2 with aggressive optimisations: inlining, escape analysis (objects that never leave a method may never be allocated on the heap), loop unrolling, removing checks that never fail. If an assumption breaks (a new subclass is loaded), the code is **deoptimised** back to the interpreter.

Production consequences:
- **Warm-up:** the first thousands of requests after a start are slower. Rolling deploys and autoscaling should account for it (readiness after warm-up, or send traffic gradually). CDS archives, Spring AOT, and GraalVM native images reduce start time.
- **Micro-benchmarks lie** unless you use **JMH**: without warm-up and protection from dead-code elimination, you measure the interpreter, or nothing at all.

---

## 2. Memory areas

```
JVM process memory  (what the container limit actually caps)
├── Heap (-Xmx)                              your objects; managed by the GC
│   ├── Young generation: Eden + 2 Survivor spaces    new objects; collected often, cheaply
│   └── Old generation                                objects that survived several young GCs
├── Metaspace                                class metadata (replaced PermGen in Java 8); grows with loaded classes
├── Thread stacks (-Xss, ~1 MB reserved each) one per PLATFORM thread: frames, locals
├── Code cache                               JIT-compiled machine code
├── Direct / native memory                   NIO buffers (Netty), the GC's own structures, JNI
```

- **Heap size:** `-Xmx` sets the maximum. In containers, the JVM reads the cgroup memory limit and by default uses **25% of it** for the max heap (`-XX:MaxRAMPercentage=25`). For a service alone in its container, 50-75% is typical; the rest is for metaspace, stacks, direct memory and the OS. The shop project's image sets `MaxRAMPercentage=75`.
- **Container OOM-kill vs Java OOM:** if the *whole process* exceeds the container limit (heap + everything else), the kernel kills it (exit code 137, `OOMKilled`) with no Java stack trace. A `java.lang.OutOfMemoryError` is the JVM itself refusing an allocation. They need different fixes.
- **Thread stacks are not on the heap.** 2,000 platform threads reserve roughly 2 GB of virtual address space for stacks. (Virtual threads, concurrency track L3, have small heap-allocated stacks.)

---

## 3. Garbage collection

**What is garbage?** An object that is not reachable from a **GC root**: local variables of live threads, static fields, JNI references. Reachability, not reference counting, so cycles are collected fine.

**Generational hypothesis:** most objects die young (request DTOs, temporary strings, iterators). So:
- **Young (minor) GC:** copy the few survivors out of Eden; everything else is freed at once. Cost is proportional to the *live* objects, not the garbage, so allocation-heavy code is cheaper than you might think.
- Objects that survive enough young GCs are **promoted** to the old generation.
- **Old/full GC:** much more expensive. A heap whose old generation keeps growing is the signature of a leak.

**The collectors you should know:**

| GC | When | Trade-off |
|---|---|---|
| **G1** (default since Java 9) | general purpose, heaps from ~1 GB up | splits the heap into regions, collects the most garbage-filled first, aims at a pause target (`-XX:MaxGCPauseMillis=200` default) |
| **ZGC** (`-XX:+UseZGC`, generational since 21) | low latency, large heaps | pauses well under 1 ms regardless of heap size; uses more CPU and memory |
| **Parallel** | batch jobs | best throughput, longer pauses |
| **Serial** | tiny containers | the JVM picks it automatically with fewer than 2 CPUs or under ~1.8 GB of memory. A surprise in small pods |

- **Stop-the-world pauses** freeze every application thread. A 500 ms pause shows up as a latency spike at p99, as timeouts upstream, and as failed health checks.
- **GC logs** are cheap; enable them in production: `-Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m`.
- Tuning order: (1) fix allocation hot spots and leaks, (2) size the heap correctly, (3) choose the collector, (4) only then tune flags.

Run `java -Xmx64m -Xlog:gc tracks/01-java-core/examples/HeapAndGc.java` and read the log lines.

---

## 4. Memory leaks in a garbage-collected language

A Java memory leak is an object that is **still reachable but will never be used again**. The GC cannot free it, because something still points at it. The classic causes, all seen in real production systems:

1. **Unbounded collections in long-lived objects**: a `static Map` "cache", a singleton Spring bean holding a `HashMap` that is only ever `put` into. Fix: a bounded cache with eviction and expiry (Caffeine), or don't cache.
2. **Mutable keys in a hash map**: the key's hash changes after insertion, so the entry can never be found or removed again. Fix: immutable keys (records). Kata `ActiveCarts`.
3. **Listeners and callbacks never unregistered**: the publisher keeps every subscriber alive.
4. **`ThreadLocal` in thread pools**: pool threads live forever, so values set and never `remove()`d stay forever, and leak data between requests (concurrency track, L3).
5. **ClassLoader leaks**: redeploying an app in a server where something still references one old class keeps the whole old app's classes in metaspace (`OutOfMemoryError: Metaspace`).
6. **Huge objects held by a small one**: a cached `subList` view or a request object kept in a log context.

**How to find one:**
- **Symptoms:** heap used *after* each full GC keeps rising (the baseline climbs), GC runs more and more often, latency grows, then `OutOfMemoryError: Java heap space` or a container OOM-kill. Typical pattern: "it dies every few days after a deploy".
- **Always run with** `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps` so the first failure leaves evidence.
- **On demand:** `jcmd <pid> GC.heap_dump /tmp/heap.hprof` (pauses the JVM; careful in production), or `jcmd <pid> GC.class_histogram` for a quick "which classes have the most instances" view. Take two histograms a few minutes apart and compare.
- **Analyse** the dump with Eclipse MAT or VisualVM: the **dominator tree** shows which object keeps the most memory alive, and "path to GC roots" shows *why* it is still reachable.

**The OutOfMemoryError messages and what they mean:**

| Message | Meaning |
|---|---|
| `Java heap space` | the heap is full of live objects: a leak, or the heap is too small for the load |
| `GC overhead limit exceeded` | the JVM spends ~98% of its time in GC and frees almost nothing: practically the same as above |
| `Metaspace` | too many classes: class loader leak, or heavy runtime code generation |
| `unable to create native thread` | the OS refused another thread: thread leak or too many platform threads |
| `Direct buffer memory` | NIO direct buffers exhausted (Netty, file channels) |

> **Kata `ActiveCarts`**: an in-memory cart store that leaks in production. The tests reproduce it; find and fix the cause.

---

## 5. Diagnosing a live JVM

| Question | Tool |
|---|---|
| Which JVMs run here, with which flags? | `jcmd` (lists them), `jcmd <pid> VM.flags`, `VM.system_properties` |
| Why is it stuck or slow? What are the threads doing? | `jcmd <pid> Thread.print` (or `jstack`): take 3 dumps a few seconds apart; threads in the same frame every time are your bottleneck or deadlock |
| Is GC the problem? | GC log; `jstat -gcutil <pid> 1s` (live heap occupancy and GC time) |
| Where do CPU time and allocations go? | **Java Flight Recorder**: `jcmd <pid> JFR.start duration=60s filename=rec.jfr`, open in JDK Mission Control. Low overhead (~1%), safe in production |
| What is filling the heap? | `jcmd <pid> GC.class_histogram`, heap dump + MAT |
| Native memory? | `-XX:NativeMemoryTracking=summary`, then `jcmd <pid> VM.native_memory summary` |

In Kubernetes: `kubectl exec -it <pod> -- jcmd 1 Thread.print`. The JVM is usually process 1 in the container.

**A CPU spike runbook:** `top -H -p <pid>` shows the busiest thread ids → convert to hex → find that `nid=0x...` in a thread dump → read its stack. Usually an infinite loop, a regex with catastrophic backtracking, or GC threads (then look at the heap).

---

## 6. Caching without leaking

Every cache is a deliberate memory trade-off and needs:
- a **bound** (max entries or max weight), with an eviction policy (LRU, or Caffeine's W-TinyLFU, which also considers frequency);
- **expiry** (time to live after write, or idle time after access), because data changes;
- **metrics**: hit ratio, evictions, load time. A 5% hit ratio means the cache costs more memory than it saves time;
- a decision about **staleness**: how old may a cached product price be? (The shop project evicts on update.)
- injected **time** (`Clock`), so expiry is testable without sleeping.

> **Kata `ExpiringCache`**: bounded + TTL + stats, with a test clock. A small version of what Caffeine does.

Weak and soft references (`WeakHashMap`, `SoftReference`) look like automatic caches but behave unpredictably under GC pressure. Prefer explicit bounds.

---

## 7. Big data, small memory: streaming

A service with a 512 MB heap must sometimes process a 10 GB file or a million database rows. The rule: **never hold it all at once**.
- Read line by line (`Files.lines`, `BufferedReader`), page through the database (keyset pagination, JDBC fetch size, JPA `Stream` results), stream HTTP responses.
- **External sort:** sort chunks that fit in memory, write each sorted chunk to disk, then **k-way merge** the chunks with a heap holding one element per chunk. The same merge combines sorted log files, database index segments (LSM trees in Cassandra, RocksDB) and search results from shards.

> **Kata `KWayMerge`**: lazily merge k sorted iterators with a `PriorityQueue`. O(log k) per element, O(k) memory, and it works on endless inputs.

---

## 8. Performance pitfalls that actually matter

1. **Algorithm first.** An O(n²) loop over 100,000 orders beats any JVM flag. Profile (JFR) before optimising.
2. **N+1 I/O** (one query or HTTP call per item) dwarfs all CPU concerns (JPA track).
3. **Boxing in hot paths** (`Map<Long, Long>` counters, `Stream<Integer>` sums): allocation and cache misses. Use primitives or primitive streams.
4. **String concatenation in loops**, and building log messages that are never logged: use `log.debug("x={}", x)`, not `log.debug("x=" + x)`.
5. **Exceptions as control flow:** creating an exception captures a stack trace, which is expensive.
6. **Regex compiled in a loop:** `String.matches` compiles every time; keep a `static final Pattern`.
7. **Unbounded thread creation or queues** (concurrency track).
8. **Excessive logging and debug-level JSON serialisation** under load.

---

## 9. Defensive copying and immutability (the "effective Java" part)

An object that hands out its internal mutable state cannot protect its invariants:

```java
public record Order(List<Item> items) {
    public Order {
        items = List.copyOf(items);   // copy IN: later changes to the caller's list do not leak in
    }                                 // and List.copyOf is unmodifiable: nothing leaks OUT either
}
```

Immutable objects are thread-safe without locks, safe as map keys, and easy to cache. Make classes immutable by default; make them mutable only when you need to (JPA entities are the usual exception).

---

## 10. Java versions you are expected to know (8 → 21)

| Version | What interviewers expect you to know |
|---|---|
| 8 (2014) | lambdas, streams, `Optional`, `java.time`, default methods, Metaspace |
| 9 | modules (JPMS), `List.of`/`Map.of`, G1 default, compact strings |
| 10 | `var` for local variables |
| 11 (LTS) | `HttpClient`, `String.strip/isBlank/lines`, single-file `java X.java` |
| 14-16 | switch expressions, records, `instanceof` pattern matching, text blocks, helpful NPE messages |
| 17 (LTS) | sealed classes |
| 21 (LTS) | **virtual threads**, pattern matching for `switch`, record patterns, sequenced collections (`getFirst`, `reversed`), generational ZGC |

Long-term support releases (8, 11, 17, 21, 25) are what companies run. Spring Boot 3 requires Java 17+; Spring Boot 4 recommends 21+.

---

## 11. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `ActiveCarts` | find a production memory leak (mutable keys) | hashing internals |
| `ExpiringCache` | bounded cache, TTL, stats, test clock | LRU via LinkedHashMap |
| `KWayMerge` | streaming, laziness, iterators | heap k-way merge |

```
cd katas && ./mvnw test -Dgroups="javacore & L3"
```

## 12. Seen in the shop project

- Container-aware heap sizing: `Dockerfile` (`-XX:MaxRAMPercentage=75`) in spring-backend-ai.
- Bounded, expiring caches instead of maps: `cache/CacheConfig.java`, `ratelimit/RateLimiter.java`.
- Keyset pagination instead of loading everything: `product/Cursor.java`, `ProductRepository`.

## 13. Interview talk track

"A Java service's memory is more than the heap: metaspace, thread stacks, code cache and direct buffers all count against the container limit, so I size the heap as a percentage of the container memory, typically 50-75%, and I watch both Java OOMs and container OOM-kills. Most objects die young, so minor GCs are cheap; a steadily rising old generation after full GCs is the sign of a leak. The usual causes are unbounded caches, mutable map keys, unregistered listeners and ThreadLocals in pools. I run with HeapDumpOnOutOfMemoryError, and investigate with class histograms, heap dumps and the dominator tree in MAT. For latency problems I look at GC logs first, then JFR for CPU and allocation profiles, and thread dumps for blocking. G1 is the right default; ZGC when pause times matter more than CPU. And I benchmark with JMH, not with a loop and System.nanoTime."

**Questions you should be able to answer:**
1. Walk through what happens from `javac` to machine code. What is JIT warm-up and why does it matter for deploys?
2. Name the JVM memory areas. Which ones count against a container's memory limit?
3. What is a GC root? Why can Java collect reference cycles?
4. Explain the generational hypothesis and minor vs full GC.
5. G1 vs ZGC vs Parallel: when would you pick each?
6. How can a garbage-collected language leak memory? Give four causes.
7. A service gets slower every day and dies after 3 days. Walk through your investigation.
8. `OutOfMemoryError: Java heap space` vs `Metaspace` vs container `OOMKilled`.
9. How do you find a CPU-hungry thread in a running JVM?
10. What must every production cache have?
11. How do you sort a file larger than memory?
12. Why are naive micro-benchmarks wrong?
