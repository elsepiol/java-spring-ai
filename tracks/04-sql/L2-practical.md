# SQL and databases, L2: indexes, window functions, pagination, transactions

**Goal:** make queries fast on purpose (indexes you can justify, plans you can read), answer "top N per group" and "running total" questions without application code, paginate correctly, write data atomically, and use transactions correctly from Java.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="sql & L2"`. Drill: `java quiz/Quiz.java --track sql --level 2`.

---

## 1. Indexes and query plans

Without an index, `WHERE customer_id = 42` reads **every row** (a sequential scan): O(n). A **B-tree index** keeps the column's values sorted in a balanced tree, each entry pointing to its row; finding a value is O(log n), a few page reads even for a billion rows. Most indexes are B-trees.

**Kinds you must know:**

| Index | Example | Use |
|---|---|---|
| single column | `CREATE INDEX ON orders (customer_id)` | equality and ranges on that column; foreign keys (PostgreSQL does NOT index them automatically) |
| composite | `ON events (customer_id, created_at)` | equality on the first column(s) + range or ORDER BY on the next |
| unique | `CREATE UNIQUE INDEX ON users (lower(email))` | a constraint and an index at once |
| expression | `ON events (lower(external_ref))` | the query must use the same expression |
| partial | `ON events (id) WHERE status = 'PENDING'` | a small hot subset (job queues, soft-deleted rows) |
| covering | `ON orders (customer_id) INCLUDE (status, total)` | an **index-only scan**: the query never touches the table |

**Composite index column order** (asked constantly): an index on `(a, b)` is sorted by `a`, then by `b` within each `a`, like a phone book sorted by last name then first name. It helps `WHERE a = ?`, `WHERE a = ? AND b > ?`, `WHERE a = ? ORDER BY b`, but **not** `WHERE b = ?` alone (the leftmost-prefix rule). Rule of thumb: equality columns first, then the range or sort column.

**When an index is NOT used:**
- a function or cast on the column (`WHERE lower(email) = ?` needs an expression index; `WHERE created_at::date = ?` cannot use an index on `created_at`: write a range instead);
- a leading wildcard (`LIKE '%mug'`; a trigram index can help);
- low selectivity: if the condition matches a large share of the table, reading the whole table sequentially is cheaper, and the planner (correctly) prefers it;
- outdated statistics (run `ANALYZE`; autovacuum normally does);
- too few rows: on tiny tables a seq scan is always cheapest, so test plans on realistic data.

**The cost of indexes:** every `INSERT`, every `UPDATE` of an indexed column and every `DELETE` must update each index; indexes use disk and memory. Add indexes for the queries you actually run; check unused ones (`pg_stat_user_indexes.idx_scan = 0`).

**Reading `EXPLAIN`:**

```sql
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM events WHERE customer_id = 42 ORDER BY created_at DESC LIMIT 20;

Limit  (cost=0.42..21.30 rows=20) (actual time=0.031..0.060 rows=20 loops=1)
  ->  Index Scan Backward using events_customer_created_idx on events  (... rows=20 ...)
        Index Cond: (customer_id = 42)
  Buffers: shared hit=24
```

- Read it inside out: the most indented node runs first.
- **Node types:** `Seq Scan` (whole table), `Index Scan` (index, then table), `Index Only Scan` (index only), `Bitmap Index/Heap Scan` (many matches: collect row locations, then read pages in order), `Sort`, `Hash Join` / `Nested Loop` / `Merge Join`, `HashAggregate`.
- `cost` is the planner's estimate in arbitrary units; `rows` estimated vs `actual ... rows`: **a big difference means bad statistics** and usually a bad plan.
- `EXPLAIN` alone only plans; `EXPLAIN ANALYZE` **runs** the query (careful with `UPDATE`/`DELETE`: wrap in `BEGIN ... ROLLBACK`).

> **Kata `indexes`**: write at most three indexes for three queries on 200,000 rows; the test reads the plans. Composite order, an expression index, and a partial index.

---

## 2. Window functions

A window function computes over a set of rows **related to the current row** without collapsing them like `GROUP BY` does.

```sql
SELECT customer_id, id, total,
       SUM(total)   OVER (PARTITION BY customer_id ORDER BY created_at) AS running_total,
       ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY total DESC) AS nth_biggest,
       LAG(total)   OVER (PARTITION BY customer_id ORDER BY created_at) AS previous_total,
       AVG(total)   OVER (PARTITION BY customer_id)                     AS customer_avg
