# Testing, L2: the test pyramid, fakes and contracts, builders, Spring test slices

**Goal:** choose the right kind of test for each risk, use fakes kept honest by contract tests, write readable test data, and use Spring's test support (slices, MockMvc, `@MockitoBean`, context caching) without making the suite slow.

Prerequisite: L1, Spring Boot web L1. Katas: `cd katas && ./mvnw test -Dgroups="testing & L2"`. Drill: `java quiz/Quiz.java --track testing --level 2`.

---

## 1. The test pyramid (and its critics)

```
        /  E2E / UI  \          few: slow, brittle, but test what users do
       / integration  \         some: real DB, real HTTP layer, real Spring context
      /     unit       \        many: fast, focused on logic
```

- **Unit**: one class or a small group, collaborators replaced; milliseconds.
- **Integration**: real infrastructure for the part under test: repository + real PostgreSQL, controller + MVC stack, a Kafka consumer + broker. The bugs live at the boundaries (SQL, mappings, serialisation, transactions, security config), so these tests catch what mocks cannot.
- **End-to-end**: the deployed system through its public interface (HTTP, browser). A few smoke paths.
- The "testing trophy" (Kent C. Dodds) argues for more integration tests in web apps; the real rule is **cost vs confidence**: test each risk at the cheapest level that can actually catch it. A repository query mocked in a unit test proves nothing about the SQL.

---

## 2. Fakes and contract tests

A **fake** is a working implementation that is cheap to run: an in-memory repository, a local file store, a fake payment gateway that remembers charges. Fakes make tests readable (state, not stubbing scripts) and survive refactoring better than mocks.

The danger: the fake **drifts** from the real thing (the real database has a unique constraint; the fake happily stores duplicates). The cure is a **contract test**: one abstract test class describing the behaviour of the interface, run against every implementation:

```java
abstract class UserAccountsContract {
    protected abstract UserAccounts newAccounts();
    @Test void duplicateEmailsAreRejected() { ... }
}
class InMemoryUserAccountsTest extends UserAccountsContract { UserAccounts newAccounts() { return new InMemoryUserAccounts(); } }
class JdbcUserAccountsIT      extends UserAccountsContract { UserAccounts newAccounts() { return new JdbcUserAccounts(realDataSource); } }
```

The same idea across services is **consumer-driven contract testing** (Pact, Spring Cloud Contract): the consumer records what it expects from an API, and the provider's build verifies it, so a breaking API change fails before deployment.

> **Kata `InMemoryUserAccounts`**: a fake that passes the contract, including concurrency.

---

## 3. Test data that reads well

Tests drown in setup when domain objects need ten valid fields. Patterns:
- **Test data builder**: `anOrder().paid().withLine("A", 2, 500).build()`: valid defaults, tests state only what matters. `but()` derives variants.
- **Object mother**: named factories (`Orders.paidOrderWithTwoLines()`); simple, but tends to grow many near-duplicates.
- Keep defaults **valid and boring**; a test relying on a default value should set it explicitly.
- Avoid shared mutable fixtures across tests; builders make fresh objects cheap.

> **Kata `OrderBuilder`**: a fluent test data builder with defaults and `but()`.

---

## 4. Spring test support

| Annotation | Starts | Use for |
|---|---|---|
| `@SpringBootTest` | the whole application context (optionally a real server: `webEnvironment = RANDOM_PORT`) | end-to-end within the service, wiring, configuration |
| `@WebMvcTest(XController.class)` | MVC infrastructure + the controller (+ advice, filters, converters, security) | request mapping, validation, status codes, JSON, error handling |
| `@DataJpaTest` | JPA, repositories, a DataSource, Flyway; **transactional per test** | queries, mappings |
| `@JsonTest`, `@RestClientTest`, `@JdbcTest` | narrower slices | serialisation, HTTP clients, JDBC |

- **`@MockitoBean`** (Spring Framework 6.2+, replacing Boot's deprecated `@MockBean`) replaces a bean in the context with a Mockito mock; `@MockitoSpyBean` wraps one.
- **MockMvc** runs the MVC stack in-process (`perform(get(...)).andExpect(status().isOk())`); `RestTestClient`/`TestRestTemplate`/`WebTestClient` call a real port.
- **Context caching**: Spring caches application contexts across test classes with the *same configuration*. Every distinct combination of `@MockitoBean`s, properties or profiles creates a new context (seconds each). Standardise test configurations (base classes) and avoid `@DirtiesContext` unless needed; a suite that starts 40 contexts is slow for that reason.
- `@Transactional` on tests rolls back after each test: convenient, but it hides commit-time behaviour (constraints deferred to commit, `@TransactionalEventListener`, lazy loading outside transactions: the JPA track's CatalogService kata).
- `@DynamicPropertySource` points the context at infrastructure started by the test (Testcontainers, the dojo's embedded PostgreSQL).

---

## 5. What to test where (a Spring service)

| Risk | Test |
|---|---|
| business rules, calculations | unit tests (no Spring) |
| JSON shape, status codes, validation, error format | `@WebMvcTest` |
| security rules (401/403 per route) | `@WebMvcTest` with the security config, or `@SpringBootTest` + MockMvc |
| SQL, mappings, N+1, migrations | `@DataJpaTest` / repository tests on the real database engine (never H2 for PostgreSQL code) |
| transactions, events, outbox, wiring | `@SpringBootTest` against real infrastructure |
| the service works when deployed | a few smoke tests against the running container |

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `InMemoryUserAccounts` | fakes, abstract contract tests, thread safety |
| `OrderBuilder` | test data builders, valid defaults, copying |

```
cd katas && ./mvnw test -Dgroups="testing & L2"
```

## 7. Seen in the shop project

- `@WebMvcTest` slices with a shared `WebSliceSecurityConfig`, and `TestUsers` helpers creating `jwt()` post-processors.
- `@SpringBootTest` API tests (`ProductApiTest`, `OrderApiTest`) on a real PostgreSQL through one shared base class (`PostgresIntegrationTest`), so the Spring context is cached and reused; a query-count test (`OrderQueryCountTest`).
- Mockito unit tests where logic is isolated (`AuthServiceTest`, `ProductServiceTest`).
- An `ApiClient` test helper so API tests read like the API's documentation.

## 8. Interview checklist

1. Explain the test pyramid. What is wrong with an inverted pyramid ("ice-cream cone")?
2. Unit vs integration test: where is the line, and what does each catch?
3. Fake vs mock: when do you prefer a fake? How do you keep it honest?
4. What is consumer-driven contract testing?
5. `@SpringBootTest` vs `@WebMvcTest` vs `@DataJpaTest`.
6. `@MockitoBean`: what does it do, and why can many different ones slow a suite down?
7. Why not test PostgreSQL queries on H2?
8. What does `@Transactional` on a test hide?
