# Testing: question bank

Drill with `java quiz/Quiz.java --track testing`.

## L1

### TEST-L1-001 · recall
What does FIRST stand for in unit testing?

<details><summary>Answer</summary>

Fast (milliseconds, so they are run constantly), Isolated/Independent (no order dependence or shared state), Repeatable (same result anywhere, any time), Self-validating (pass/fail without reading logs), Timely (written with the code, ideally before it).

</details>

### TEST-L1-002 · recall
What is the Arrange-Act-Assert structure?

<details><summary>Answer</summary>

Arrange: set up the object under test and its inputs. Act: perform the one behaviour being tested. Assert: check the outcome. Also called Given-When-Then. One Act per test keeps tests focused and failures easy to read.

</details>

### TEST-L1-003 · why
Why should a test check behaviour rather than implementation details?

<details><summary>Answer</summary>

Tests coupled to internals (private methods, call sequences, exact SQL strings) break on every refactoring even when behaviour is unchanged, so people stop trusting or maintaining them. Behaviour-based tests (outputs, state changes, observable effects) allow refactoring, which is one of the main reasons to have tests.

</details>

### TEST-L1-004 · why
Why should you see a new test fail before trusting it?

<details><summary>Answer</summary>

A test that cannot fail proves nothing: a wrong assertion, a test that never runs (missing @Test, wrong tag), or a mock returning what the assertion expects. Watching it fail for the right reason (test-first, or temporarily breaking the code) shows that it actually detects the bug.

</details>

### TEST-L1-005 · recall
JUnit 6 lifecycle: what runs when for `@BeforeAll`, `@BeforeEach`, `@AfterEach`, `@AfterAll`? How many test-class instances are created?

<details><summary>Answer</summary>

`@BeforeAll` (static) once before all tests of the class, `@BeforeEach` before each test, `@AfterEach` after each, `@AfterAll` (static) once at the end. By default JUnit creates a new instance of the test class for every test method (`PER_METHOD`), so instance fields start fresh; `@TestInstance(PER_CLASS)` changes that.

</details>

### TEST-L1-006 · recall
What is a parameterised test? Name three sources.

<details><summary>Answer</summary>

One test method run with many argument sets, each reported separately: `@ParameterizedTest` with `@ValueSource` (single values), `@CsvSource`/`@CsvFileSource` (rows), `@MethodSource` (a factory method returning arguments), `@EnumSource`, `@ArgumentsSource`.

</details>

### TEST-L1-007 · recall
Why prefer AssertJ's `assertThat(list).containsExactly("a", "b")` to `assertTrue(list.equals(List.of("a", "b")))`?

<details><summary>Answer</summary>

The failure message: AssertJ prints the actual and expected contents and what differs; `assertTrue` says only "expected true but was false". AssertJ also reads like a sentence, chains several checks, and offers specific assertions (extracting, containsExactlyInAnyOrder, hasMessageContaining).

</details>

### TEST-L1-008 · bug
```java
@Test void parsesDate() {
    LocalDate d = parser.parse("01/02/2026");
    assertThat(d).isNotNull();
}
```
What is weak about this test?

<details><summary>Answer</summary>

It asserts almost nothing: any date (1 Feb or 2 Jan) passes, and only null fails. Assert the exact expected value, `isEqualTo(LocalDate.of(2026, 2, 1))`, and add cases for the ambiguity (day/month order) and invalid input.

</details>

### TEST-L1-009 · recall
What is a seam in the context of testability?

<details><summary>Answer</summary>

A place where behaviour can be replaced without editing the code under test, usually a constructor parameter or an interface (a Clock, an IdGenerator, a PaymentGateway). Seams let tests substitute deterministic or fake collaborators.

</details>

### TEST-L1-010 · scenario
How do you test code that uses `Instant.now()` and `UUID.randomUUID()`?

<details><summary>Answer</summary>

Inject them: a `java.time.Clock` (Clock.systemUTC() in production, Clock.fixed or a mutable test clock in tests) and a `Supplier<UUID>`/IdGenerator. Then tests control time and ids exactly, including boundaries (expiry at the exact second), without sleeping or reading the real clock.

