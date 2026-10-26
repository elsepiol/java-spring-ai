# Track 04: SQL and databases (PostgreSQL)

The data layer every backend depends on, learned on a real PostgreSQL that the katas start for you (no Docker needed).

| Level | Lesson | Katas |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): relational model, how SELECT is evaluated, joins, aggregation, NULL logic, subqueries and CTEs, JDBC, SQL injection | `sql/l1/*.sql`: cheap_in_stock, orders_per_customer, revenue_by_category, never_ordered, best_customer_per_city, not_in_berlin (bug); `ProductSearch` (bug: injection) |
| L2 Practical | [L2-practical.md](L2-practical.md): B-tree, composite, expression, partial and covering indexes, EXPLAIN, window functions, OFFSET vs keyset, upserts, transactions, schema design | `sql/l2/*.sql`: top_products_per_category, running_total, monthly_growth, remove_duplicate_signups, keyset_page, add_stock, indexes (checked via EXPLAIN on 200k rows) |
| L3 Production | [L3-production.md](L3-production.md): MVCC and vacuum, lost updates, FOR UPDATE / SKIP LOCKED, optimistic locking, isolation levels and write skew, retries, pools and timeouts, zero-downtime migrations, scaling | `StockReservations`, `OptimisticPrices`, `OnCall` (bug: write skew), `sql/l3/add_customer_status.sql` (migration) |

```
cd katas && ./mvnw test -Dgroups="sql & L1"       # starts an embedded PostgreSQL 17 automatically
java quiz/Quiz.java --track sql --level 2
```

- SQL katas: edit `katas/src/main/resources/sql/lN/*.sql`. Java katas: `katas/src/main/java/dojo/sql/lN/`.
- The data every query runs on: `katas/src/test/resources/sql/shop.sql`. Work answers out by hand first.
- Your own server instead of the embedded one: `DOJO_DB_URL='jdbc:postgresql://localhost:5432/postgres?user=postgres&password=pg' ./mvnw test -Dgroups=sql`.
- Embedded PostgreSQL refuses to run as root (some containers); use `DOJO_DB_URL` there.

Question bank: [questions.md](questions.md) (60 questions).
