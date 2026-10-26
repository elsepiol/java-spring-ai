# SQL and databases: question bank

Drill with `java quiz/Quiz.java --track sql`. The shop data used in "predict" questions is `katas/src/test/resources/sql/shop.sql`.

## L1

### SQL-L1-001 · recall
In which logical order does PostgreSQL evaluate the clauses of a SELECT?

<details><summary>Answer</summary>

FROM (and JOINs) → WHERE → GROUP BY → HAVING → SELECT (expressions, aliases, window functions) → DISTINCT → ORDER BY → LIMIT/OFFSET. That is why WHERE cannot use a SELECT alias or an aggregate, while ORDER BY can use aliases.

</details>

### SQL-L1-002 · recall
Primary key, unique constraint, foreign key: what does each guarantee?

<details><summary>Answer</summary>

**Primary key**: unique and NOT NULL, one per table, identifies the row. **Unique**: no two rows with the same (non-NULL) value; NULLs are allowed (several of them, by default). **Foreign key**: the value must exist in the referenced table's key; deleting or changing the referenced row is refused (or cascades, per `ON DELETE`). All are enforced by the database for every writer, and PK/unique create an index; foreign keys do **not** create one on the referencing column in PostgreSQL.

</details>

### SQL-L1-003 · predict
On the shop data, what does this return for Eve (a customer without orders)?

```sql
SELECT c.name, COUNT(*) FROM customers c LEFT JOIN orders o ON o.customer_id = c.id GROUP BY c.name;
```

<details><summary>Answer</summary>

`Eve | 1`. The LEFT JOIN produces one row for Eve with NULLs in the order columns, and `COUNT(*)` counts rows. Use `COUNT(o.id)`, which skips NULLs, to get 0.

</details>

### SQL-L1-004 · bug
"Customers with no PAID orders, with their number of PAID orders (0)". Why does this return no rows for them?

```sql
SELECT c.name, COUNT(o.id)
FROM customers c LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'PAID'
GROUP BY c.name;
```

<details><summary>Answer</summary>

For customers without paid orders, the joined row has `o.status = NULL`, and `NULL = 'PAID'` is not true, so WHERE removes them: the LEFT JOIN behaves like an INNER JOIN. Move the condition into the join: `LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID'`.

</details>

### SQL-L1-005 · recall
`WHERE` vs `HAVING`?

<details><summary>Answer</summary>

`WHERE` filters **rows** before grouping and cannot use aggregates. `HAVING` filters **groups** after GROUP BY and can use aggregates (`HAVING SUM(total) > 1000`). Filter in WHERE whenever possible: fewer rows to group.

</details>

### SQL-L1-006 · predict
What do these return: `SELECT NULL = NULL;`, `SELECT NULL IS NULL;`, `SELECT 1 WHERE NULL <> 5;`

<details><summary>Answer</summary>

`NULL` (unknown, not true), `true`, and **no row**: `NULL <> 5` is unknown, and WHERE keeps only rows whose condition is true. Use `IS NULL`, `IS DISTINCT FROM`, `COALESCE`.

</details>

### SQL-L1-007 · bug
Why does this return no rows at all, although many customers have no orders?

```sql
SELECT name FROM customers WHERE id NOT IN (SELECT customer_id FROM legacy_orders);
```

<details><summary>Answer</summary>

`legacy_orders.customer_id` contains at least one NULL. `id NOT IN (1, 2, NULL)` means `id <> 1 AND id <> 2 AND id <> NULL`, and the last part is unknown, so the whole condition is never true. Use `NOT EXISTS (SELECT 1 FROM legacy_orders l WHERE l.customer_id = c.id)`, which is NULL-safe (or filter NULLs in the subquery).

</details>

### SQL-L1-008 · recall
INNER, LEFT, RIGHT, FULL and CROSS JOIN: what does each return?

<details><summary>Answer</summary>

INNER: only matching pairs. LEFT: all left rows, plus matches or NULLs. RIGHT: all right rows (mirror of LEFT). FULL: all rows of both, matched where possible, NULLs elsewhere. CROSS: every combination (n × m), no condition. A **self join** joins a table with itself (e.g. employees with their managers).