</details>

### TEST-L1-011 · recall
Dummy, stub, mock, spy, fake: define each.

<details><summary>Answer</summary>

Dummy: passed to satisfy a signature, never used. Stub: returns canned answers to calls. Mock: records interactions so the test can verify them (often also stubs). Spy: a real object wrapped to record or partially override calls. Fake: a working, simplified implementation (in-memory repository, fake payment gateway).

</details>

### TEST-L1-012 · recall
`when(x).thenReturn(...)` vs `doReturn(...).when(x)` vs `doThrow(...).when(mock).method()`: when do you need the do-forms?

<details><summary>Answer</summary>

For void methods (you cannot put a void call inside `when(...)`), and for spies, where `when(spy.method())` would call the real method while stubbing. Otherwise `when().thenReturn()` reads better and is type-checked.

</details>

### TEST-L1-013 · bug
`verify(mailer).send(eq(email), any())` vs `verify(mailer).send(email, any())`. Which one fails, and how?

<details><summary>Answer</summary>

The second: Mockito requires that if any argument uses a matcher, all arguments use matchers, otherwise it throws `InvalidUseOfMatchersException`. Wrap raw values in `eq(...)`.

</details>

### TEST-L1-014 · recall
What does an `ArgumentCaptor` do?

<details><summary>Answer</summary>

It captures the arguments passed to a mocked method during `verify`, so you can assert on them afterwards (`verify(mailer).send(captor.capture()); assertThat(captor.getValue().to()).isEqualTo(...)`). Useful when the argument is built inside the code under test.

</details>

### TEST-L1-015 · recall
What does Mockito's strict stubbing (the default with MockitoExtension) report?

<details><summary>Answer</summary>

Unused stubs (`UnnecessaryStubbingException`: the test sets up something the code never calls, usually dead setup or a wrong expectation) and stubbing argument mismatches (`PotentialStubbingProblem`: the code called the stubbed method with different arguments). Both catch tests that do not test what they claim.

</details>

### TEST-L1-016 · why
Why is mocking value objects, lists or your own simple helpers a bad idea?

<details><summary>Answer</summary>

Their real behaviour is cheap and correct, and a mock replaces it with a script that may be wrong (a mocked `List.size()` returning 3 while `get(2)` fails). Tests become long and brittle. Mock collaborators at boundaries (I/O, external systems), and use real objects for the rest.

</details>

### TEST-L1-017 · design
When is verifying an interaction (`verify(...)`) the right assertion, and when should you assert on state instead?

<details><summary>Answer</summary>

Verify interactions when the interaction is the observable behaviour and there is no state to check: an email sent, a payment charged, an event published, nothing called on invalid input. Otherwise assert on results or state (return value, repository content), which survives refactoring.

</details>

### TEST-L1-018 · predict
A test with no assertions calls the service and passes. What does that test check?

<details><summary>Answer</summary>

Only that no exception is thrown (and that it does not hang). It contributes to line coverage while asserting nothing about results, which is why coverage alone is misleading. Either add assertions or name it accordingly (`doesNotThrow...`) and use `assertThatNoException()`.

</details>

### TEST-L1-019 · interview
"How do you decide what to unit test?"

<details><summary>Answer</summary>

Test behaviour that has logic and risk: business rules, calculations, branching, edge cases (empty, null, boundaries, overflow, time zones), error handling. Skip trivial getters, framework behaviour and generated code. Each bug fix gets a regression test. Keep the code testable (injected dependencies, pure functions) so unit tests stay cheap, and leave wiring and infrastructure to integration tests.

</details>

### TEST-L1-020 · dsa
A function takes three independent boolean flags. How many combinations exist, and what technique reduces the test cases while still catching most interaction bugs?

<details><summary>Answer</summary>

2^3 = 8 combinations (2^n in general, which explodes). Pairwise (all-pairs) testing covers every combination of each PAIR of parameters with far fewer cases (here 4), since most bugs involve one or two parameters. Equivalence partitioning and boundary value analysis reduce cases for ranged inputs.

</details>

