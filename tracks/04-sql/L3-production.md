# SQL and databases, L3: concurrency, isolation, migrations and operations

**Goal:** keep data correct when many requests change it at once (lost updates, write skew, deadlocks), pick an isolation level knowingly, change a live schema without downtime, and keep a production database healthy (pools, timeouts, vacuum, slow queries, replicas). This is what the Level 3-5 lessons of the shop project used, explained from the database's side.

Prerequisite: L1, L2, and the concurrency track (L1-L2). Katas: `cd katas && ./mvnw test -Dgroups="sql & L3"`. Drill: `java quiz/Quiz.java --track sql --level 3`.

---

## 1. How PostgreSQL handles concurrent transactions: MVCC

PostgreSQL uses **multi-version concurrency control**. An `UPDATE` does not overwrite a row in place: it writes a **new version** of the row and marks the old one as expired by this transaction. Each transaction reads from a **snapshot**: the versions that were committed when its snapshot was taken.

Consequences:
- **Readers never block writers, writers never block readers.** A long report does not stop orders from being placed.
- **Writers block writers on the same row:** a second `UPDATE` of a row waits until the first transaction commits or rolls back (row lock).
- Old versions pile up and are cleaned by **VACUUM** (autovacuum runs it). A transaction left open for hours ("idle in transaction") prevents cleanup: tables and indexes **bloat**, queries slow down. Kill such sessions; set `idle_in_transaction_session_timeout`.
- Every `UPDATE` writes a full new row version plus index entries, so very frequent updates of the same row (a global counter) are expensive. Use increments on separate rows, or count in batches.

---

## 2. Lost updates and how to prevent them

```
T1: SELECT stock FROM products WHERE id = 1;   -> 10
T2: SELECT stock FROM products WHERE id = 1;   -> 10
T1: UPDATE products SET stock = 9 WHERE id = 1; COMMIT;
T2: UPDATE products SET stock = 9 WHERE id = 1; COMMIT;     two sold, stock says one
```

Under the default isolation level (READ COMMITTED) nothing stops this, because each statement is fine on its own. Three standard fixes, in order of preference:

1. **Atomic conditional update** (no read in the application at all):
   ```sql
   UPDATE products SET stock = stock - 1 WHERE id = 1 AND stock >= 1;   -- 0 rows updated = sold out
   ```
   The second transaction waits for the first's row lock, then **re-evaluates the WHERE on the new version**. Correct across connections, threads and service instances. This is what the shop's `InventoryRepository` does.
