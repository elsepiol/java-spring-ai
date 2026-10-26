# JPA and transactions, L2: N+1, lazy loading, @Transactional, projections

**Goal:** recognise and fix the performance and correctness problems every JPA codebase has: N+1 queries, `LazyInitializationException`, open-session-in-view, misused `@Transactional`, and heavy entity loading where a projection would do.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="jpa & L2"`. Drill: `java quiz/Quiz.java --track jpa-transactions --level 2`.

---

## 1. The N+1 query problem

```java
List<OrderHeader> orders = repository.findAll();         // 1 query
for (OrderHeader o : orders) {
    o.getItems().size();                                  // +1 query PER ORDER (lazy collection loads)
}
```

1,000 orders → 1,001 round trips. Fast on a laptop with 10 rows, a 4-second page in production. It also happens through `@ManyToOne` (each order's customer), through serialising entities to JSON, and in `toString()`.

**Detect:** SQL logs; Hibernate statistics (`generate_statistics=true`, `getPrepareStatementCount()`, used by the kata); tests that assert a statement count; APM traces showing hundreds of identical queries.

**Fix:**
- **Fetch join:** `select distinct o from OrderHeader o left join fetch o.items` (one query; careful: fetching two collections at once creates a cartesian product, and combining fetch joins with pagination makes Hibernate paginate in memory: it warns).
- **`@EntityGraph(attributePaths = "items")`** on a repository method: the same, declaratively.
- **Batch fetching:** `@BatchSize(size = 50)` or `hibernate.default_batch_fetch_size=50`: lazy loads are grouped into `WHERE order_id IN (...)`: 1 + N/50 queries, works with pagination.
- **Projection / aggregate in SQL:** if you need totals, let the database `SUM` (the kata's reference solution): one query, no entities.

> **Kata `OrderTotals`**: from 32 statements to at most 2.

---

## 2. LazyInitializationException and open-session-in-view

A lazy association can only load while its persistence context is open. Outside a transaction, a repository call opens and closes its own short context; the returned entity is **detached**, and touching a lazy collection throws `LazyInitializationException: could not initialize proxy - no Session`.

Why tests miss it: `@DataJpaTest` (and `@Transactional` tests) wrap each test in a transaction, so the context stays open for the whole test and lazy loading "works". The kata's test runs without a test transaction (`Propagation.NOT_SUPPORTED`), like production.

Fixes, in order of preference:
1. Build what you return **inside a transaction**: `@Transactional(readOnly = true)` on the service method that maps entities to DTOs.
2. Fetch what you need explicitly (fetch join, `@EntityGraph`) or query a **DTO projection** directly.
3. Not: making everything EAGER (loads too much everywhere, N+1 in other places).
4. Not: **open-session-in-view** (`spring.jpa.open-in-view`, **true by default** in Spring Boot, with a warning at startup): it keeps the persistence context open during view rendering/JSON serialisation, so lazy loading works in the web layer, but it hides N+1 queries in serialisation and holds a database connection for the whole request. Set `spring.jpa.open-in-view=false` in new projects.

> **Kata `CatalogService`**: the bug tests never caught.

---

## 3. `@Transactional` basics in Spring

```java
@Service
public class OrderService {
    @Transactional                                  // one transaction for the whole business operation
    public PlacedOrder place(CreateOrderRequest r) {
        reserveStock(r);                            // all of this commits together, or rolls back together
        CustomerOrder order = orders.save(...);
        outbox.publish(new OrderPlaced(order.getId()));
        return ...;
    }

    @Transactional(readOnly = true)                 // reads: no dirty checking flush, a hint to the driver/database
    public OrderResponse find(long id) { ... }
}
```

- Put it on **service** methods that are business operations (the unit of work), not on controllers, not on every repository call (Spring Data repository methods are already transactional individually).
- **Rollback rules:** unchecked exceptions (RuntimeException, Error) roll back; **checked exceptions commit** unless `rollbackFor = Exception.class`.
- Catching an exception inside the method and continuing means the interceptor sees success and **commits**.
- It works through a proxy: **self-invocation, private and final methods are not transactional** (Spring core L3); L3 shows the consequence with propagation.
- **`readOnly = true`**: Hibernate skips dirty checking (faster for large reads), and you can route read-only transactions to a replica (shop Level 5).
- Keep transactions **short**: no HTTP calls, no waiting inside them (SQL track L2).

---

## 4. Projections and read models

Loading entities costs: every column, a managed object, a snapshot for dirty checking, maybe lazy proxies. For **reads** that do not modify anything, query exactly what the screen needs:

```java
record ProductSummary(String sku, String name, long priceCents) {}

@Query("select new dojo.ProductSummary(p.sku, p.name, p.priceCents) from Product p where p.category = :c")
List<ProductSummary> summaries(@Param("c") String category);         // a DTO directly from JPQL
```

Or interface projections (`interface ProductSummary { String getSku(); ... }`), or Spring Data's derived projections (`List<ProductSummary> findByCategory(String c)`). For complex reporting, plain SQL with `JdbcClient` is often clearer than JPA: JPA is a great fit for loading, changing and saving aggregates, not for every query.

---

## 5. Batching writes

- `hibernate.jdbc.batch_size=50` + `order_inserts/order_updates=true` groups inserts/updates into JDBC batches (one round trip per 50).
- **IDENTITY ids disable insert batching** (each insert must return its id); use `SEQUENCE` with an `allocationSize` (or client-side UUIDs) for bulk inserts.
- For large imports, use `COPY` or JDBC batches directly, and flush/clear the persistence context every N entities, or memory grows with every managed entity.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `OrderTotals` (bug) | N+1 detection with statistics, fetch join, aggregate projection |
| `CatalogService` (bug) | LazyInitializationException, transaction boundaries, why test transactions hide bugs |

```
cd katas && ./mvnw test -Dgroups="jpa & L2"
```

## 7. Seen in the shop project

- `spring.jpa.open-in-view=false` and services mapping to response records inside transactions.
- Tests that count SQL statements to keep N+1 out (Level 3/4 tests use Hibernate statistics or a statement counter).
- `@Transactional(readOnly = true)` reads routed to the read replica (Level 5).

## 8. Interview checklist

1. What is the N+1 problem? How do you detect it, and four ways to fix it?
2. Fetch join vs `@EntityGraph` vs batch fetching: trade-offs (cartesian products, pagination).
3. What causes `LazyInitializationException`? Why do `@DataJpaTest` tests often miss it?
4. What is open-session-in-view, what is its default in Spring Boot, and why turn it off?
5. Where do you put `@Transactional`? Which exceptions roll back by default?
6. What does `readOnly = true` do?
7. Entity vs projection for a read-only list page: which and why?
8. Why do IDENTITY ids disable batch inserts?
