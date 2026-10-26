# Testing, L3: real infrastructure, async code, flaky tests, fast suites

**Goal:** run integration tests against real infrastructure (Testcontainers, embedded databases) with proper isolation, test asynchronous and concurrent code without sleeps, hunt down flaky tests, and keep a large suite fast and trustworthy in CI. Know what coverage and mutation testing really tell you.

Prerequisite: L1, L2, Concurrency L1-L2. Katas: `cd katas && ./mvnw test -Dgroups="testing & L3"`. Drill: `java quiz/Quiz.java --track testing --level 3`.

---

## 1. Testcontainers and friends

**Testcontainers** starts real dependencies in Docker for the test run: PostgreSQL, Kafka, Redis, LocalStack (AWS), WireMock, any image.

```java
@SpringBootTest
@Testcontainers
class OrderFlowIT {
    @Container @ServiceConnection                          // Spring Boot wires the DataSource to it
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}
```

- Same engine and version as production: real SQL, real locking, real JSON operators.
- `@ServiceConnection` (Boot 3.1+) replaces hand-written `@DynamicPropertySource`.
- Start containers once per JVM (static fields, singleton pattern, reusable containers locally) and keep the Spring context cached across classes.
- No Docker? The dojo uses **embedded PostgreSQL** (real binaries in the test JVM), and CI runners provide Docker for Testcontainers.
- For HTTP dependencies: **WireMock** (a programmable fake server: stub responses, inject delays and faults, verify requests), so timeouts and retries are tested for real.

---

## 2. Isolation between integration tests

Tests that share a database must not see each other's data:
- **Rollback per test** (`@Transactional`): fast, but nothing commits, so commit-time behaviour is untested, and code running in other threads does not see the data.
- **Clean before each test**: `TRUNCATE ... RESTART IDENTITY CASCADE` all tables except the migration history, in one statement. Fast in PostgreSQL even for many tables.
- **A fresh database or schema per test class** (what the dojo's `Db.freshDatabase()` does; PostgreSQL's `CREATE DATABASE ... TEMPLATE` makes it quick).
- **Unique data per test** (random emails, unique tenant ids), so tests can even run in parallel against one database.

Never depend on ids or counts from other tests, or on test order.

> **Kata `DatabaseCleaner`**: find the tables in `information_schema`, keep Flyway's history, truncate in one statement.

---

## 3. Testing asynchronous code

`Thread.sleep(2000); assertThat(...)` is the classic mistake: slow when the work takes 5 ms, flaky when CI is overloaded and it takes 2.1 s.
- **Poll with a timeout**: check the condition repeatedly and return as soon as it holds (**Awaitility**: `await().atMost(5, SECONDS).untilAsserted(() -> assertThat(repo.count()).isEqualTo(1))`).
- **Make async code synchronous in unit tests**: inject the `Executor` (use a direct executor `Runnable::run`), the scheduler, the clock; test the logic without threads.
- **Concurrency tests**: start many threads behind a `CountDownLatch` so they really overlap, repeat (`@RepeatedTest`), and assert invariants (no duplicates, totals match). They can prove a race exists; they cannot prove its absence. For the JVM memory model there is jcstress.
- Always bound waits (timeouts on futures, latches, `@Timeout`), so a bug fails the test instead of hanging CI.

> **Kata `Eventually`**: replace "sleep, then assert" with a polling helper that fails fast and explains itself.

---

## 4. Flaky tests

A flaky test passes and fails without code changes. It is worse than no test: people learn to re-run and ignore red builds. Common causes:

| Cause | Fix |
|---|---|
| shared state (static fields, singletons, the database, system properties, `Locale.setDefault`) leaking between tests | instance state, reset in `@BeforeEach`, unique data, no static mutable state |
| test order dependence | run with random order (`junit.jupiter.testmethod.order.default=random`) to expose it |
| time (`now()`, time zones, DST, midnight, month ends) | inject `Clock`, fix the zone |
| sleeps and races in async tests | polling with timeouts, deterministic executors |
| unordered collections (`HashMap` iteration, SQL without `ORDER BY`) | assert ignoring order, or order explicitly |
| real network, ports, external services | fakes, WireMock, random ports |
| resource leaks (threads, files, connections) | close everything, `@TempDir` |

Process: quarantine only with a ticket and an owner, reproduce (repeat in a loop, under CPU load, with random order), fix the root cause. Track flakiness (CI re-run statistics).

> **Kata `FeatureFlags`**: a test that fails only when run after another one.

---

## 5. Coverage and mutation testing

- **Line/branch coverage** (JaCoCo) shows what was *executed*, not what was *checked*: a test without assertions gets 100%. Useful to find untested areas; a target like 80% easily becomes a game.
- **Mutation testing** (PIT, `pitest-maven`) changes the code (flip `<` to `<=`, remove a call, return null) and checks that some test fails. Surviving mutants reveal weak assertions. Expensive: run it on core logic, nightly, or on changed code.

---

## 6. A fast, trustworthy suite in CI

- Measure first (slowest test classes from the surefire reports, the number of Spring contexts started).
- Keep contexts cached (shared base configuration), start infrastructure once per JVM.
- Parallelise: surefire `forkCount`, JUnit parallel execution for isolated tests.
- Split: unit tests on every push, slower integration and E2E suites in parallel jobs; never skip them before merging.
- Fail on flaky behaviour rather than hide it; keep CI green as a team rule.
- Test what matters in production too: migrations on a copy of production data, contract tests between services, smoke tests after deploys, synthetic monitoring.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `DatabaseCleaner` | integration test isolation, `information_schema`, `TRUNCATE ... RESTART IDENTITY CASCADE` |
| `Eventually` | polling instead of sleeping, deadlines, interrupt handling, diagnosable failures |
| `FeatureFlags` (bug) | order-dependent tests caused by static state |

```
cd katas && ./mvnw test -Dgroups="testing & L3"
```

## 8. Seen in the shop project

- `support/PostgresIntegrationTest`: one Testcontainers PostgreSQL with `@ServiceConnection`, shared by every integration test for the whole JVM. Its comment records a real bug: with one container per class, a later class reusing a **cached Spring context** found its database already stopped, and every request hung for 30 seconds.
- Isolation by unique data (random SKUs and emails per test) rather than cleaning tables, so the shared database never needs resetting.
- `NotificationApiTest` waits for the async outbox worker with Awaitility (`await().atMost(10s).untilAsserted(...)`), never with a sleep; `support/Concurrently` starts threads together for race tests.
- CI runs the whole suite on every push, plus a container smoke test (Level 6).

## 9. Interview talk track

"I test logic with fast unit tests, designing code so time, ids and I/O are injected, and I mock only real boundaries. Anything involving SQL, serialisation, security config or transactions gets an integration test against the real engine, with Testcontainers or an embedded database, isolated by truncating tables or using unique data. Fakes are kept honest with contract tests. For async code I poll with timeouts instead of sleeping, or make the executor synchronous. Flaky tests are bugs: I look for shared state, time, ordering and races, and fix the root cause. I keep the suite fast by reusing Spring contexts and containers, and I treat coverage as a map of untested code, with mutation testing to check the assertions on critical logic."

**Questions you should be able to answer:**
1. Why use Testcontainers instead of H2 or mocks for repository tests?
2. Three ways to isolate integration tests that share a database, with trade-offs.
3. How do you test code that publishes an event processed asynchronously?
4. What causes flaky tests? How do you find an order-dependent test?
5. What does 90% line coverage tell you, and what not? What is mutation testing?
6. A Spring test suite takes 25 minutes. Where do you look first?
7. How would you test a retry with exponential backoff without waiting?