2. **Pessimistic locking:** `SELECT ... FOR UPDATE` locks the rows you read until commit; others trying to lock or update them wait. Use when the decision needs logic you cannot express in one statement. Lock in a consistent order (by id) to avoid deadlocks. Variants: `FOR UPDATE NOWAIT` (fail instead of waiting), `FOR UPDATE SKIP LOCKED` (take rows nobody is working on: the standard way to build a job queue on PostgreSQL; the shop's outbox worker uses it).
3. **Optimistic locking** (section 3): no lock, detect the conflict at write time.

> **Kata `StockReservations`**: 20 buyers, 10 units, 20 connections at once.

---

## 3. Optimistic locking: conflicts across HTTP requests

A human opens an edit form, thinks for two minutes, and saves. No transaction may stay open that long, so database locks cannot protect the edit. Instead, each row carries a **version**:

```sql
SELECT price_cents, version FROM products WHERE id = 4;            -- version 7, shown in the form
UPDATE products SET price_cents = 4500, version = version + 1
WHERE id = 4 AND version = 7;                                      -- 0 rows: someone saved in between
```

The API turns "0 rows" into **409 Conflict** (or uses ETags and `If-Match` → **412 Precondition Failed**, shop Level 4), and the client reloads. JPA's `@Version` generates exactly this `WHERE version = ?`.

Optimistic vs pessimistic: optimistic is cheap when conflicts are rare and works across requests; pessimistic is better when conflicts are frequent (retrying would waste work) and the critical section is short and inside one transaction.

> **Kata `OptimisticPrices`**.

---

## 4. Isolation levels and anomalies

| Anomaly | Meaning |
|---|---|
| **Dirty read** | reading another transaction's uncommitted change |
| **Non-repeatable read** | reading the same row twice in one transaction gives different values |
| **Phantom** | re-running a query returns new rows that others inserted meanwhile |
| **Lost update** | two read-modify-write cycles overwrite each other (section 2) |
| **Write skew** | two transactions read overlapping data, then each writes a *different* row based on what it read; each is fine alone, together they break a rule |

| Level (SQL standard) | PostgreSQL behaviour |
|---|---|
| READ UNCOMMITTED | treated as READ COMMITTED: PostgreSQL never shows dirty data |
| **READ COMMITTED** (default) | each **statement** sees a fresh snapshot of committed data. Non-repeatable reads and phantoms are possible across statements |
| REPEATABLE READ | one snapshot for the **whole transaction** (snapshot isolation): no non-repeatable reads, no phantoms. Concurrent updates of the same row fail with a serialization error. **Write skew is still possible** |
| SERIALIZABLE | Serializable Snapshot Isolation: the result is guaranteed to equal *some* serial order; PostgreSQL detects dangerous read/write patterns and aborts one transaction with **SQLState 40001** |

**Write skew, the classic:** a hospital requires at least one doctor on call per shift. Alice and Bob are both on call; both open "go off call" at the same moment. Each transaction checks "2 doctors on call, so I may leave", each updates **its own** row, both commit: nobody on call. No row was updated twice, so no row lock helped. Fixes:
- **Lock what the decision reads**: `SELECT ... FROM doctors WHERE shift_id = ? FOR UPDATE` (all doctors of the shift), so the second transaction waits and re-checks. ("Materialise the conflict.")
- **SERIALIZABLE** plus a retry loop around the whole transaction on 40001.
- A **constraint** that expresses the rule, when one exists (unique indexes, exclusion constraints, a counter row updated with a CHECK).

**Retrying is part of the contract.** At REPEATABLE READ or SERIALIZABLE, and whenever deadlocks are possible (SQLState **40P01**), the application must be ready to roll back and re-run the *whole* transaction (a few attempts, with backoff). Spring Retry or a simple loop around a `@Transactional` method.

**Deadlocks in the database** look like the concurrency track's: T1 locks row A then wants B, T2 locks B then wants A. PostgreSQL detects the cycle after `deadlock_timeout` (1 s) and aborts one with 40P01. Prevent with a consistent lock order (update rows sorted by id), keep transactions short, and retry.

> **Kata `OnCall`**: the doctors' write skew, forced deterministically by a barrier.

---

## 5. Connections, pools and timeouts

- A PostgreSQL connection is a **server process** (several MB of memory). Hundreds of connections hurt the database more than they help: past a point, throughput drops.
- Applications use a **pool** (HikariCP in Spring Boot): a fixed set of open connections that requests borrow and return. Sizing is counter-intuitive: **small pools are faster**. A starting point from the PostgreSQL wiki: `connections ≈ cores × 2 + effective disks`, for the database server, shared by **all** instances (10 pods × pool 10 = 100 connections).
- `connection-timeout` (how long a request waits for a free connection, then fails) turns "the pool is exhausted" into a fast, visible error instead of hanging threads.
- Timeouts at every level: `statement_timeout` (kill runaway queries), `lock_timeout` (do not queue forever behind a lock, essential for migrations), `idle_in_transaction_session_timeout`.
- Many services or serverless functions: put **PgBouncer** in front (transaction pooling), so thousands of clients share a few server connections.
- A connection that is not returned (forgotten `close()`, a transaction left open in an exception path) is a **pool leak**: after N leaks every request waits for a connection. Hikari's `leakDetectionThreshold` logs where it was borrowed.

---

## 6. Schema migrations without downtime

Schema changes are versioned scripts (**Flyway**: `V1__create_products.sql`, `V2__...`), applied in order, each once, recorded in `flyway_schema_history`, and run automatically at application start or as a deploy step. Never edit a migration that has run anywhere; add a new one.

During a **rolling deploy**, old and new application versions run **at the same time** against the same schema. So every migration must be compatible with both. That is **expand / contract**:

1. **Expand:** add things in a backward-compatible way (new nullable column or column with a default, new table, new index). Old code ignores them.
2. **Migrate:** deploy code that writes both old and new; backfill old rows in batches.
3. **Contract:** once no running version uses the old thing, remove it in a later release.

Renaming a column is therefore *add new, write both, backfill, read new, drop old* across several releases, never one `RENAME` (old pods would fail instantly).

**Locks matter more than the SQL.** Most `ALTER TABLE` forms take an `ACCESS EXCLUSIVE` lock: no reads or writes while it runs. Worse, while it **waits** for that lock (behind a long transaction), every new query on the table queues behind it: a short migration can freeze the application. Always `SET lock_timeout = '3s'` in migrations and retry.

| Change | On a large table |
|---|---|
| `ADD COLUMN` nullable, or with a non-volatile `DEFAULT` (PG 11+) | instant (catalog only) |
| `ADD COLUMN` with a volatile default (`gen_random_uuid()`, `clock_timestamp()`) | rewrites the table (each row needs its own value). `DEFAULT now()` is stable, so it is instant |
| `ALTER COLUMN ... TYPE` (most changes) | rewrites the table and its indexes |
| `SET NOT NULL` | scans the whole table under an exclusive lock (skipped if a validated `CHECK (col IS NOT NULL)` exists) |
| `ADD CONSTRAINT ... CHECK/FOREIGN KEY` | scans under a strong lock. Instead: `... NOT VALID` (instant, enforced for new rows), then `VALIDATE CONSTRAINT` (scans with a weak lock) |
| `CREATE INDEX` | blocks writes for the duration. Instead: `CREATE INDEX CONCURRENTLY` (slower, no write lock; cannot run in a transaction) |
| `DROP COLUMN` | instant, but only after no code reads it |

> **Kata `add_customer_status`**: an expand migration that the still-running old version survives.

---

## 7. Scaling a relational database

In the order you should reach for them:
1. **Fix the queries:** `pg_stat_statements` shows which statements take the most total time; EXPLAIN them; add or fix indexes; remove N+1 query patterns in the application.
2. **Cache** hot, rarely changing reads (shop Level 4).
3. **Read replicas:** streaming replication copies every change to replicas that serve read-only queries. Replication is asynchronous by default, so a replica can be behind (**replication lag**): a user who just placed an order may not see it on the replica. Route "read your own writes" to the primary (shop Level 5).
4. **Partitioning:** split a huge table by time or key (`PARTITION BY RANGE (created_at)`); queries touch only relevant partitions; dropping a month of old data is instant.
5. **Sharding:** different rows on different database servers by a shard key (customer id). Cross-shard joins and transactions become hard; it is a last resort (or a distributed SQL database: Citus, CockroachDB, Spanner).

Operations basics: backups you have actually restored (point-in-time recovery from base backups + WAL), monitoring of connections, replication lag, longest transaction, locks (`pg_locks`, `pg_stat_activity`), disk usage, and autovacuum activity.

---

## 8. Katas for this level

| Kata | Practises |
|---|---|
| `StockReservations` | lost updates, atomic conditional UPDATE, row locks |
| `OptimisticPrices` | version column, compare-and-set, 409 Conflict |
| `OnCall` (bug) | write skew, SELECT FOR UPDATE or SERIALIZABLE + retry, testing an interleaving |
| `add_customer_status` | expand migration, instant ADD COLUMN, NOT VALID + VALIDATE, partial index |

```
cd katas && ./mvnw test -Dgroups="sql & L3"
```

## 9. Seen in the shop project

- Conditional update against overselling: `InventoryRepository` (Level 3).
- Idempotency keys enforced by a unique constraint, plus the "same key, different request" check: `OrderService`, `V3__orders_and_inventory.sql`.
- Optimistic locking with `@Version` and ETags (`If-Match` → 412): `Product`, `V4__product_version_and_price_index.sql` (Level 4).
- `FOR UPDATE SKIP LOCKED` job queue: the outbox worker (`outbox/`, Level 5).
- Read replica routing and read-your-writes: `db/ReadReplicaConfig.java` (Level 5).
- Pool sizing and timeouts: `spring.datasource.hikari.*` in `application.properties` (Level 4).

## 10. Interview talk track

"I keep correctness in the database, not in the JVM, because the service runs as several instances. For counters and stock I use a single conditional UPDATE and check the row count; where a decision needs several reads I lock the rows with SELECT FOR UPDATE in a consistent order; for edits that span HTTP requests I use optimistic locking with a version column and return 409 or 412. I know PostgreSQL's isolation levels: READ COMMITTED by default, REPEATABLE READ as snapshot isolation, which still allows write skew, and SERIALIZABLE, which detects it but requires retrying on 40001; deadlocks also abort with an error the application must retry. Pools stay small, with connection, statement and lock timeouts. Schema changes go through Flyway with expand and contract, because old and new versions run side by side during a rolling deploy; I check each statement's lock level, use NOT VALID constraints, CREATE INDEX CONCURRENTLY and a lock_timeout. To scale, I fix queries first with pg_stat_statements and EXPLAIN, then cache, then read replicas with read-your-writes routing, then partitioning."

**Questions you should be able to answer:**
1. What is MVCC? Why do readers not block writers in PostgreSQL? What does a long-open transaction cost?
2. Show a lost update with two transactions. Give three ways to prevent it.
3. `FOR UPDATE` vs `FOR UPDATE SKIP LOCKED` vs `NOWAIT`: when do you use each?
4. Optimistic vs pessimistic locking: trade-offs and where each fits.
5. The four isolation levels and the anomalies each allows; how PostgreSQL implements them.
6. What is write skew? Give an example and two fixes.
7. What do SQLStates 40001 and 40P01 mean, and what must the application do?
8. How do you size a connection pool? Why are small pools often faster?
9. What is expand/contract? How do you rename a column with zero downtime?
10. Which ALTER TABLE operations are dangerous on a large table, and what are the safe alternatives?
11. Why does a migration waiting for a lock freeze the application, and what prevents it?
12. Replication lag: what problem does it cause and how do you route around it?
