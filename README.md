# java-spring-ai: the full-stack dojo

A training ground for the core knowledge of a Java / Spring / React full-stack developer: the concepts production systems rely on and interviews ask about, learned the way that sticks.

**Read → see it run → build it → get asked about it → get asked again when you are about to forget.**

Every topic is a **track** with three levels:

| Level | Meaning | You can... |
|---|---|---|
| **L1 Basic** | the concept from zero | explain what it is and use it correctly in simple code |
| **L2 Practical** | the everyday toolkit | write it the way production code does, and avoid the common traps |
| **L3 Production** | what breaks under load and in operations | diagnose incidents, make design trade-offs, and answer senior interview questions |

Each level of each track has:
1. **A lesson** (`tracks/NN-topic/L1-basics.md`, ...): concept, why it exists, small examples, traps, where it appears in the [shop project](https://github.com/elsepiol/spring-backend-ai), interview checklist.
2. **Runnable examples** (`tracks/NN-topic/examples/*.java`): one file each, `java File.java`, no build. They *show* the behaviour (a race losing updates, a deadlock being detected, virtual threads vs a pool).
3. **Katas** (`katas/`): code you write to make failing tests pass. Some are "build it" (an LRU cache, a bounded blocking queue), some are "find the bug" (a memory leak, a deadlock). Reference solutions in `solutions/`.
4. **A question bank** (`tracks/NN-topic/questions.md`): recall, "why", "what does this print", "spot the bug", production scenarios, design and interview questions, with hidden answers.

DSA problems appear inside the tracks where real systems use them (LRU cache next to collections, heaps next to top-K and k-way merge, ring buffers next to producer-consumer, token buckets next to rate limiting), not as a separate puzzle list.

---

## Quick start

Requires JDK 21 (Maven is downloaded by the wrapper). The SQL katas start a real PostgreSQL inside the tests: no Docker needed.

```
java quiz/Quiz.java                                   # 10 questions: the ones due for review first, then new ones
java quiz/Quiz.java --track concurrency --level 1     # one track and level
java quiz/Quiz.java --kind predict,bug --count 20     # only "what does it print" and "spot the bug"
java quiz/Quiz.java --stats                           # what you know, per track and level, and your weakest questions

java tracks/02-concurrency/examples/RaceCondition.java    # run an example

cd katas
./mvnw test -Dgroups="javacore & L1"                  # the katas of one track and level (they fail until you solve them)
./mvnw test -Dtest=LruCacheTest                        # one kata
```

**How the quiz remembers:** after each answer you say whether you knew it (`y`), partly (`p`) or not (`n`). Questions move through boxes asked again after 1, 3, 7, 14, then 30 days; a miss sends a question back to box 1 and asks it again before the session ends (spaced repetition, Leitner system). Your progress is stored in `.dojo/progress.tsv`, which is not committed.

**A good daily routine (30-60 minutes):** `java quiz/Quiz.java` for the reviews that are due (10 minutes), then one lesson section, its example, and its kata.

---

## Tracks

Ordered so that each track builds on the ones before it.

| # | Track | L1 Basic | L2 Practical | L3 Production | Status |
|---|---|---|---|---|---|
| 01 | [Java core](tracks/01-java-core/) | JVM, types, Strings, OOP, equals/hashCode, exceptions | collections internals, generics, streams, records/sealed | JVM memory, GC, leaks, diagnostics, Java 8-21 | **Built**: 3 lessons, 12 katas, 62 questions |
| 02 | [Concurrency](tracks/02-concurrency/) | threads, races, synchronized, volatile, interruption | pools, CompletableFuture, locks, concurrent collections, queues | memory model, deadlocks, stampedes, virtual threads, ThreadLocal, cross-instance | **Built**: 3 lessons, 10 katas, 60 questions |
| 03 | [Build tools](tracks/03-build-tools/) | Maven lifecycle, coordinates, versions, scopes, wrapper | mediation, BOMs, plugins, multi-module reactor, profiles, Gradle | reproducible builds, classpath conflicts, fast CI, releases, supply chain | **Built**: 3 lessons, 7 katas, a Maven playground, 60 questions |
| 04 | [SQL and databases](tracks/04-sql/) | joins, grouping, NULL logic, JDBC, SQL injection | indexes and EXPLAIN, window functions, keyset pagination, upserts | MVCC, locking, isolation and write skew, zero-downtime migrations | **Built**: 3 lessons, 9 katas on a real PostgreSQL, 60 questions |
| 05 | [Web, HTTP, REST](tracks/05-web-http/) | wire format, status codes, safe/idempotent methods, REST design | caching, ETags, idempotency keys, cursors, CORS, tokens vs cookies | timeouts, retries + jitter, circuit breakers, rate limiting, API evolution | **Built**: 3 lessons, 8 katas, 2 examples, 60 questions |
| 06 | [Spring core](tracks/06-spring-core/) | IoC and DI (build a container), beans, @Bean/@Primary/@Qualifier | scopes, lifecycle, configuration, profiles, events | proxies, self-invocation, post-processors, AOP, startup | **Built**: 3 lessons, 9 katas, 60 questions |
| 07 | [Spring Boot web](tracks/07-spring-boot-web/) | Boot, request flow, REST controllers, MockMvc | validation, problem details, filters vs interceptors, Jackson | auto-configuration and starters, typed config, actuator, production | **Built**: 3 lessons, 7 katas, 60 questions |
| 08 | [Spring Data JPA and transactions](tracks/08-jpa-transactions/) | entities, relationships, persistence context, repositories, JPQL | N+1, lazy loading and open-in-view, @Transactional, projections, batching | propagation and pitfalls, optimistic/pessimistic locking, outbox | **Built**: 3 lessons, 6 katas on a real PostgreSQL, 60 questions |
| 09 | [Security](tracks/09-security/) | authn vs authz, 401/403/404, IDOR, password hashing | JWT inside out, Spring Security filter chain, CSRF, CORS, cookies | method security, injection family, brute force, secrets, OWASP Top 10 | **Built**: 3 lessons, 7 katas, 60 questions |
| 10 | [Testing](tracks/10-testing/) | good tests, JUnit 6, AssertJ, seams, Mockito | test pyramid, fakes + contract tests, builders, Spring slices | Testcontainers, isolation, async tests, flaky tests, fast CI | **Built**: 3 lessons, 7 katas, 60 questions |
| 11 | [Docker and deployment](tracks/11-docker-deploy/) | containers, images, tags and digests, Dockerfiles | layers and caching, compose, probes, networking, config | JVM in containers, graceful shutdown, Kubernetes, rollouts, CI/CD | **Built**: 3 lessons, 6 katas, 3 reference files, 60 questions |
| 12 | [JavaScript and TypeScript](tracks/12-js-ts/) | equality, closures, `this`, modules, TypeScript basics | event loop, promises, concurrency limits, debounce/throttle | advanced types, runtime validation, leaks, performance, tooling | **Built**: 3 lessons, 7 katas (TypeScript, Vitest), 60 questions |
| 13 | React | components, JSX, props, state | hooks, effects, data fetching, forms, tests | rendering performance, state architecture, a11y, security | Planned (phase 6) |
| 14 | [Git, Linux, networking](tracks/14-git-linux-networking/) | Git object model, shell, permissions, processes, IP/CIDR, DNS, TCP | merge vs rebase, reflog, team workflows, log slicing, connection debugging | bisect, production debugging, TLS, keep-alive and load balancers | **Built**: 3 lessons, 7 katas, 60 questions |
| 15 | System design | caching, queues, scaling vocabulary | messaging, consistency, idempotency | full design interviews, using the shop as the case study | Planned (phase 7) |

## Global paths

Prefer breadth first? Follow a global level across all tracks, in this order, then move to the next level:

- **Global L1 (foundations):** 01 L1 → 02 L1 → 03 L1 → 04 L1 → 05 L1 → 06 L1 → 07 L1 → 08 L1 → 09 L1 → 10 L1 → 11 L1 → 12 L1 → 13 L1 → 14 L1 → 15 L1
- **Global L2 (practitioner):** the same order at L2.
- **Global L3 (production):** the same order at L3.

`java quiz/Quiz.java --level 1` drills everything at a global level.

Prefer depth? Take one track L1 → L3 before the next. Both work; mixing is fine. The quiz keeps everything you have seen alive either way.

---

## Repository layout

```
tracks/NN-topic/
  README.md            the track at a glance
  L1-basics.md         lessons
  L2-practical.md
  L3-production.md
  questions.md         the question bank (parsed by the quiz)
  examples/*.java      runnable single-file programs
katas/                 one Maven project; tests tagged by track and level
  src/main/java/dojo/<track>/l1..l3/   what you edit
  src/test/java/dojo/<track>/l1..l3/   the tests (do not edit)
solutions/katas/       reference solutions, same paths
quiz/Quiz.java         the drill (no dependencies)
scripts/verify-katas.sh
```

**CI** (`.github/workflows/ci.yml`) proves, on every push, that every kata fails without its solution and passes with it, that every question has an answer and a valid id, and that every example compiles. If a kata is broken, CI says so.

## Writing your own solutions

Work directly in `katas/src/main/java`. To reset a kata to its unsolved state: `git checkout -- katas/src/main/java/dojo/<track>/<level>/<Kata>.java`. To compare with the reference: `diff katas/src/main/java/dojo/... solutions/katas/src/main/java/dojo/...`. Look at the solution only after your tests pass, or after a real attempt.