FROM order_totals;
```

- `PARTITION BY` = the groups; `ORDER BY` inside `OVER` = the order within each group.
- **Ranking:** `ROW_NUMBER()` (1,2,3,4 always distinct), `RANK()` (1,2,2,4: ties share, gaps after), `DENSE_RANK()` (1,2,2,3).
- **Offsets:** `LAG(x)` / `LEAD(x)`: previous / next row's value; month-over-month change is `x - LAG(x) OVER (ORDER BY month)`.
- **Frames:** with an `ORDER BY`, aggregate windows default to "from the start up to the current row" (`RANGE ... CURRENT ROW`, which includes rows *tied* with the current one); write `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` when you mean exactly the rows so far, or `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW` for a 7-day moving average.
- Window functions run after `WHERE`/`GROUP BY`/`HAVING`, so to filter on them ("top 2 per category") wrap the query: `SELECT * FROM (... ROW_NUMBER() ... AS rn) t WHERE rn <= 2`.

**"Top N per group" and "remove duplicates keeping the first" are the two most common SQL interview tasks**, and both are `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)`.

> **Katas**: `top_products_per_category`, `running_total`, `monthly_growth`, `remove_duplicate_signups`.

---

## 3. Pagination: OFFSET vs keyset

```sql
-- OFFSET: simple, and gets slower with every page
SELECT * FROM products ORDER BY price_cents, id LIMIT 20 OFFSET 40000;   -- reads 40,020 rows, throws 40,000 away

-- Keyset ("seek", "cursor"): continue after the last row the client saw
SELECT * FROM products WHERE (price_cents, id) > (:lastPrice, :lastId) ORDER BY price_cents, id LIMIT 20;
```

| | OFFSET | Keyset |
|---|---|---|
| Cost of page n | O(offset + limit) | O(limit) with an index on the sort key |
| New rows inserted while paging | items shift: duplicates or skipped items | stable |
| Jump to page 57 | yes | no (next/previous only) |
| Use for | small admin tables, "page 3 of 5" UIs | feeds, infinite scroll, APIs, exports, batch jobs |

- The sort key must be **unique**: add the id as a tie-breaker, or rows with equal prices are skipped or repeated at page boundaries.
- `(a, b) > (x, y)` is a row-value comparison: `a > x OR (a = x AND b > y)`, and PostgreSQL can use an index on `(a, b)` for it.
- The API returns the last row's key as an opaque `nextCursor` (the shop project does: `product/Cursor.java`).

> **Kata `keyset_page`**, including equal prices.

---

## 4. Writing atomically: upserts, RETURNING, bulk

"Read, decide in Java, then write" is a race when two requests do it at once (concurrency track). Let one statement do the deciding:

```sql
-- insert or update, atomically (needs a unique constraint or primary key to detect the conflict)
INSERT INTO stock_counters (sku, quantity) VALUES (?, ?)
ON CONFLICT (sku) DO UPDATE SET quantity = stock_counters.quantity + EXCLUDED.quantity;

-- insert unless it exists (idempotent)
INSERT INTO notifications (event_id, user_id, message) VALUES (?, ?, ?) ON CONFLICT (event_id, user_id) DO NOTHING;

-- conditional update: the WHERE is the business rule, the row count is the answer
UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?;

