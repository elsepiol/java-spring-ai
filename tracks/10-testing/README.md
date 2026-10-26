# Track 10: Testing

Testing as a professional skill: good unit tests and testable code, then choosing the right test level and Spring's test support, then real infrastructure, async code, flaky tests and fast suites.

| Level | Lesson | Katas (`katas/src/main/java/dojo/testing/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): what makes a good test, JUnit 6, AssertJ, seams for time and randomness, test doubles and Mockito | `l1/TrialPeriod` (a Clock seam), `l1/SignupService` (implement it from Mockito tests) |
| L2 Practical | [L2-practical.md](L2-practical.md): the test pyramid, fakes kept honest by contract tests, test data builders, Spring test slices, `@MockitoBean`, context caching | `l2/InMemoryUserAccounts` (a fake + contract), `l2/OrderBuilder` |
| L3 Production | [L3-production.md](L3-production.md): Testcontainers, isolating integration tests, async and concurrency tests, flaky tests, coverage vs mutation testing, fast CI | `l3/DatabaseCleaner` (real PostgreSQL), `l3/Eventually` (poll, don't sleep), `l3/FeatureFlags` (bug) |

```
cd katas && ./mvnw test -Dgroups="testing & L1"
java quiz/Quiz.java --track testing
```

Every kata in the dojo is itself a test-first exercise: the tests are written, you make them pass. In this track, reading the tests is half of the kata.

Question bank: [questions.md](questions.md) (60 questions).
