# SQL and databases, L1: tables, queries and JDBC

**Goal:** read and write the SQL a backend uses every day (filter, join, group, subqueries), understand NULL properly, and talk to PostgreSQL from Java safely. Every Spring/JPA feature later is built on exactly this.

**Setup:** nothing to install. The SQL katas start a real PostgreSQL 17 inside the test JVM (embedded-postgres). To experiment by hand, use any PostgreSQL (`docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=pg postgres:17`, then `psql -h localhost -U postgres`) and load the dojo's shop data: `psql ... -f katas/src/test/resources/sql/shop.sql`.

Katas: `cd katas && ./mvnw test -Dgroups="sql & L1"`. Your queries go in `katas/src/main/resources/sql/l1/*.sql`. Drill: `java quiz/Quiz.java --track sql --level 1`.

---

## 1. The relational model in five minutes

- A **table** holds rows of the same shape; each **column** has a type (`bigint`, `text`, `timestamptz`, `boolean`, `numeric`).
- A **primary key** identifies a row: unique and not null. Usually a surrogate `bigint` (or UUID), not a business value that might change (an email).
- A **foreign key** (`customer_id REFERENCES customers (id)`) says "this value must exist there". The database enforces it: you cannot create an order for a missing customer, or delete a customer who still has orders (unless you say `ON DELETE CASCADE`).
- **Constraints** (`NOT NULL`, `UNIQUE`, `CHECK (price_cents >= 0)`) are rules the database enforces for **every** writer: your service, a migration, a colleague with psql at 2 a.m. Application validation is for nice error messages; constraints are for correctness.
- **Normalisation** (the short version): store each fact once. The customer's city lives in `customers`, not copied into every order; otherwise the copies drift apart. Exception: `order_items.unit_price_cents` deliberately copies the price, because an order must keep the price *at the time it was placed*.

The dojo's shop (`katas/src/test/resources/sql/shop.sql`):

```
customers (id, name, email UNIQUE, city NULL, created_at)
products  (id, sku UNIQUE, name, category, price_cents, stock, version)
orders    (id, customer_id -> customers, status PLACED|PAID|CANCELLED, created_at)
order_items (order_id -> orders, product_id -> products, quantity, unit_price_cents)   PK (order_id, product_id)
```

---

## 2. How a SELECT is evaluated

You write the clauses in this order, but the database evaluates them **logically** in another one, and that explains most "why can't I use this here?" errors:

```
SELECT   category, SUM(price_cents) AS total     -- 5. compute output columns (aliases are born here)
FROM     products                                -- 1. which rows exist (and joins)
WHERE    stock > 0                               -- 2. filter ROWS
GROUP BY category                                -- 3. form groups
HAVING   SUM(price_cents) > 1000                 -- 4. filter GROUPS
ORDER BY total DESC                              -- 6. sort (aliases usable)
LIMIT    10;                                     -- 7. cut
```

- You cannot use the alias `total` in `WHERE`: it does not exist yet at step 2.
- You cannot filter on an aggregate in `WHERE` (`WHERE SUM(...) > 1000`): groups do not exist yet. That is what `HAVING` is for.
- Without `ORDER BY`, the order of rows is **not defined**, even if it looks stable today. Any query whose result order matters (pagination!) needs an `ORDER BY` that is unique.

---

## 3. Filtering and sorting

```sql
SELECT sku, price_cents FROM products
WHERE category = 'kitchen' AND price_cents BETWEEN 1000 AND 3000   -- BETWEEN includes both ends
  AND name ILIKE 'm%'                                               -- case-insensitive pattern (PostgreSQL)
  AND sku IN ('MUG-1', 'PAN-1')
ORDER BY price_cents DESC, sku                                      -- tie-breaker
LIMIT 5;
```

Strings use single quotes `'text'`; double quotes are for identifiers (`"Order"`). Comparing text is case-sensitive.

---

## 4. Joins