## L2

### TEST-L2-001 · recall
Describe the test pyramid.

<details><summary>Answer</summary>

Many fast unit tests at the bottom, fewer integration tests in the middle (real database, real HTTP layer, real context), very few end-to-end tests at the top (the deployed system through its UI or API). Lower tests are cheaper and more precise; higher ones give confidence that the pieces work together.

</details>

### TEST-L2-002 · recall
What is the "ice-cream cone" anti-pattern?

<details><summary>Answer</summary>

An inverted pyramid: most testing is manual or end-to-end through the UI, with few unit tests. The suite is slow, brittle and expensive, failures are hard to locate, and teams stop running it.

</details>

### TEST-L2-003 · why
Why do mocked repository tests not catch bugs in your SQL or JPA mappings?

<details><summary>Answer</summary>

The mock returns whatever you stub; no SQL is executed, no mapping or constraint is checked, no transaction or lazy loading happens. Those bugs live at the boundary, so test repositories against the real database engine (integration tests).

</details>

### TEST-L2-004 · recall
Fake vs mock: when do you prefer a fake?

<details><summary>Answer</summary>

When many tests use the collaborator and care about state rather than calls: an in-memory repository or a fake message bus makes tests read naturally ("save, then find") without stubbing scripts, and survives refactoring. Prefer mocks for verifying a specific interaction or simulating a failure.

</details>

### TEST-L2-005 · recall
What is a contract test for an interface with a fake and a real implementation?

<details><summary>Answer</summary>

An abstract test class that specifies the behaviour every implementation must have, with an abstract factory method; one subclass per implementation (fake, JDBC) runs the same tests. It keeps the fake from drifting from reality (e.g. the unique-email rule).

</details>

### TEST-L2-006 · recall
What is consumer-driven contract testing?

<details><summary>Answer</summary>

Each API consumer publishes the requests it makes and the responses it relies on (Pact files, Spring Cloud Contract); the provider's build replays them against the real provider. A breaking change fails the provider's build before deployment, without full end-to-end environments.

</details>

### TEST-L2-007 · recall
What is a test data builder?

<details><summary>Answer</summary>

A helper that creates valid domain objects with sensible defaults and fluent overrides: `anOrder().paid().withLine("A", 2, 500).build()`. Tests state only the details relevant to them, setup stays short, and adding a required field changes one place instead of hundreds of tests.

</details>

### TEST-L2-008 · bug
A shared `static Order ORDER = new Order(...)` fixture is mutated by one test (`ORDER.addLine(...)`). What problem appears?

<details><summary>Answer</summary>

Tests depend on each other: whether another test passes depends on execution order and on which tests ran before. Create fresh fixtures per test (builders, `@BeforeEach`) and avoid static mutable state.

</details>

### TEST-L2-009 · recall
`@SpringBootTest` vs `@WebMvcTest` vs `@DataJpaTest`.

<details><summary>Answer</summary>

`@SpringBootTest` loads the whole application context (optionally a real server): for wiring and end-to-end tests inside the service. `@WebMvcTest` loads only the MVC layer for chosen controllers (plus advice, filters, converters, security): for status codes, JSON, validation; other beans are supplied with `@MockitoBean`. `@DataJpaTest` loads JPA, repositories and a DataSource, with each test in a rolled-back transaction: for queries and mappings.

</details>

### TEST-L2-010 · recall
What does `@MockitoBean` do?

<details><summary>Answer</summary>

Replaces (or adds) a bean of that type in the Spring test context with a Mockito mock, which the test can stub and verify. It is the Spring Framework 6.2+ replacement for Spring Boot's deprecated `@MockBean`; `@MockitoSpyBean` wraps the real bean.

</details>

### TEST-L2-011 · why
Why can sprinkling different `@MockitoBean`s across test classes make the suite much slower?

<details><summary>Answer</summary>

Spring caches application contexts by their configuration, and the set of mocked beans is part of the cache key. Each distinct combination starts a new context (seconds each, plus connection pools). Standardise test configurations (shared base classes) so contexts are reused.

</details>

