# Testing, L1: JUnit, AssertJ, testable code, Mockito

**Goal:** write clear, fast unit tests with JUnit 6 and AssertJ, design code so it can be tested (time, randomness and I/O as dependencies), and use Mockito mocks correctly, without testing the mocks instead of the code.

Prerequisite: Java core L1. Katas: `cd katas && ./mvnw test -Dgroups="testing & L1"`. Drill: `java quiz/Quiz.java --track testing --level 1`.

---

## 1. Why test, and what a good test looks like

Tests let you change code without fear: refactoring, upgrades and new features with a safety net, plus documentation that cannot go stale. A good unit test is:
- **Fast** (milliseconds) and **isolated** (no order dependence, no shared state, no network);
- **Deterministic** (same result every run: no real clock, no randomness, no sleeps);
- **Readable**: one behaviour per test, named after it (`refusesTakenEmails`), structured **Arrange / Act / Assert** (Given / When / Then);
- **Testing behaviour, not implementation**: assert on results and observable effects; a refactoring that keeps behaviour must not break tests.

A test that never fails is worthless; check that it fails for the right reason (write it first, or break the code once and watch it go red). That is exactly how every kata in this dojo works.

---

## 2. JUnit 6 essentials

```java
@Tag("fast")
class PriceCalculatorTest {
    private PriceCalculator calc;

    @BeforeEach void setUp() { calc = new PriceCalculator(); }        // a fresh instance per test (JUnit also creates a new test-class instance per test)

    @Test
    void appliesTenPercentForGold() {
        long price = calc.price(10_000, Tier.GOLD);                 // act
        assertThat(price).isEqualTo(9_000);                         // assert
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({ "BASIC, 10000", "SILVER, 9500", "GOLD, 9000" })
    void pricesByTier(Tier tier, long expected) {
        assertThat(calc.price(10_000, tier)).isEqualTo(expected);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatIllegalArgumentException().isThrownBy(() -> calc.price(-1, Tier.GOLD)).withMessageContaining("negative");
    }
}
```