</details>

### SQL-L1-009 · why
Why is it dangerous to join `customers` to both `orders` and `support_tickets` and then `SUM(order_total)`?

<details><summary>Answer</summary>

Both are one-to-many from customers: the join produces every combination of a customer's orders and tickets, so each order row is repeated once per ticket, and the SUM is multiplied (fan-out). Aggregate each child table separately (subquery or CTE grouped by customer_id), then join the aggregates.

</details>

### SQL-L1-010 · interview
"Customers who placed at least one order": write it with JOIN, IN and EXISTS. Which do you prefer and why?

<details><summary>Answer</summary>

`SELECT DISTINCT c.* FROM customers c JOIN orders o ON o.customer_id = c.id` (needs DISTINCT because of fan-out); `... WHERE c.id IN (SELECT customer_id FROM orders)`; `... WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id)`. EXISTS (or IN) states the intent ("has any"), needs no DISTINCT, stops at the first match; PostgreSQL usually plans IN and EXISTS as the same semi-join. For the negation, use NOT EXISTS (NOT IN breaks with NULLs).

</details>

### SQL-L1-011 · recall
`COUNT(*)`, `COUNT(col)`, `COUNT(DISTINCT col)`: what does each count? What does `SUM` return over zero rows?

<details><summary>Answer</summary>

Rows; non-NULL values of col; distinct non-NULL values. `SUM` (and AVG, MAX...) over zero rows returns **NULL**, not 0: use `COALESCE(SUM(x), 0)`. `COUNT` returns 0.

</details>

### SQL-L1-012 · scenario
A login endpoint builds `"SELECT * FROM users WHERE email = '" + email + "' AND password_hash = '" + hash + "'"`. What can an attacker do, and how do you fix it?

<details><summary>Answer</summary>

SQL injection. Email `admin@shop.com' --` comments out the password check and logs in as admin; `' OR 1=1 --` returns the first user; with stacked statements or UNION they can read other tables or modify data. Fix: a `PreparedStatement` with `?` placeholders (or JPA/JdbcTemplate parameters), so input is sent as data and can never change the SQL structure. Defence in depth: a least-privilege database user. (And compare password hashes in the application with a slow hash like bcrypt, not in SQL.)

</details>

### SQL-L1-013 · recall
What cannot be a `?` parameter in a PreparedStatement, and how do you handle those safely?

<details><summary>Answer</summary>

Identifiers and keywords: table and column names, `ORDER BY` columns, `ASC`/`DESC`, `LIMIT` in some drivers. Map user choices to a fixed allow-list in code (`switch (sort) { case "price" -> "price_cents"; ...}`) and reject everything else. Never concatenate raw input into SQL.

</details>

### SQL-L1-014 · why
Why should business rules like "price is never negative" and "email is unique" be database constraints and not only Java validation?

<details><summary>Answer</summary>

The database sees **every** writer: all instances of the service, batch jobs, migrations, manual fixes. Java validation in one place cannot prevent races either: two concurrent registrations can both pass a "does this email exist?" check, and only a UNIQUE constraint stops the second insert. Validation gives good error messages; constraints guarantee correctness.

</details>

### SQL-L1-015 · predict
What is the order of rows returned by `SELECT name FROM customers WHERE city = 'Berlin';`?

<details><summary>Answer</summary>

**Undefined.** Without ORDER BY, the database may return rows in any order, and the order can change with data, plans, parallel workers, or versions. If order matters (UI, pagination, tests), add an ORDER BY with a unique tie-breaker.

</details>

### SQL-L1-016 · recall
What does `RETURNING` do, and why is it useful from Java?

<details><summary>Answer</summary>

It makes `INSERT`, `UPDATE` or `DELETE` return columns of the affected rows: `INSERT ... RETURNING id, created_at`, `UPDATE ... RETURNING stock`. You get generated keys and new values in the same round trip, without a second query that could race with other writers.

</details>

### SQL-L1-017 · dsa
Write a query that returns the second-highest product price. What if two products share the highest price?

<details><summary>Answer</summary>