### TEST-L2-012 · recall
What is MockMvc, and how does it differ from calling a real port?

<details><summary>Answer</summary>

It drives the full Spring MVC stack in-process (filters, dispatcher, converters, advice, security when applied) with mock servlet requests: no network, fast. A real port (`webEnvironment = RANDOM_PORT` with TestRestTemplate/RestTestClient) also exercises the embedded server, real HTTP serialisation and connection behaviour, and the test runs in a different thread from the server.

</details>

### TEST-L2-013 · bug
A `@SpringBootTest` with a real port and `@Transactional` on the test class: the test saves a product, then calls `GET /products/1` over HTTP and gets 404. Why?

<details><summary>Answer</summary>

The test's transaction has not committed (it will roll back at the end), and the HTTP request is served by a server thread in its own transaction, which cannot see uncommitted data. Test transactions only work when the code runs in the test's thread (MockMvc). Commit and clean up, or use MockMvc.

</details>

### TEST-L2-014 · why
Why should PostgreSQL-backed code not be tested on H2?

<details><summary>Answer</summary>

H2 differs in SQL dialect, functions, types (JSONB, arrays), locking and isolation, constraint timing, and error codes. Tests can pass on H2 and fail in production (or the reverse), and PostgreSQL-specific features cannot be tested at all. Use the real engine via Testcontainers or embedded PostgreSQL.

</details>

### TEST-L2-015 · design
Which test type for each: (a) a discount calculation, (b) a 400 response with field errors, (c) a native query with window functions, (d) an outbox event published after commit?

<details><summary>Answer</summary>

(a) Plain unit test. (b) `@WebMvcTest` with MockMvc. (c) Repository integration test against real PostgreSQL. (d) `@SpringBootTest` against real infrastructure, committing, then polling for the published event.

</details>

### TEST-L2-016 · recall
What does `@DynamicPropertySource` do?

<details><summary>Answer</summary>

