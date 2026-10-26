# Track 08: JPA and transactions

JPA/Hibernate with Spring Data on a real PostgreSQL (schema owned by Flyway): mapping and the persistence context, then the classic performance and correctness traps, then transaction boundaries and concurrency control in production.

| Level | Lesson | Katas (`katas/src/main/java/dojo/jpa/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): the layers, entities, relationships, the persistence context, repositories and queries | `l1/mapping` (Mapping), `l1/queries` (Queries) |
| L2 Practical | [L2-practical.md](L2-practical.md): N+1, LazyInitializationException and open-in-view, `@Transactional` basics, projections, batching | `l2/nplus1` (OrderTotals, bug), `l2/lazy` (CatalogService, bug) |
| L3 Production | [L3-production.md](L3-production.md): how transactions work, propagation, optimistic and pessimistic locking, side effects and the outbox, JPA at scale | `l3/propagation` (TransferService, bug), `l3/locking` (OptimisticLocking) |

```
cd katas && ./mvnw test -Dgroups="jpa & L1"
java quiz/Quiz.java --track jpa-transactions
```

The katas start an embedded PostgreSQL (or use `DOJO_DB_URL`) and apply `katas/src/main/resources/db/migration/V1__jpa_katas.sql`; Hibernate runs with `ddl-auto=validate`, and the SQL log is on so you can read every statement.

Question bank: [questions.md](questions.md) (60 questions).