```
INNER JOIN   only pairs that match                      orders with their customer
LEFT JOIN    every row of the left table; NULLs where   customers, with their orders if any
             there is no match on the right
RIGHT JOIN   the mirror image (rarely used: swap the tables and use LEFT)
FULL JOIN    everything from both sides
CROSS JOIN   every combination (n x m rows)
self join    a table joined with itself (employee -> manager)
```

```sql
SELECT c.name, o.id, o.status
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id;   -- Eve (no orders) appears once, with o.id = NULL
```

Two LEFT JOIN traps:
- **A condition on the right table in `WHERE` turns the LEFT JOIN into an INNER JOIN**: `WHERE o.status = 'PAID'` removes Eve, because `NULL = 'PAID'` is not true. Put it in the `ON` clause to keep customers without paid orders: `LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID'`.
- **Joins multiply rows.** Joining customers to orders gives one row per order; joining again to order_items gives one row per item. `SUM` over such rows can double-count if you join two independent one-to-many relations at once. Aggregate one side first (in a subquery or CTE), then join.

---

## 5. Aggregation

```sql
SELECT c.name, COUNT(o.id) AS orders           -- COUNT(column) skips NULLs
FROM customers c LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name
ORDER BY orders DESC, c.name;
```

- `COUNT(*)` counts rows; `COUNT(col)` counts non-NULL values; `COUNT(DISTINCT col)` distinct ones. With the LEFT JOIN above, `COUNT(*)` would give Eve **1** (her one row with NULLs).
- `SUM`, `AVG`, `MIN`, `MAX` ignore NULLs; `SUM` of no rows is **NULL**, not 0: use `COALESCE(SUM(x), 0)`.
- Every selected column must be grouped or aggregated (PostgreSQL allows other columns of a table whose primary key is grouped).

---

## 6. NULL: three-valued logic

NULL means "unknown". Any comparison with NULL is **unknown** (NULL), and `WHERE` keeps only rows where the condition is **true**.

| Expression | Result |
|---|---|
| `NULL = NULL` | NULL (not true!) |
| `NULL <> 'Berlin'` | NULL |
| `city IS NULL` | true/false |
| `city IS DISTINCT FROM 'Berlin'` | true for NULL (NULL-safe "not equal") |
| `TRUE OR NULL` / `FALSE AND NULL` | true / false |
| `COALESCE(city, 'unknown')` | the first non-NULL value |

Classic bugs:
- `WHERE city <> 'Berlin'` silently drops customers with an unknown city (kata `not_in_berlin`).
- **`NOT IN` with a NULL in the list returns nothing**: `WHERE id NOT IN (SELECT customer_id FROM x)` is empty as soon as one `customer_id` is NULL, because `id <> NULL` is unknown. Use `NOT EXISTS`.
- `UNIQUE` allows several NULLs (they are not equal to each other).

---

## 7. Subqueries, EXISTS and CTEs

```sql
-- Anti-join: products never ordered
SELECT sku FROM products p
WHERE NOT EXISTS (SELECT 1 FROM order_items i WHERE i.product_id = p.id);

-- Scalar subquery
SELECT name, (SELECT count(*) FROM orders o WHERE o.customer_id = c.id) AS orders FROM customers c;

-- CTE (WITH): name an intermediate result, read top-down
WITH paid AS (
    SELECT o.customer_id, SUM(i.quantity * i.unit_price_cents) AS spent
    FROM orders o JOIN order_items i ON i.order_id = o.id
    WHERE o.status = 'PAID'
    GROUP BY o.customer_id
)
SELECT c.name, paid.spent FROM customers c JOIN paid ON paid.customer_id = c.id ORDER BY spent DESC;
```

`EXISTS` stops at the first match and is NULL-safe; a correlated subquery runs (logically) once per outer row, though the planner often turns it into a join.

---

## 8. Changing data, and JDBC

```sql
INSERT INTO products (id, sku, name, category, price_cents) VALUES (8, 'CUP-1', 'Cup', 'kitchen', 900) RETURNING id;
UPDATE products SET stock = stock - 1 WHERE id = 1 AND stock >= 1;   -- the row count tells you if it happened
DELETE FROM orders WHERE status = 'CANCELLED' AND created_at < now() - interval '1 year';
```