It registers properties computed at runtime (a container's JDBC URL and random port) into the test's Spring Environment before the context starts. `@ServiceConnection` replaces it for supported containers.

</details>

### TEST-L2-017 · bug
A `@WebMvcTest` fails at startup: "No qualifying bean of type OrderService". Why, and the fix?

<details><summary>Answer</summary>

The slice loads only web components; services are not scanned. Provide the dependency: `@MockitoBean OrderService orders` (and stub it), or `@Import` a real lightweight implementation.

</details>

### TEST-L2-018 · scenario
You need to test that your HTTP client retries on 503 and times out after 2 seconds. How?

<details><summary>Answer</summary>

Run a programmable fake server (WireMock, MockWebServer) that returns 503 then 200, or delays its response; point the client at it, and assert on the result, the number of requests received, and the timeout exception. Inject a fake sleeper or clock so the backoff does not really wait.

</details>

### TEST-L2-019 · interview
"What would your testing strategy be for a new Spring Boot microservice?"

<details><summary>Answer</summary>

Unit tests for domain logic (no Spring), `@WebMvcTest` for the API contract (status codes, JSON, validation, security rules), repository tests against real PostgreSQL via Testcontainers, a few `@SpringBootTest` flows (transactions, messaging, outbox), consumer-driven contracts with the services it talks to, and smoke tests after deployment. Shared test configuration for context caching, builders for test data, CI on every push, no H2, no sleeps.

</details>

### TEST-L2-020 · dsa
A Spring test suite has 120 test classes whose configurations fall into 6 distinct combinations. If each context takes 4 seconds to start, what is the startup cost with context caching vs without?

<details><summary>Answer</summary>

With caching: 6 × 4 = 24 seconds (each distinct configuration starts once, if the cache is big enough: default max 32 contexts). Without (e.g. `@DirtiesContext` everywhere): 120 × 4 = 480 seconds. It is memoization keyed by configuration, with an LRU eviction policy.

</details>

## L3

### TEST-L3-001 · recall
What is Testcontainers, and what does `@ServiceConnection` add?

<details><summary>Answer</summary>

A library that starts real dependencies (PostgreSQL, Kafka, Redis, LocalStack) in Docker containers for tests and cleans them up. `@ServiceConnection` (Spring Boot 3.1+) lets Boot derive the connection details (URL, credentials) from the container automatically, instead of `@DynamicPropertySource`.

</details>

### TEST-L3-002 · bug
Each integration test class starts its own static PostgreSQL container. A later class that reuses a cached Spring context hangs for 30 seconds on every request. Why?

<details><summary>Answer</summary>

The cached context's connection pool points at the first class's container, which was stopped when that class finished. The new class gets the cached context (same configuration) with a dead database, so connection attempts wait for the pool timeout. Fix: one container per JVM shared by all classes (singleton pattern), as the shop's PostgresIntegrationTest does.

</details>

### TEST-L3-003 · recall
Name three ways to isolate integration tests sharing a database, with one drawback each.

<details><summary>Answer</summary>

Rollback per test (`@Transactional`): hides commit-time behaviour and other threads cannot see the data. Truncate tables before each test: costs a statement per test and does not allow parallel tests. Unique data per test (random emails, ids): needs discipline, and assertions must be scoped (no "count all rows"). A fresh database or schema per class: slower setup.

</details>

### TEST-L3-004 · recall
What does `TRUNCATE a, b RESTART IDENTITY CASCADE` do in PostgreSQL?

<details><summary>Answer</summary>

Empties the listed tables in one statement (fast, without scanning rows), resets their identity columns and sequences to the start, and with CASCADE also truncates tables that reference them by foreign key. Truncating them together avoids foreign-key ordering problems. Exclude the migration history table.

</details>

### TEST-L3-005 · bug
`publisher.publish(event); Thread.sleep(1000); assertThat(consumer.received()).hasSize(1);` What is wrong?

<details><summary>Answer</summary>

A fixed sleep is both slow (waits a full second even when delivery takes 5 ms) and flaky (fails when CI is slow and delivery takes 1.1 s). Poll with a timeout instead: `await().atMost(5, SECONDS).untilAsserted(() -> assertThat(consumer.received()).hasSize(1))`.

</details>

### TEST-L3-006 · recall
How can you unit test code that submits work to an executor without dealing with threads?

<details><summary>Answer</summary>

Inject the Executor and use a direct one in tests (`Runnable::run`, or `MoreExecutors.directExecutor()`), so tasks run synchronously in the test thread. Similarly inject the scheduler and clock. Real concurrency is then tested separately in a few dedicated tests.

</details>

### TEST-L3-007 · scenario
How do you write a test that has a real chance of exposing a race condition?

<details><summary>Answer</summary>

Start many threads (more than the number of cores) waiting on a CountDownLatch so they start together, have each perform many operations, join with a timeout, then assert invariants (counts, no duplicates). Repeat the test (`@RepeatedTest`). A pass is not proof of correctness; for memory-model issues use jcstress.

</details>

### TEST-L3-008 · recall
What makes a test flaky? Name five causes.

<details><summary>Answer</summary>

Shared mutable state between tests (statics, singletons, database rows), order dependence, real time (now(), time zones, DST, month ends), fixed sleeps and races in async code, unordered collections or SQL without ORDER BY, real network and ports, resource leaks, and dependence on the environment (locale, file system, CPU speed).

</details>

### TEST-L3-009 · scenario
A test fails about once in 30 CI runs. What is your process?

<details><summary>Answer</summary>

Do not just re-run. Reproduce: run it in a loop, in random order (`junit.jupiter.testmethod.order.default=random`), in parallel, and under CPU load; read failure logs for timing or ordering clues. Look for the usual causes (shared state, time, sleeps, unordered results). Fix the root cause; if it must wait, quarantine it with a ticket and an owner rather than ignoring red builds.

</details>

### TEST-L3-010 · bug
`assertThat(repo.findByStatus(NEW)).containsExactly(order1, order2);` passes locally and sometimes fails in CI. Likely cause?

<details><summary>Answer</summary>

The query has no ORDER BY, so row order is unspecified and can change with data volume, plans or concurrent tests. Either order explicitly in the query (if the API promises an order) or assert with `containsExactlyInAnyOrder`.

</details>

### TEST-L3-011 · recall
What does line coverage measure, and what does it not?

<details><summary>Answer</summary>

The share of lines executed during tests. It does not tell whether the results were checked (a test without assertions counts), whether edge cases were tried, or whether the tests would catch a change in behaviour. Use it to find untested areas, not as proof of quality.

</details>

### TEST-L3-012 · recall
What is mutation testing?

<details><summary>Answer</summary>

A tool (PIT for Java) makes small changes ("mutants") to the code, such as flipping conditions, changing boundaries, removing calls or returning defaults, and runs the tests against each. A mutant that no test detects (it "survives") points to a missing or weak assertion. It is expensive, so it is usually run on critical modules or changed code.

</details>

### TEST-L3-013 · scenario
A Spring Boot test suite takes 25 minutes in CI. Where do you look first?

<details><summary>Answer</summary>

Count Spring contexts started (context cache misses from differing `@MockitoBean`s, properties, profiles, `@DirtiesContext`) and containers started per class; then the slowest test classes from the surefire reports, fixed sleeps and long timeouts, and E2E tests that could be lower-level. Fix with shared test configuration, singleton containers, polling instead of sleeps, parallel forks, and splitting the suite into parallel CI jobs.

</details>

### TEST-L3-014 · recall
What is WireMock useful for?

<details><summary>Answer</summary>

A programmable HTTP server for tests: stub responses for downstream APIs, simulate errors (500, 503), delays and dropped connections, and verify what requests were sent. It lets you test timeouts, retries, circuit breakers and error mapping without the real service.

</details>

### TEST-L3-015 · scenario
How do you test a Flyway migration that changes a big table?

<details><summary>Answer</summary>

Run all migrations from scratch in CI (an empty database through Testcontainers), test the migrated schema with the application's integration tests, and for risky changes run the migration against a production-sized copy (or an anonymised snapshot) to measure locks and duration. Test both the expand and contract steps with the old and new application versions.

</details>

### TEST-L3-016 · why
Why must every wait in a test be bounded?

<details><summary>Answer</summary>

An unbounded wait (`future.get()`, `latch.await()`, polling without a timeout) turns a bug into a hung build that blocks CI until a global timeout kills it, with no useful failure message. Bounded waits fail fast with a clear assertion error.

</details>

### TEST-L3-017 · recall
What are smoke tests and synthetic monitoring?

<details><summary>Answer</summary>

Smoke tests: a few quick checks right after a deployment (the service starts, health is up, a core flow works) that gate the rollout. Synthetic monitoring: the same kind of scripted checks run continuously against production, alerting when a key user journey breaks even without user traffic.

</details>

### TEST-L3-018 · bug
A test sets `Locale.setDefault(Locale.GERMANY)` to test number formatting and never resets it. What happens?

<details><summary>Answer</summary>

Every later test in the same JVM runs with a German locale: formatting of decimals ("1,5"), parsing and string comparisons change, so unrelated tests fail depending on order. Restore global state in `@AfterEach`/try-finally, or better, pass the Locale explicitly to the code under test.

</details>

### TEST-L3-019 · interview
"How do you test an event-driven flow: order placed → outbox → Kafka → email service?"

<details><summary>Answer</summary>

Unit test the handlers' logic. Integration test the producer side: placing an order commits the order and the outbox row in one transaction (real PostgreSQL), and the relay publishes it to a real broker (Testcontainers Kafka), with polling assertions. Test the consumer with a real broker too, including duplicates (idempotency) and poison messages. Agree on the event schema with contract tests, and have one end-to-end smoke test in a staging environment.

</details>

### TEST-L3-020 · dsa
A test is flaky with failure probability p = 2% per run, and a CI pipeline runs 50 such independent tests. What is the probability that a pipeline run is red without any real bug?

<details><summary>Answer</summary>

1 − (1 − 0.02)^50 = 1 − 0.98^50 ≈ 1 − 0.364 ≈ 64%. Small per-test flakiness compounds into mostly-red pipelines, which is why flaky tests must be fixed rather than tolerated.

</details>