- Lifecycle: `@BeforeAll`/`@AfterAll` (static, once per class), `@BeforeEach`/`@AfterEach`.
- `@ParameterizedTest` with `@CsvSource`, `@ValueSource`, `@MethodSource`, `@EnumSource`: one test, many cases.
- `@Nested` groups related tests; `@DisplayName`; `@Disabled` (with a reason and a ticket, never permanently); `@Tag` to select subsets (the dojo's `-Dgroups="testing & L1"`); `@TempDir` for files; `@Timeout`.
- Extensions (`@ExtendWith`) plug in Mockito, Spring and more.

---

## 3. AssertJ

Fluent, readable assertions with good failure messages:

```java
assertThat(order.lines()).hasSize(2).extracting(Line::sku).containsExactly("A", "B");
assertThat(user.email()).isEqualTo("ada@example.com");
assertThat(result).isPresent().get().extracting(User::name).isEqualTo("ada");
assertThatThrownBy(() -> service.pay(order)).isInstanceOf(PaymentDeclined.class).hasMessageContaining("card");
assertThat(price).as("discounted price").isBetween(8_000L, 9_000L);
```

Prefer one precise assertion over `assertTrue(list.size() == 2)`, which fails with "expected true". `as(...)` labels a failure; `usingRecursiveComparison()` compares object graphs; `SoftAssertions` reports several failures at once.

---

## 4. Designing for testability: seams

Code is hard to test when it reaches out for things: `Instant.now()`, `new Random()`, `UUID.randomUUID()`, a static `HttpClient`, `System.getenv`. The fix is the same as in Spring core: **make it a dependency** and inject it.

```java
public TrialPeriod(Instant signedUpAt, Clock clock) { ... }        // production: Clock.systemUTC() as a bean
boolean isActive() { return clock.instant().isBefore(signedUpAt.plus(LENGTH)); }

// test
new TrialPeriod(signup, Clock.fixed(Instant.parse("2026-03-15T11:59:59Z"), ZoneOffset.UTC));
```

- `java.time.Clock` for time (`Clock.fixed`, `Clock.offset`, or a mutable test clock as the katas use); a `Supplier<UUID>` or `IdGenerator` for ids; a seeded `Random` or `RandomGenerator` parameter; interfaces around I/O (`Mailer`, `PaymentGateway`).
- A **seam** is a place where you can change behaviour without editing the code: a constructor parameter is the simplest one.
- Keep logic in **pure functions** (input → output, no side effects) and push I/O to the edges: most of the code then needs no mocks at all ("functional core, imperative shell").

> **Kata `TrialPeriod`**: turn an untestable class into a testable one by using the clock it was already given.

---

## 5. Test doubles and Mockito

| Double | What it is | Example |
|---|---|---|
| **Dummy** | passed but never used | a `null`-safe placeholder |
| **Stub** | returns canned answers | `when(repo.find(1)).thenReturn(Optional.of(ada))` |
| **Mock** | records calls so you can verify interactions | `verify(mailer).send(any())` |
| **Spy** | wraps a real object, records calls | `spy(realList)` (rarely needed) |
| **Fake** | a working lightweight implementation | an in-memory repository (L2) |

```java
@ExtendWith(MockitoExtension.class)            // strict stubs: unused stubbings fail the test
class SignupServiceTest {
    @Mock UserAccounts accounts;
    @Mock Mailer mailer;
    @Captor ArgumentCaptor<Mailer.Email> mail;

    @Test void sendsAWelcomeMail() {
        when(accounts.create("ada@example.com", "hashed")).thenReturn(42L);   // stub
        new SignupService(accounts, mailer, p -> "hashed").signUp("ada@example.com", "long enough pw");
        verify(mailer).send(mail.capture());                                   // exactly once
        assertThat(mail.getValue().body()).contains("42");
    }
}
```

- `when(...).thenReturn/thenThrow/thenAnswer`; `doThrow(...).when(mock).voidMethod()` for void methods.
- `verify(mock, times(2) / never() / atLeastOnce())`, `verifyNoInteractions`, `verifyNoMoreInteractions` (use sparingly), `InOrder`.
- Matchers: `any()`, `eq(...)`, `argThat(...)`; if one argument uses a matcher, all must.
- **Mock roles, not values**: mock collaborators at boundaries (mail, payment, external APIs, sometimes repositories); use real value objects, records, collections and pure helpers.
- **Don't over-mock**: a test that stubs five calls and verifies seven mostly re-states the implementation and breaks on every refactoring. Verify interactions only when the interaction *is* the behaviour (an email sent, a payment charged, nothing called on invalid input).
- Mockito cannot mock final classes and statics without the inline mock maker (the default since Mockito 5) and you should rarely need to: inject instead.

> **Kata `SignupService`**: implement a service from its Mockito tests: stubs, captors, `never()`, `verifyNoInteractions`, a failing collaborator.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `TrialPeriod` | seams, `Clock.fixed`/`Clock.offset`, parameterised boundary tests |
| `SignupService` | reading and satisfying Mockito tests, interaction vs state verification |

```
cd katas && ./mvnw test -Dgroups="testing & L1"
```

## 7. Seen in the shop project

- A `Clock` bean (`JwtConfig`) injected into `AuthService` and token issuing, so time is a dependency rather than `Instant.now()`.
- Mockito unit tests (`AuthServiceTest`, `ProductServiceTest`), `@WebMvcTest` slices (`PingControllerTest`), and many integration tests on a real PostgreSQL.

## 8. Interview checklist

1. What makes a unit test good (FIRST: fast, isolated, repeatable, self-validating, timely)?
2. Arrange/Act/Assert. One behaviour per test: why?
3. How do you test code that uses the current time or random values?
4. Stub vs mock vs fake vs spy.
5. When should you verify interactions, and when only results?
6. What goes wrong when you mock too much?
7. What does Mockito's strict stubbing catch?
8. Parameterised tests: when do they help?