`SELECT DISTINCT price_cents FROM products ORDER BY price_cents DESC OFFSET 1 LIMIT 1;` or `SELECT MAX(price_cents) FROM products WHERE price_cents < (SELECT MAX(price_cents) FROM products);`. Both treat equal top prices as one value (DISTINCT / strictly less). With window functions: `DENSE_RANK() OVER (ORDER BY price_cents DESC) = 2`. Clarify in an interview whether "second" means second distinct value or second row.

</details>

### SQL-L1-018 · recall
What does normalisation mean, and when do you deliberately copy data anyway?

<details><summary>Answer</summary>

Store each fact once, so it cannot become inconsistent (a customer's city in `customers`, not repeated in every order). Deliberate copies: historical facts that must not change (the unit price on an order item, the shipping address of a past order), and measured read-performance needs (denormalised counters, read models), with a defined way to keep them in sync.

</details>

### SQL-L1-019 · bug
Why is this JDBC code a production problem?

```java
Connection c = dataSource.getConnection();
PreparedStatement ps = c.prepareStatement("SELECT ...");
ResultSet rs = ps.executeQuery();
// ... read rows, return the list
```

<details><summary>Answer</summary>

Nothing is closed. With a pool, the connection is never returned: after `maximum-pool-size` calls every request waits for a connection and times out (a **connection leak**). Use try-with-resources for Connection, PreparedStatement and ResultSet (or JdbcTemplate/JPA, which manage them).

</details>

### SQL-L1-020 · predict
On the shop data: `SELECT category, COUNT(*) FROM products GROUP BY category HAVING COUNT(*) > 2;`

<details><summary>Answer</summary>

`kitchen | 3`. Living and office have 2 products each (Lamp, Rug; Pen, Desk), kitchen has Mug, Pan, Knife.

</details>

## L2

### SQL-L2-001 · recall
How does a B-tree index make `WHERE id = 42` fast, and what does each index cost?

<details><summary>Answer</summary>

The index stores the column's values sorted in a balanced tree of pages; a lookup walks from the root to a leaf in O(log n) page reads (3-4 for millions of rows) and follows the pointer to the row. Costs: every INSERT, DELETE and UPDATE of indexed columns must update the index (slower writes), plus disk and cache memory. Index for the queries you run.

</details>

### SQL-L2-002 · predict
An index exists on `(customer_id, created_at)`. Which of these can use it efficiently? (a) `WHERE customer_id = 7` (b) `WHERE created_at > now() - interval '1 day'` (c) `WHERE customer_id = 7 AND created_at > ...` (d) `WHERE customer_id = 7 ORDER BY created_at DESC LIMIT 10`

<details><summary>Answer</summary>

(a) yes (leftmost prefix), (b) **no** (the index is ordered by customer first; created_at alone is scattered across it), (c) yes, a range within one customer, (d) yes, and without a sort: read the customer's part of the index backwards and stop after 10. Equality columns first, then the range/sort column.

</details>

### SQL-L2-003 · bug
There is an index on `orders (created_at)`. Why is this query still a sequential scan, and how do you fix it?

```sql
SELECT * FROM orders WHERE date(created_at) = '2026-02-03';
```

<details><summary>Answer</summary>

The function `date(created_at)` is applied to the column, so the index on the raw column cannot be used. Rewrite as a range on the column: `WHERE created_at >= '2026-02-03' AND created_at < '2026-02-04'`. (Or an expression index on `date(created_at)`, if that exact expression is common.)

</details>

### SQL-L2-004 · why
`WHERE status = 'DONE'` matches 95% of a table with an index on status. Why does PostgreSQL ignore the index, and is that wrong?

<details><summary>Answer</summary>

Not wrong. Using the index would mean jumping to almost every table page in random order (index then heap); reading the whole table sequentially is cheaper. Indexes pay off for selective conditions. For the rare value ('PENDING'), a **partial index** `WHERE status = 'PENDING'` is small and fast.

</details>

### SQL-L2-005 · recall
What are covering, partial and expression indexes? A use case for each.

<details><summary>Answer</summary>

**Covering** (`INCLUDE (col)` or all needed columns in the key): the query is answered from the index alone (Index Only Scan), e.g. listing an order's status by customer. **Partial** (`WHERE status = 'PENDING'`): indexes only a subset, e.g. a job queue's pending rows or non-deleted rows. **Expression** (`ON users (lower(email))`): for queries filtering on that exact expression, e.g. case-insensitive email login (as a UNIQUE index it also enforces case-insensitive uniqueness).

</details>

### SQL-L2-006 · recall
In an `EXPLAIN ANALYZE` output, what do Seq Scan, Index Scan, Index Only Scan and Bitmap Heap Scan mean? What does a big gap between estimated and actual rows tell you?

<details><summary>Answer</summary>

Seq Scan: reads the whole table. Index Scan: walks the index and fetches each matching row from the table. Index Only Scan: answers from the index without touching the table (needs a covering index and a mostly vacuumed table). Bitmap: collects matching row locations from the index, then reads table pages in physical order (good for many matches). A big estimate gap means the planner's statistics are wrong (stale, skewed, correlated columns): plans may be bad. `ANALYZE`, raise statistics targets, or extended statistics.

</details>

### SQL-L2-007 · recall
`ROW_NUMBER`, `RANK`, `DENSE_RANK` for the scores 100, 90, 90, 80?

<details><summary>Answer</summary>

ROW_NUMBER: 1, 2, 3, 4 (ties get arbitrary distinct numbers unless the ORDER BY breaks them). RANK: 1, 2, 2, 4 (gap after the tie). DENSE_RANK: 1, 2, 2, 3.

</details>

### SQL-L2-008 · interview
Write "the 3 most expensive products per category".

<details><summary>Answer</summary>

```sql
SELECT category, sku, price_cents FROM (
  SELECT category, sku, price_cents,
         ROW_NUMBER() OVER (PARTITION BY category ORDER BY price_cents DESC, sku) AS rn
  FROM products
) t
WHERE rn <= 3
ORDER BY category, rn;
```

The window function cannot be filtered in WHERE of the same query (it is computed after WHERE), hence the subquery. Use RANK instead of ROW_NUMBER if ties should all be included. (PostgreSQL alternative: a LATERAL subquery with ORDER BY ... LIMIT 3 per category.)

</details>

### SQL-L2-009 · interview
How do you delete duplicate rows (same email, ignoring case), keeping the oldest one?

<details><summary>Answer</summary>

```sql
DELETE FROM newsletter WHERE id IN (
  SELECT id FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(email) ORDER BY id) AS n FROM newsletter
  ) t WHERE n > 1);
```

Then prevent new duplicates: `CREATE UNIQUE INDEX ON newsletter (lower(email))`. On a huge table, delete in batches.

</details>

### SQL-L2-010 · recall
What is the default window frame when `OVER (ORDER BY created_at)` is used with `SUM`, and when does that surprise you?

<details><summary>Answer</summary>

`RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`: from the start up to the current row **including all rows with the same ORDER BY value** (peers). If two orders share a timestamp, both rows show the total including both. Write `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` for a strict row-by-row running total, and make the ORDER BY unique.

</details>

### SQL-L2-011 · why
Why does `LIMIT 20 OFFSET 100000` get slow, and what goes wrong when rows are inserted while a user pages?

<details><summary>Answer</summary>

The database must produce and discard the first 100,000 rows of the sorted result before returning 20: cost grows with the page number. And offsets shift: a row inserted before the current position pushes everything down, so the next page repeats an item (or a deleted row makes one get skipped). Keyset pagination (`WHERE (sort_key, id) > (:last_key, :last_id) ORDER BY sort_key, id LIMIT 20`) costs the same on every page and is stable.

</details>

### SQL-L2-012 · bug
A keyset query uses `WHERE price_cents > :lastPrice ORDER BY price_cents LIMIT 20`. What bug appears in production?

<details><summary>Answer</summary>

When several products share `lastPrice` and a page boundary falls among them, the remaining ones are **skipped** (strictly greater). The sort key must be unique: add the id, `WHERE (price_cents, id) > (:lastPrice, :lastId) ORDER BY price_cents, id`.

</details>

### SQL-L2-013 · scenario
Two requests register the same new email at the same moment. The code does "SELECT by email; if none, INSERT". What happens, and what is the right design?

<details><summary>Answer</summary>

Both SELECTs see no row, both INSERT: a duplicate account (or, with a unique index, one request fails with a unique-violation error, SQLState 23505). Right design: a **UNIQUE constraint** (on `lower(email)`) as the source of truth, just INSERT, and translate 23505 into "email already registered" (409). Or `INSERT ... ON CONFLICT DO NOTHING RETURNING id` and check whether a row came back.

</details>

### SQL-L2-014 · recall
What does `INSERT ... ON CONFLICT (sku) DO UPDATE SET quantity = stock_counters.quantity + EXCLUDED.quantity` do, and what does it require?

<details><summary>Answer</summary>

Inserts the row; if a row with the same `sku` already exists, atomically updates it instead, adding the new quantity (`EXCLUDED` is the row that was proposed for insertion). It requires a unique index or constraint on the conflict target (`sku`). It is safe under concurrency, unlike SELECT-then-INSERT-or-UPDATE.

</details>

### SQL-L2-015 · recall
What does ACID stand for? What is auto-commit in JDBC?

<details><summary>Answer</summary>

Atomicity (all or nothing), Consistency (constraints hold at commit), Isolation (concurrent transactions do not interfere, to a degree set by the isolation level), Durability (committed data survives crashes, via the write-ahead log). Auto-commit (JDBC default): each statement is its own transaction, committed immediately. `setAutoCommit(false)` starts grouping statements until `commit()`/`rollback()`; Spring's `@Transactional` manages this.

</details>

### SQL-L2-016 · why
Why should a transaction never include an HTTP call to another service?

<details><summary>Answer</summary>

The transaction holds row locks and a database connection for the duration of the call: if the service is slow, other requests block on those rows and the pool runs out of connections. If the call succeeds but the commit fails (or vice versa), the two systems disagree, and you cannot roll back the remote side anyway. Do remote calls before or after the transaction, or use the outbox pattern to publish after commit (shop Level 5).

</details>

### SQL-L2-017 · recall
`timestamp` vs `timestamptz` in PostgreSQL: what is stored, and which do you use for "when was this order placed"?

<details><summary>Answer</summary>

`timestamptz` stores an absolute instant (UTC internally) and converts to the session's time zone for display; `timestamp` stores a date and wall-clock time with no zone, which means nothing without knowing where. Events (order placed, login) are instants: `timestamptz`. Use `timestamp` (or `date` + `time` + zone id) only for things like "the shop opens at 09:00 local time".

</details>

### SQL-L2-018 · design
Bigint identity vs UUID as primary keys: trade-offs?

<details><summary>Answer</summary>

**Bigint identity**: 8 bytes, ordered (new rows go to the end of the index: good locality), fast joins, human-friendly; but generated by the database only, and sequential ids leak business information and are guessable. **UUID**: 16 bytes, generated anywhere (clients, offline, multiple databases), not guessable; random v4 UUIDs scatter inserts across the index (more page splits and cache misses). Time-ordered **UUIDv7** combine global uniqueness with good index locality. Common pattern: bigint internally, a UUID or opaque public id in URLs.

</details>

### SQL-L2-019 · dsa
Running total of revenue per day, and a 7-day moving average: write the window clauses.

<details><summary>Answer</summary>

Given a `daily(day, revenue)` CTE: `SUM(revenue) OVER (ORDER BY day ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)` and `AVG(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)`. Days without sales must exist as rows (generate them with `generate_series` and LEFT JOIN), or the "7 rows" are not 7 days; alternatively `RANGE BETWEEN interval '6 days' PRECEDING AND CURRENT ROW` over a date column.

</details>

### SQL-L2-020 · scenario
A new feature's query is fast in development and takes 8 seconds in production. How do you investigate?

<details><summary>Answer</summary>

Get the exact query with real parameters (logs, `pg_stat_statements`). Run `EXPLAIN (ANALYZE, BUFFERS)` against production-like data (a replica or a copy): look for Seq Scans on big tables, Sorts spilling to disk, nested loops with many iterations, and estimate vs actual row gaps. Typical causes: missing index (dev had 100 rows, so everything was a seq scan anyway), a function on an indexed column, OFFSET pagination, stale statistics, N+1 queries from the ORM. Fix, re-check the plan, and add a test or an EXPLAIN check so it cannot regress.

</details>

## L3

### SQL-L3-001 · recall
What is MVCC, and why do readers not block writers in PostgreSQL?

<details><summary>Answer</summary>

Multi-version concurrency control: an UPDATE writes a new row version instead of overwriting; each transaction reads the versions visible in its snapshot (committed before the snapshot). A reader keeps reading the old version while a writer creates the new one, so neither waits for the other. Writers of the **same** row do wait for each other (row lock). Old versions are later removed by VACUUM.

</details>

### SQL-L3-002 · scenario
A batch job opened a transaction at 02:00 and is still "idle in transaction" at 10:00. Users complain the database is getting slower. Why?

<details><summary>Answer</summary>

The open transaction's snapshot forces PostgreSQL to keep every row version that it might still see, so VACUUM cannot remove dead versions anywhere: tables and indexes **bloat**, queries read more pages, and it may also hold locks blocking writers or migrations. Kill the session (`pg_terminate_backend`), fix the job to commit or close, and set `idle_in_transaction_session_timeout`. Monitor the oldest transaction age.

</details>

### SQL-L3-003 · interview
Show a lost update with two transactions and give three ways to prevent it.

<details><summary>Answer</summary>

T1 and T2 both read stock = 10; both write 9; two items sold, one recorded. Prevent: (1) **atomic conditional update** `UPDATE ... SET stock = stock - 1 WHERE id = ? AND stock >= 1` and check the row count; (2) **pessimistic lock** `SELECT stock ... FOR UPDATE` in the transaction, then update; (3) **optimistic lock** with a version column, update `WHERE version = ?`, retry or report a conflict when 0 rows change. (REPEATABLE READ also detects the concurrent update of the same row and aborts one, which must then retry.)

</details>

### SQL-L3-004 · recall
`FOR UPDATE`, `FOR UPDATE NOWAIT`, `FOR UPDATE SKIP LOCKED`: what does each do, and a use case for each?

<details><summary>Answer</summary>

`FOR UPDATE`: lock the selected rows until commit; others wanting to lock or update them wait (decisions spanning several reads). `NOWAIT`: fail immediately with an error if a row is locked (better to tell the user "being edited" than to hang). `SKIP LOCKED`: silently skip locked rows: several workers each grab different jobs from a queue table without blocking each other (outbox, job queues).

</details>

### SQL-L3-005 · design
Optimistic vs pessimistic locking: when do you use each?

<details><summary>Answer</summary>

**Optimistic** (version column): conflicts are rare, or the "transaction" spans user think time across HTTP requests where no lock can be held; cost is a retry or a 409 on conflict. **Pessimistic** (`FOR UPDATE`): conflicts are frequent (retries would waste work), the critical section is short and inside one database transaction, or the work is expensive to redo. Both are per-row; for "at most N" rules over many rows, lock a parent row or use constraints.

</details>

### SQL-L3-006 · recall
List the isolation levels with the anomalies each prevents in PostgreSQL.

<details><summary>Answer</summary>

- READ UNCOMMITTED: behaves as READ COMMITTED in PostgreSQL (no dirty reads ever).
- READ COMMITTED (default): no dirty reads; each statement sees a new snapshot, so non-repeatable reads and phantoms between statements are possible; lost updates possible with read-modify-write.
- REPEATABLE READ: one snapshot per transaction: no non-repeatable reads, no phantoms (stronger than the standard requires); concurrent updates of the same row abort with 40001; **write skew** still possible.
- SERIALIZABLE (SSI): no anomalies; detects dangerous dependency patterns and aborts with 40001; the application must retry.

</details>

### SQL-L3-007 · interview
What is write skew? Give an example and two fixes.

<details><summary>Answer</summary>

Two transactions read an overlapping set of rows, each decides based on it, then each writes a **different** row; individually valid, together they violate an invariant, and since no row is written twice no lock conflict occurs. Example: two on-call doctors both go off call because each saw "2 on call"; or two bookings of the same room for overlapping times inserted as separate rows. Fixes: lock the rows the decision is based on (`SELECT ... WHERE shift_id = ? FOR UPDATE`, or lock a parent row), run at SERIALIZABLE and retry on 40001, or encode the rule as a constraint (an exclusion constraint for overlapping bookings).

</details>

### SQL-L3-008 · recall
What do SQLStates 40001 and 40P01 mean, and what must the application do?

<details><summary>Answer</summary>

40001 `serialization_failure`: at REPEATABLE READ/SERIALIZABLE, the transaction conflicted with a concurrent one. 40P01 `deadlock_detected`: PostgreSQL found a lock cycle and aborted this transaction. Both mean "nothing of this transaction happened; try again": roll back and **re-run the whole transaction** (re-reading data), with a small bounded number of retries and backoff. Retrying only the last statement is wrong.

</details>

### SQL-L3-009 · scenario
Logs show occasional `deadlock detected` errors in a job that updates many product rows per transaction. What is the likely cause and fix?

<details><summary>Answer</summary>

Two transactions update overlapping sets of rows in **different orders** (job A updates 5 then 9, job B 9 then 5). Fix: update rows in a consistent order (sort by id before updating, or `UPDATE ... WHERE id IN (...)` after locking with `SELECT ... ORDER BY id FOR UPDATE`), keep transactions small (batches), and retry on 40P01.

</details>

### SQL-L3-010 · interview
How do you size a database connection pool for 6 service instances talking to one PostgreSQL server with 8 cores?

<details><summary>Answer</summary>

Start from what the database can run in parallel: the PostgreSQL wiki's rule of thumb is about `cores × 2 + effective disks`, here ~20 active connections in total. Divide across instances: ~3-5 per instance (maybe 10 with headroom for bursts and idle time), not 6 × 50. More connections than the database can run concurrently only add contention and memory. Set `connection-timeout` so requests fail fast when the pool is exhausted, and load-test. For many clients, use PgBouncer.

</details>

### SQL-L3-011 · recall
Which timeouts should a production service set on PostgreSQL, and what does each protect against?

<details><summary>Answer</summary>

Pool `connection-timeout` (waiting for a free connection), `statement_timeout` (a runaway query), `lock_timeout` (waiting forever for a lock, critical in migrations), `idle_in_transaction_session_timeout` (abandoned open transactions), plus socket/connect timeouts in the driver. Each turns a silent hang into a fast, visible error.

</details>

### SQL-L3-012 · design
How do you rename `customers.name` to `full_name` with zero downtime during rolling deploys?

<details><summary>Answer</summary>

Expand/contract over several releases: (1) migration adds `full_name` (nullable); (2) release writes both columns, reads `name`; backfill `full_name` from `name` in batches; (3) release reads `full_name` (still writing both); (4) release stops writing `name`; (5) migration drops `name`. At every step both the old and new running versions work. A single `RENAME COLUMN` would break the old pods immediately.

</details>

### SQL-L3-013 · scenario
A migration `ALTER TABLE orders ADD COLUMN source text;` normally takes milliseconds, but during a deploy the whole application froze for 2 minutes. What happened?

<details><summary>Answer</summary>

The ALTER needs an ACCESS EXCLUSIVE lock and had to **wait** for a long-running transaction (a report, a stuck job) that was using `orders`. While it waited in the lock queue, every new query on `orders` queued **behind it**, so the application blocked until the long transaction ended. Fix: `SET lock_timeout = '3s'` in migrations (fail fast, retry later), find and kill long transactions before migrating, keep transactions short.

</details>

### SQL-L3-014 · recall
Which of these rewrite or fully scan a large table in PostgreSQL 11+? (a) `ADD COLUMN x int` (b) `ADD COLUMN x int DEFAULT 0` (c) `ADD COLUMN x uuid DEFAULT gen_random_uuid()` (d) `ALTER COLUMN price TYPE numeric` (e) `ADD CONSTRAINT ... CHECK (...)` (f) `SET NOT NULL`

<details><summary>Answer</summary>

(a) and (b) are instant (catalog only: since PG 11 a non-volatile default is evaluated once and stored in the catalog; even `DEFAULT now()` is instant, every old row gets the ALTER's timestamp). (c) **rewrites** the table: `gen_random_uuid()` is volatile, so each existing row needs its own value. (d) rewrites the table and indexes (for most type changes). (e) scans the table under a strong lock (use `NOT VALID` + `VALIDATE`). (f) scans under ACCESS EXCLUSIVE (unless a validated `CHECK (x IS NOT NULL)` already proves it, PG 12+).

</details>

### SQL-L3-015 · recall
Why `CREATE INDEX CONCURRENTLY`, and what are its caveats?

<details><summary>Answer</summary>

A plain `CREATE INDEX` blocks all writes to the table until it finishes (minutes to hours on big tables). `CONCURRENTLY` builds without blocking writes, scanning the table twice. Caveats: slower; cannot run inside a transaction block (Flyway: a separate migration with transactions disabled); if it fails it leaves an INVALID index that must be dropped and retried; it waits for existing transactions that could use the table.

</details>

### SQL-L3-016 · scenario
After moving reads to a replica, users report: "I placed an order, and my order list does not show it". Why, and how do you fix it?

<details><summary>Answer</summary>

Streaming replication is asynchronous: the replica applies changes some milliseconds to seconds later (**replication lag**). The order list read hit the replica before the order arrived. Fix: **read-your-writes**: route a user's reads to the primary for a short time after they write (or for their own data), use the primary for anything just written in the same flow, or wait for the replica to reach the write's LSN. Monitor lag. (Shop Level 5 does the time-window routing.)

</details>

### SQL-L3-017 · design
Design a job queue table in PostgreSQL that several workers consume without processing a job twice.

<details><summary>Answer</summary>

`jobs(id, status, run_at, attempts, payload, locked_until)` with a partial index on pending jobs `(run_at) WHERE status = 'PENDING'`. A worker claims a batch: `UPDATE jobs SET status = 'RUNNING', locked_until = now() + interval '5 min' WHERE id IN (SELECT id FROM jobs WHERE status = 'PENDING' AND run_at <= now() ORDER BY run_at LIMIT 10 FOR UPDATE SKIP LOCKED) RETURNING *`, processes, marks DONE (or schedules a retry with backoff, DEAD after N attempts). A sweeper returns jobs whose `locked_until` passed (crashed worker). Handlers must be idempotent because delivery is at-least-once. This is the shop's outbox design.

</details>

### SQL-L3-018 · recall
How do you find the most expensive queries in a running PostgreSQL?

<details><summary>Answer</summary>

`pg_stat_statements` (extension): normalised statements with calls, total and mean time, rows, buffer hits/reads; sort by total time (what costs the most overall) and by mean time (what is slow per call). Live: `pg_stat_activity` (what runs now, for how long, waiting on what), `pg_locks` (who blocks whom), the slow query log (`log_min_duration_statement`), `auto_explain` for plans of slow queries.

</details>

### SQL-L3-019 · interview
In which order would you scale a struggling PostgreSQL-backed service, and why in that order?

<details><summary>Answer</summary>

(1) Fix queries and indexes (pg_stat_statements, EXPLAIN, remove N+1s): cheapest, often 10-100x. (2) Right-size pools and timeouts. (3) Cache hot reads. (4) Vertical scaling (more CPU/RAM/IOPS). (5) Read replicas for read-heavy load, with read-your-writes routing. (6) Partition huge tables (time-based data). (7) Move work async (queues). (8) Sharding or a distributed database, last: it makes joins, transactions and operations much harder.

</details>

### SQL-L3-020 · scenario
A request does `INSERT INTO orders ...` and then `INSERT INTO outbox ...` in auto-commit mode. What can go wrong, and what is the fix?

<details><summary>Answer</summary>

Each statement commits on its own: if the process crashes or the second insert fails after the first, an order exists without its event (no confirmation email, no stock reservation downstream), or with the order the other way round, an event for an order that does not exist. Both inserts must be in **one transaction** (`setAutoCommit(false)` ... `commit()`, or `@Transactional`), so they commit or roll back together. That atomicity is the whole point of the transactional outbox.

</details>