An `UPDATE` or `DELETE` without `WHERE` changes **every row**. In psql, try it inside `BEGIN; ... ROLLBACK;` first.

**JDBC** is Java's standard API for SQL databases. Spring's `JdbcTemplate`, JPA and every database library sit on top of it.

```java
try (Connection c = DriverManager.getConnection(url);                  // in a service: from a pool (HikariCP)
     PreparedStatement ps = c.prepareStatement(
         "SELECT sku, price_cents FROM products WHERE category = ? AND price_cents < ?")) {
    ps.setString(1, category);                                          // parameters are 1-based
    ps.setLong(2, maxCents);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String sku = rs.getString("sku");
            long price = rs.getLong("price_cents");
        }
    }
}   // closed in reverse order; a leaked Connection is a leaked pool slot
```

**SQL injection**, the number one rule of database code: **user input must never become part of the SQL text.**

```java
// NEVER: the input is parsed as SQL
"SELECT * FROM users WHERE email = '" + email + "'"
// input:  x' OR 1=1 --         ->  WHERE email = 'x' OR 1=1 --'      every user returned
// input:  x'; DROP TABLE users; --
```

A `PreparedStatement` sends the SQL and the values **separately**; a value can never change the statement's structure. That also handles apostrophes in names (O'Brien) for free. Identifiers (table or column names, sort direction) cannot be parameters: pick them from a fixed allow-list in code.

> **Kata `ProductSearch`**: the shop's search box, injectable. Fix it, including literal `%` and `_`.

---

## 9. Katas for this level

| Kata | File | Practises |
|---|---|---|
| cheap_in_stock | `sql/l1/cheap_in_stock.sql` | WHERE, ORDER BY |
| orders_per_customer | `sql/l1/orders_per_customer.sql` | LEFT JOIN, COUNT(col) vs COUNT(*) |
| revenue_by_category | `sql/l1/revenue_by_category.sql` | joins, GROUP BY, HAVING |
| never_ordered | `sql/l1/never_ordered.sql` | anti-join with NOT EXISTS |
| best_customer_per_city | `sql/l1/best_customer_per_city.sql` | conditions in ON vs WHERE, COALESCE |
| not_in_berlin (bug) | `sql/l1/not_in_berlin.sql` | NULL logic |
| `ProductSearch` (bug) | `dojo/sql/l1/ProductSearch.java` | JDBC, PreparedStatement, SQL injection |

```
cd katas && ./mvnw test -Dgroups="sql & L1"
```

The tests (`katas/src/test/java/dojo/sql/l1/`) show the expected rows. Work out an answer by hand from `shop.sql` before you run it.

## 10. Seen in the shop project

- Flyway migrations with keys, foreign keys and checks: `src/main/resources/db/migration/V1..V5` in spring-backend-ai (e.g. `V3__orders_and_inventory.sql`).
- `unit_price_cents` copied into order items on purpose (`OrderItem`).
- Parameterised queries everywhere: Spring Data `@Query` with `:named` parameters (`InventoryRepository`, `ProductRepository`).

## 11. Interview checklist

1. Primary key vs unique constraint vs foreign key.
2. In which order are the clauses of a SELECT evaluated? Why can't `WHERE` use a SELECT alias or an aggregate?
3. INNER vs LEFT JOIN; why does a `WHERE` on the right table turn a LEFT JOIN into an INNER JOIN?
4. `COUNT(*)` vs `COUNT(col)` vs `COUNT(DISTINCT col)`.
5. `WHERE` vs `HAVING`.
6. Why does `NULL = NULL` not return true? What does `NOT IN` do with a NULL in the list?
7. `EXISTS` vs `IN` vs `JOIN` for "customers who ordered something".
8. What is SQL injection, and why does a PreparedStatement prevent it? What cannot be a parameter?
9. Why should constraints live in the database and not only in the application?
