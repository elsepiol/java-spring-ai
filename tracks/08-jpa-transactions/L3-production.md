# JPA and transactions, L3: propagation, locking, events and consistency in production

**Goal:** control transaction boundaries precisely (propagation, isolation, timeouts), handle concurrent modification (optimistic and pessimistic locking, retries), keep side effects consistent with the database (transactional events, the outbox), and run JPA at scale.

Prerequisite: L1, L2, SQL L3, Spring core L3 (proxies). Katas: `cd katas && ./mvnw test -Dgroups="jpa & L3"`. Drill: `java quiz/Quiz.java --track jpa-transactions --level 3`.

---

## 1. How Spring transactions work

`@Transactional` → a proxy → `TransactionInterceptor` → `PlatformTransactionManager` (`JpaTransactionManager`): it begins a transaction, binds the `EntityManager` and JDBC connection to the **current thread**, calls your method, then commits or rolls back. Everything that uses the same thread (repositories, `JdbcTemplate` on the same DataSource) joins that transaction. Consequences:
- A new thread (`@Async`, `CompletableFuture`, a parallel stream) runs **outside** the caller's transaction.
- Self-invocation bypasses the proxy (Spring core L3).
- `TransactionTemplate` gives the same programmatically, when annotations do not fit (loops of independent units, retries).

---

## 2. Propagation

| Propagation | If a transaction exists | If none |
|---|---|---|
| `REQUIRED` (default) | join it | start one |
| `REQUIRES_NEW` | **suspend** it, run in a new, independent one, then resume | start one |
| `NESTED` | savepoint inside it (JDBC savepoints; the JPA persistence context is not rolled back with it) | start one |
| `SUPPORTS` | join | run without |
| `NOT_SUPPORTED` | suspend, run without | run without |
| `MANDATORY` | join | **throw** |
| `NEVER` | **throw** | run without |

- **REQUIRES_NEW** for work that must commit regardless of the caller: audit logs of failed attempts, idempotency records, "this job ran" markers. It uses a **second connection** while the first is suspended: with a small pool, nested REQUIRES_NEW under load can **deadlock the pool** (every thread holds one connection and waits for a second).
- **Joined transactions share fate:** if an inner `REQUIRED` method throws a RuntimeException, the shared transaction is marked **rollback-only**; if the outer method catches it and returns normally, the commit fails with `UnexpectedRollbackException`.
- Isolation (`@Transactional(isolation = REPEATABLE_READ)`) and timeouts (`timeout = 5`) are set per transaction; the SQL track L3 explains the anomalies.

> **Kata `TransferService`**: REQUIRES_NEW that silently did nothing (self-invocation), so audits vanished with the rollback.

---

## 3. Concurrency control with JPA

**Optimistic locking** (`@Version`): Hibernate adds `WHERE version = ?` to updates and increments the version; zero rows updated → `OptimisticLockException` (Spring: `ObjectOptimisticLockingFailureException`). Right for most web edits (conflicts rare, no locks held, works across requests with the version in the form or an `ETag`). Handle it by returning **409/412** to the client, or by **retrying the whole transaction** for automated processes (re-read, re-apply).

**Pessimistic locking:** `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a repository method → `SELECT ... FOR UPDATE`. Right for short critical sections with frequent conflicts (allocating the last seats). Add a lock timeout hint, lock in a consistent order, keep the transaction short.

**Or no lock:** a single conditional `UPDATE` (`@Modifying @Query("update Inventory i set i.quantity = i.quantity - :q where i.id = :id and i.quantity >= :q")`) is often the simplest correct answer (SQL track L3, the shop's inventory).

> **Kata `OptimisticLocking`**: `@Version`, two stale forms, and translating the exception for the API.

---

## 4. Side effects and consistency

Database transaction and side effects (emails, messages, HTTP calls, cache evictions) are not atomic together:
- Sending inside the transaction, then rolling back → an email for an order that does not exist.
- Sending after commit in memory (`@TransactionalEventListener(AFTER_COMMIT)`) → lost if the process dies between commit and send.
- **Transactional outbox:** write the event into an `outbox` table **in the same transaction**; a separate worker publishes it and marks it done (at-least-once; consumers deduplicate). This is what the shop's Level 5 built (`FOR UPDATE SKIP LOCKED` worker).
- Cache eviction after commit (the shop orders `@CacheEvict` outside `@Transactional`).

---

## 5. JPA at scale: what to watch

- **Statement count per request** (N+1), measured in tests and production traces.
- **Connection hold time:** transactions doing slow work hold connections; long transactions also block vacuum.
- **Memory:** big result sets as entities; stream or page, clear the persistence context in batch jobs.
- **Second-level cache** (Hibernate L2 cache): shared across transactions; useful for read-mostly reference data; invalidation across instances is the hard part. Often an application cache (Caffeine/Redis) on DTOs is simpler.
- **Schema changes:** Flyway migrations with expand/contract (SQL L3); entity changes deployed with backward-compatible migrations.
- **Startup validation:** `ddl-auto=validate` catches mapping/schema drift before traffic.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `TransferService` (bug) | propagation, REQUIRES_NEW, self-invocation, real commits in tests |
| `OptimisticLocking` | `@Version`, detached entities and merge, exception translation |

```
cd katas && ./mvnw test -Dgroups="jpa & L3"
```

## 7. Seen in the shop project

- `@Version` on `Product` + ETag/If-Match (Level 4); conditional inventory update (Level 3).
- Outbox table written in the order transaction, a worker with `FOR UPDATE SKIP LOCKED`, idempotent consumers (Level 5).
- Cache evictions ordered outside the transaction (`CacheConfig`).

## 8. Interview talk track

"A Spring transaction is bound to the thread by a proxy-based interceptor, so self-calls, private methods and work on other threads run outside it. I put @Transactional on service methods that form a business operation, readOnly for queries, and keep remote calls out. I know the propagation modes: REQUIRES_NEW for audit or idempotency records that must survive the caller's rollback, keeping in mind it needs a second connection; and that a failing inner REQUIRED method marks the whole transaction rollback-only. For concurrent edits I use @Version and turn the conflict into a 409, pessimistic locks for short hot sections, or a single conditional update. Side effects go through a transactional outbox rather than being sent inside the transaction. Performance-wise, I watch statement counts for N+1, keep open-in-view off, use projections for reads, and batch writes with sequence ids."

**Questions you should be able to answer:**
1. Explain REQUIRED vs REQUIRES_NEW vs NESTED with a use case each.
2. Why can REQUIRES_NEW deadlock a connection pool?
3. What is `UnexpectedRollbackException`, and when does it happen?
4. How does `@Version` work at the SQL level? What do you do when it fails?
5. When would you use a pessimistic lock in JPA, and how?
6. Why is sending an email inside a `@Transactional` method risky? What does the outbox pattern solve?
7. Does `@Transactional` apply to code running in `@Async` methods or `CompletableFuture`s started inside it?
8. What is the Hibernate second-level cache, and why is it often not the first choice?