-- get generated values back without a second query
INSERT INTO orders (...) VALUES (...) RETURNING id, created_at;
```

**Bulk work:** one statement for many rows beats a loop of statements (each one is a network round trip). JDBC `addBatch()`/`executeBatch()`; for large loads `COPY`; for large deletes, delete in batches (`DELETE ... WHERE id IN (SELECT id ... LIMIT 10000)`) so locks and the transaction stay small.

> **Kata `add_stock`**: an upsert that stays correct under 10 concurrent writers.

---

## 5. Transactions (the basics)

A **transaction** groups statements into one unit: all of them happen, or none (**ACID**):
- **Atomicity**: all or nothing (`ROLLBACK` undoes everything since `BEGIN`; a crash mid-way leaves nothing half-done).
- **Consistency**: constraints hold at commit.
- **Isolation**: concurrent transactions do not see each other's uncommitted changes (how much they see of committed ones: the isolation level, L3).
- **Durability**: once `COMMIT` returns, it survives a crash (the write-ahead log).

In JDBC a connection is in **auto-commit** mode by default: every statement is its own transaction.

```java
c.setAutoCommit(false);
try {
    debit(c, from, amount);
    credit(c, to, amount);
    c.commit();
} catch (SQLException | RuntimeException e) {
    c.rollback();          // never leave a failed transaction open on a pooled connection
    throw e;
} finally {
    c.setAutoCommit(true);
}
```

Spring's `@Transactional` does exactly this for you (JPA track), and gives every method in the call the same connection.

Rules of thumb:
- **Keep transactions short.** An open transaction holds locks and blocks cleanup of old row versions (L3). Never wait for a human or a remote HTTP call inside one.
- One business operation = one transaction. Placing an order (stock, order, items, outbox event) commits or fails together.

---

## 6. Schema design choices that matter later

- **Types:** money in `bigint` cents or `numeric(12,2)`, never floating point; times as `timestamptz` (an instant), not `timestamp` (a wall-clock reading without a zone), unless you really mean local time; `text` with a `CHECK` on length instead of guessing `varchar(255)`; `boolean`, not `'Y'/'N'`.
- **IDs:** `bigint GENERATED ALWAYS AS IDENTITY` (compact, ordered, fast to index) or UUIDs (generated anywhere, not guessable, larger; prefer time-ordered UUIDv7 over random v4 for index locality). Never expose sequential ids where guessing them is a problem.
- **Enums:** a `text` column with a `CHECK` (easy to extend) or a lookup table.
- **Soft delete** (`deleted_at`) makes every query and unique constraint more complicated (`WHERE deleted_at IS NULL`, partial unique indexes). Use it when you need it, not by default.
- **Denormalise deliberately**, for measured read performance, with a plan for keeping copies in sync.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `top_products_per_category` | aggregate then rank, ROW_NUMBER, filtering on a window |
| `running_total` | SUM OVER with a ROWS frame, CTEs |
| `monthly_growth` | LAG |
| `remove_duplicate_signups` | ROW_NUMBER for de-duplication, DELETE with a subquery |
| `keyset_page` | keyset pagination with row values and a tie-breaker |
| `add_stock` | INSERT ... ON CONFLICT under concurrency |
| `indexes` | composite, expression and partial indexes, EXPLAIN |

```
cd katas && ./mvnw test -Dgroups="sql & L2"
```

## 8. Seen in the shop project

- Keyset pagination with an opaque cursor: `product/Cursor.java`, `ProductRepository` (whose comment explains why the row-value form matters).
- Indexes checked with EXPLAIN in a test: `ProductQueryPlanTest` (Level 4), and `V4__product_version_and_price_index.sql`.
- Idempotent insert with ON CONFLICT: `NotificationRepository.recordOnce`.
- Conditional update as a business rule: `InventoryRepository`.

## 9. Interview checklist

1. How does a B-tree index make a lookup fast? What does it cost?
2. Index on `(a, b)`: which queries can use it? Why does column order matter?
3. Name four reasons the planner ignores an index.
4. What are covering, partial and expression indexes? Give a use case for each.
5. How do you read an EXPLAIN plan? What does a big gap between estimated and actual rows mean?
6. `ROW_NUMBER` vs `RANK` vs `DENSE_RANK`. Write "top 3 products per category".
7. How do you delete duplicate rows keeping one?
8. Why is OFFSET pagination slow for deep pages and unstable under inserts? How does keyset work?
9. What does `INSERT ... ON CONFLICT` solve that SELECT-then-INSERT does not?
10. What does ACID mean? What does auto-commit mean in JDBC?
11. Why should transactions be short?
12. `timestamp` vs `timestamptz`; `bigint` identity vs UUID.
