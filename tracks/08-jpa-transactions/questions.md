# JPA and transactions: question bank

Drill with `java quiz/Quiz.java --track jpa-transactions`.

## L1

### JPA-L1-001 · recall
What are JPA, Hibernate and Spring Data JPA, and how do they relate?

<details><summary>Answer</summary>

JPA (Jakarta Persistence) is a specification: annotations like `@Entity` and the `EntityManager` API. Hibernate is the most common implementation of it (it generates SQL, manages the persistence context, dirty checking, caching). Spring Data JPA sits on top and generates repository implementations from interfaces (`JpaRepository`, derived queries, `@Query`).

</details>

### JPA-L1-002 · recall
What is the persistence context?

<details><summary>Answer</summary>

The set of managed entity instances held by an `EntityManager`, a first-level cache keyed by entity type and id. Within one context, each database row is represented by at most one Java object; Hibernate tracks changes to those objects and writes them at flush. In Spring, it normally lives as long as the transaction.

</details>

### JPA-L1-003 · predict
Inside one `@Transactional` method you call `repo.findById(1L)` twice. How many SELECTs run, and are the results `==`?

<details><summary>Answer</summary>

One SELECT. The second call finds the entity in the persistence context (first-level cache), and both return the same instance, so `a == b` is true.

</details>

### JPA-L1-004 · why
Inside a `@Transactional` method you load a product, call `product.setPrice(...)`, and never call `save()`. Is the change persisted? Why?

<details><summary>Answer</summary>

Yes. The entity is managed; at flush (before commit) Hibernate's dirty checking compares it with the snapshot taken when it was loaded and issues an `UPDATE`. Calling `save()` on a managed entity is redundant.

</details>

### JPA-L1-005 · recall
Name the four entity states and how an entity moves between them.

<details><summary>Answer</summary>

Transient (new object, not known to any context) → `persist` → managed. Managed → the context closes or `detach`/`clear` → detached. Detached → `merge` → a managed copy. Managed → `remove` → removed (deleted at flush).

</details>

### JPA-L1-006 · recall
What is the difference between flush and commit?

<details><summary>Answer</summary>

Flush sends pending SQL (inserts, updates, deletes) to the database inside the current transaction; the changes are visible to that transaction but can still be rolled back. Commit ends the transaction and makes them durable and visible to others. Hibernate flushes automatically before commit and before queries that could see pending changes.

</details>

### JPA-L1-007 · why
Why must an entity have a no-argument constructor, and why can it be `protected`?

<details><summary>Answer</summary>

Hibernate instantiates entities by reflection when loading rows (and subclasses them for lazy proxies), so it needs a constructor without parameters. It does not need to be public; `protected` keeps application code using your real constructors.

</details>

### JPA-L1-008 · bug
An entity maps `@Enumerated private Status status;` with values `NEW, PAID, SHIPPED`. Someone inserts `CANCELLED` between NEW and PAID. What breaks?

<details><summary>Answer</summary>

The default is `EnumType.ORDINAL`: rows store 0, 1, 2. After reordering, every existing `1` (PAID) now reads as CANCELLED and `2` as PAID. Silent data corruption. Always use `@Enumerated(EnumType.STRING)`.

</details>

### JPA-L1-009 · scenario
You save a customer with a duplicate email (a unique constraint) using an IDENTITY id. When do you get the exception: at `save()` or at commit?

<details><summary>Answer</summary>

At `save()`. With IDENTITY, Hibernate must run the INSERT immediately to obtain the generated id, so the constraint violation (`DataIntegrityViolationException` in Spring) surfaces right there. With SEQUENCE or assigned ids the insert is delayed until flush, so it would come at flush/commit. The Mapping kata's test hit exactly this.

</details>

### JPA-L1-010 · recall
IDENTITY vs SEQUENCE id generation: what is the practical difference?

<details><summary>Answer</summary>

IDENTITY: the database assigns the id during the INSERT, so Hibernate inserts immediately and cannot batch inserts. SEQUENCE: Hibernate fetches ids from a database sequence, with `allocationSize` (e.g. 50) it reserves blocks so most ids need no round trip, and inserts can be delayed and JDBC-batched. Client-generated UUIDs need no database call at all.

</details>

### JPA-L1-011 · recall
In a bidirectional `Order`–`OrderItem` relationship, which side is the owner, and what does `mappedBy` mean?

<details><summary>Answer</summary>

The owner is the side with the foreign key column: `OrderItem.order` with `@ManyToOne @JoinColumn(name = "order_id")`. `@OneToMany(mappedBy = "order")` on `Order.items` says "this is the inverse side, mapped by the `order` field over there". Only changes on the owning side are written to the database, so set `item.setOrder(order)` (helper methods keep both sides in sync).

</details>

### JPA-L1-012 · recall
What are the default fetch types for `@ManyToOne`, `@OneToOne`, `@OneToMany` and `@ManyToMany`? What do you usually set?

<details><summary>Answer</summary>

To-one associations (`@ManyToOne`, `@OneToOne`) are EAGER by default; collections (`@OneToMany`, `@ManyToMany`) are LAZY. Set `fetch = FetchType.LAZY` on every to-one association and fetch what you need per use case (fetch join, entity graph).

</details>

### JPA-L1-013 · bug
`OrderItem` has `@ManyToOne(cascade = CascadeType.ALL) private Product product;`. What goes wrong when you delete an order item?

<details><summary>Answer</summary>

The REMOVE cascades to the product: Hibernate tries to delete the shared product (failing on other items' foreign keys, or worse, deleting it). Cascade only from a parent to children it owns (order → items), never towards shared reference entities.

</details>

### JPA-L1-014 · recall
What does `orphanRemoval = true` do on a `@OneToMany`?

<details><summary>Answer</summary>

Removing a child from the parent's collection (`order.getItems().remove(item)`) deletes the child row at flush, because a child cannot exist without its parent. Without it, the child stays (with the FK, or the update fails for a NOT NULL FK).

</details>

### JPA-L1-015 · recall
JPQL vs native SQL in `@Query`: what is the difference, and when do you need native?

<details><summary>Answer</summary>

JPQL queries entities and their fields (`select p from Product p where p.category = :c`), is portable across databases and is validated against the mapping at startup. Native (`nativeQuery = true`) is plain SQL on tables and columns, needed for database-specific features: window functions, `ILIKE`, JSON operators, CTEs, `FOR UPDATE SKIP LOCKED`, hints.

</details>

### JPA-L1-016 · bug
`@Modifying @Query("update Product p set p.priceCents = p.priceCents * 2")` runs, then the code reads an already-loaded product and sees the old price. Why, and the fix?

<details><summary>Answer</summary>

Bulk JPQL updates go straight to the database and bypass the persistence context; managed entities keep their old state. Use `@Modifying(clearAutomatically = true)` (and `flushAutomatically = true` so pending changes are written first), or re-query after clearing. Also note bulk updates skip `@Version` increments and entity callbacks.

</details>

### JPA-L1-017 · recall
What is a projection in Spring Data JPA? Name two kinds.

<details><summary>Answer</summary>

A query result that is not a managed entity, containing only the columns you need. Interface projections (`interface CategoryTotal { String getCategory(); long getProducts(); }` with aliases matching the getters) and class/record (DTO) projections (`select new com.x.Summary(p.sku, p.name) from Product p`, or derived queries returning a record type).

</details>

### JPA-L1-018 · recall
What does `spring.jpa.hibernate.ddl-auto` control, and what should it be in production?

<details><summary>Answer</summary>

Whether Hibernate generates the schema at startup: `create`, `create-drop`, `update`, `validate`, `none`. In production (and the katas) use `validate` or `none`, with the schema owned by versioned migrations (Flyway/Liquibase). `update` cannot drop, rename or migrate data safely, and is not reviewable.

</details>

### JPA-L1-019 · interview
"Why would you use JPA at all instead of writing SQL with JdbcTemplate?" Give a balanced answer.

<details><summary>Answer</summary>

JPA removes mapping boilerplate, tracks changes (dirty checking), manages object graphs and cascades, gives optimistic locking with `@Version`, and Spring Data generates repositories. It fits loading, changing and saving aggregates in transactional business logic. The cost: hidden SQL (N+1, over-fetching), a learning curve (persistence context, lazy loading), and less control. Many teams use JPA for writes and simple reads and plain SQL (`JdbcClient`, native queries, jOOQ) for reporting and complex queries.

</details>

### JPA-L1-020 · dsa
The persistence context guarantees "one object per row". Which data structure gives that, and what is the key?

<details><summary>Answer</summary>

A hash map (identity map) keyed by (entity class, primary key) → the managed instance. Lookups by id first check the map (O(1)), which is why a second `findById` costs no SQL. It is also why entity `hashCode`/`equals` must not change while the entity sits in hash-based collections.

</details>

## L2

### JPA-L2-001 · recall
What is the N+1 query problem?

<details><summary>Answer</summary>

One query loads N parent rows, then accessing a lazy association on each one triggers one more query per parent: N+1 round trips in total. It grows linearly with data and is invisible with small test data.

</details>

### JPA-L2-002 · predict
`findAll()` returns 200 orders; for each you call `order.getCustomer().getName()` where `customer` is `@ManyToOne(fetch = LAZY)`, and there are 30 distinct customers. How many queries?

<details><summary>Answer</summary>

1 + 30 = 31: one for the orders, then one per distinct customer (customers already loaded are found in the persistence context). Without batch fetching or a join fetch, it is still N+1 in shape.

</details>

### JPA-L2-003 · recall
Name four ways to fix an N+1 problem.

<details><summary>Answer</summary>

1) A fetch join in JPQL (`join fetch o.items`). 2) `@EntityGraph(attributePaths = "items")` on the repository method. 3) Batch fetching (`@BatchSize` or `hibernate.default_batch_fetch_size`) so lazy loads become `IN (...)` queries. 4) Query a projection or aggregate (DTO, `SUM` in SQL) instead of entities.

</details>

### JPA-L2-004 · scenario
How would you prove in a test that an endpoint does not have an N+1 problem?

<details><summary>Answer</summary>

Count the SQL statements: enable Hibernate statistics (`hibernate.generate_statistics=true`) and assert on `getPrepareStatementCount()` after clearing them, or wrap the DataSource with a statement-counting proxy (datasource-proxy). Use enough rows (e.g. 30 parents) that N+1 would be obvious, and assert a small constant. The OrderTotals kata does this.

</details>

### JPA-L2-005 · bug
`select o from Order o join fetch o.items join fetch o.payments` combined with `Pageable`. What problems appear?

<details><summary>Answer</summary>

Fetching two collections at once produces a cartesian product (items × payments rows per order); Hibernate throws `MultipleBagFetchException` for two `List` bags. With pagination, a collection fetch join makes Hibernate load everything and paginate in memory (warning HHH90003004). Fix: page the ids first, then fetch one collection per query, or use batch fetching.

</details>

### JPA-L2-006 · recall
What causes `LazyInitializationException`?

<details><summary>Answer</summary>

Accessing an uninitialised lazy proxy or collection after its persistence context has closed, typically on an entity returned from a service (or repository call) outside a transaction, e.g. in a controller or during JSON serialisation with open-in-view disabled.

</details>

### JPA-L2-007 · why
The CatalogService bug passed all the existing `@DataJpaTest` tests but failed in production. Why?

<details><summary>Answer</summary>

`@DataJpaTest` makes every test method transactional, so the persistence context stays open for the whole test and lazy loading works anywhere. In production the service method had no transaction, so the repository's context closed before the lazy collection was touched. The kata's test runs with `@Transactional(propagation = NOT_SUPPORTED)` to behave like production.

</details>

### JPA-L2-008 · recall
What is open-session-in-view (open-in-view), what is its default in Spring Boot, and why disable it?

<details><summary>Answer</summary>

A web interceptor that keeps the persistence context open for the whole HTTP request, so lazy associations can load in the controller or during JSON rendering. Spring Boot enables it by default (and logs a warning). Disable it (`spring.jpa.open-in-view=false`) because it hides N+1 queries in serialisation, makes the web layer issue SQL outside transactions, and holds a connection for the whole request.

</details>

### JPA-L2-009 · design
Your controller returns an entity `Order` directly as JSON. List the problems.

<details><summary>Answer</summary>

Lazy associations either throw (`LazyInitializationException`) or trigger N+1 during serialisation (with open-in-view); bidirectional relationships can recurse infinitely; internal fields leak (password hashes, internal ids); the API contract changes whenever the schema changes; Hibernate proxies may serialise oddly. Map to response records inside the transaction instead.

</details>

### JPA-L2-010 · recall
By default, which exceptions make a Spring `@Transactional` method roll back?

<details><summary>Answer</summary>

Unchecked exceptions: `RuntimeException` and `Error`. Checked exceptions commit unless configured with `rollbackFor = Exception.class` (or a specific class); `noRollbackFor` does the opposite.

</details>

### JPA-L2-011 · bug
```java
@Transactional
public void importAll(List<Row> rows) {
    for (Row r : rows) {
        try { importOne(r); } catch (RuntimeException e) { log.warn("skip", e); }
    }
}
```
`importOne` is in another bean and also `@Transactional`. A row fails. What happens at the end?

<details><summary>Answer</summary>

`importOne` joins the outer transaction (REQUIRED). Its RuntimeException crosses its proxy, which marks the shared transaction rollback-only. The outer method swallows it and returns normally, and the commit fails with `UnexpectedRollbackException`: nothing is imported. Fix: `REQUIRES_NEW` per row (an independent transaction each), or no transaction on the outer method, or validate before writing.

</details>

### JPA-L2-012 · bug
`@Transactional` is placed on a `private` method that the public method of the same class calls. Does it work?

<details><summary>Answer</summary>

No. Spring's transactions are proxy-based: only calls coming through the proxy are intercepted, and private methods (and self-invocation in general) bypass it. Put the annotation on the public entry point, or move the method into another bean.

</details>

### JPA-L2-013 · recall
What does `@Transactional(readOnly = true)` do?

<details><summary>Answer</summary>

Spring sets the flush mode to manual and Hibernate skips dirty checking snapshots for that session (less memory and CPU for big reads); the JDBC connection is marked read-only (PostgreSQL can then refuse writes); and routing DataSources can send read-only transactions to a replica. It documents intent too.

</details>

### JPA-L2-014 · design
Where should `@Transactional` go: controller, service or repository? Why?

<details><summary>Answer</summary>

On service methods that represent a business operation (one unit of work, e.g. place an order: reserve stock, save order, write outbox). Controllers should not hold transactions (HTTP concerns, long durations); Spring Data repository methods are already transactional individually but cannot combine several calls into one unit.

</details>

### JPA-L2-015 · design
A product list page needs sku, name and price for 10,000 products. Entities or a projection? Why?

<details><summary>Answer</summary>

A projection (DTO or interface). Entities load every column, create managed instances plus dirty-checking snapshots and possibly proxies; projections load only the needed columns, no tracking, no lazy-loading traps. Entities are for when you will change and save the data.

</details>

### JPA-L2-016 · why
Why do IDENTITY ids prevent JDBC insert batching in Hibernate?

<details><summary>Answer</summary>

Each inserted entity must get its id back immediately after its INSERT (it becomes managed with that id), so Hibernate executes inserts one at a time. With SEQUENCE ids it knows the ids in advance and can send 50 inserts in one JDBC batch.

</details>

### JPA-L2-017 · scenario
A nightly job loads and updates 2 million entities in one transaction and runs out of memory. Why, and what do you do?

<details><summary>Answer</summary>

Every entity stays managed in the persistence context (plus its snapshot) until the transaction ends, so memory grows with every row. Process in chunks: flush and `clear()` every N entities (or one transaction per chunk), use JDBC batching, and better still a single set-based SQL `UPDATE` if the logic allows.

</details>

### JPA-L2-018 · recall
What is `@EntityGraph` used for?

<details><summary>Answer</summary>

To declare, per repository method, which lazy associations to fetch in the same query: `@EntityGraph(attributePaths = {"items", "customer"}) List<Order> findByStatus(Status s);`. It turns them into joins without writing a fetch-join JPQL query, and keeps the mapping LAZY by default.

</details>

### JPA-L2-019 · interview
"Our API got slow as data grew; there were no code changes." Where do you look first in a JPA application?

<details><summary>Answer</summary>

SQL per request: enable SQL logging/statistics or look at APM traces for N+1 patterns (hundreds of identical queries), missing indexes (`EXPLAIN ANALYZE` on the slow queries, now doing seq scans on bigger tables), unbounded queries (no pagination, OFFSET deep pages), over-fetching entities where projections would do, and connection pool waits caused by long transactions.

</details>

### JPA-L2-020 · dsa
Batch fetching with size 50 for 1,000 lazy collections: how many queries, and what is the general formula? Compare with N+1 and a join fetch.

<details><summary>Answer</summary>

1 + ceil(1000/50) = 21 queries. In general 1 + ceil(N/B). N+1 gives 1,001; a join fetch gives 1 (but a wider, duplicated result set and no database pagination). Batch fetching is the middle ground that keeps pagination working.

</details>

## L3

### JPA-L3-001 · recall
How does Spring implement `@Transactional` under the hood?

<details><summary>Answer</summary>

A proxy around the bean runs `TransactionInterceptor`, which asks the `PlatformTransactionManager` (`JpaTransactionManager`) to begin a transaction, binds the EntityManager and the JDBC connection to the current thread (`TransactionSynchronizationManager`), invokes the method, then commits or rolls back depending on the outcome and rollback rules.

</details>

### JPA-L3-002 · recall
REQUIRED vs REQUIRES_NEW vs NESTED.

<details><summary>Answer</summary>

REQUIRED (default): join the current transaction or start one; failures affect the whole transaction. REQUIRES_NEW: suspend the current one and run in a separate transaction that commits or rolls back independently (needs a second connection). NESTED: a savepoint inside the current transaction; a failure rolls back to the savepoint, but nothing commits unless the outer transaction commits (JDBC savepoints; with JPA, Hibernate's persistence context is not rolled back to the savepoint, so managed entities can be out of sync: use it with care).

</details>

### JPA-L3-003 · scenario
Failed login attempts must be recorded even though the login transaction rolls back. How?

<details><summary>Answer</summary>

Write the audit record in a method annotated `@Transactional(propagation = REQUIRES_NEW)` in a **separate bean** (so the call goes through the proxy). It commits independently before the outer transaction rolls back.

</details>

### JPA-L3-004 · bug
In the TransferService kata, `audit(...)` was annotated `REQUIRES_NEW` but the audit rows vanished when the transfer failed. Why?

<details><summary>Answer</summary>

`audit` was called from `transfer` in the same class (`this.audit(...)`), which bypasses the proxy, so the annotation did nothing: the audit ran inside the transfer's transaction and was rolled back with it. Moving it into a separate `TransferAudit` bean fixes it.

</details>

### JPA-L3-005 · why
Why can heavy use of REQUIRES_NEW deadlock an application under load?

<details><summary>Answer</summary>

The outer transaction keeps its connection while the inner one needs another from the pool. With a pool of 10 and 10 concurrent requests each holding one connection and waiting for a second, nobody can proceed until `connectionTimeout` expires. Size the pool for it, avoid REQUIRES_NEW on hot paths, or restructure (do the independent write before or after the main transaction).

</details>

### JPA-L3-006 · recall
What is `UnexpectedRollbackException`?

<details><summary>Answer</summary>

Thrown at commit when the transaction was marked rollback-only (by an inner participating method that threw) but the outer code returned normally and tried to commit. Spring rolls back and throws it so the caller does not believe the work succeeded.

</details>

### JPA-L3-007 · predict
A `@Transactional` method starts a `CompletableFuture.supplyAsync(() -> repo.save(x))` and then throws. Is `x` saved?

<details><summary>Answer</summary>

Yes, probably. The async task runs on another thread, which has no transaction bound to it (transactions are thread-bound), so `save` runs in its own repository transaction and commits independently of the caller's rollback. The same applies to `@Async` methods and parallel streams.

</details>

### JPA-L3-008 · recall
How does `@Version` optimistic locking work at the SQL level?

<details><summary>Answer</summary>

Hibernate adds the version to updates: `UPDATE product SET price = ?, version = 4 WHERE id = ? AND version = 3`. If another transaction changed the row first, zero rows match; Hibernate throws `OptimisticLockException`, which Spring translates into `ObjectOptimisticLockingFailureException`. The version is incremented on every successful update.

</details>

### JPA-L3-009 · design
Two admins edit the same product in a web UI at the same time. How do you prevent lost updates, end to end?

<details><summary>Answer</summary>

`@Version` on the entity; send the version to the client (a hidden field or an `ETag`); on save, apply the client's version (or check `If-Match`), so a stale edit fails with an optimistic locking exception; translate it into `409 Conflict` (or `412 Precondition Failed`) and let the user reload and reapply. The OptimisticLocking kata translates it into a `ConcurrentEditException`.

</details>

### JPA-L3-010 · scenario
An automated process (not a user) gets optimistic locking failures under contention. How do you handle them?

<details><summary>Answer</summary>

Retry the **whole transaction** (re-read the current state and re-apply the change) a bounded number of times with a little jitter, e.g. with Spring Retry or a loop around `TransactionTemplate`. Retrying only the save inside the same transaction does not work, since the context still holds the stale entity and the transaction is rollback-only. If conflicts are frequent, use a pessimistic lock or an atomic SQL update instead.

</details>

### JPA-L3-011 · recall
How do you take a pessimistic lock with Spring Data JPA, and when?

<details><summary>Answer</summary>

`@Lock(LockModeType.PESSIMISTIC_WRITE)` on a repository query method, which generates `SELECT ... FOR UPDATE`; optionally `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))`. Use it for short critical sections with frequent conflicts (allocating the last seats, counters), inside a transaction, locking rows in a consistent order.

</details>

### JPA-L3-012 · design
Decrementing stock: optimistic lock, pessimistic lock, or something else?

<details><summary>Answer</summary>

Often neither: one atomic conditional update `UPDATE inventory SET quantity = quantity - :q WHERE product_id = :id AND quantity >= :q` and check the affected row count (1 = reserved, 0 = insufficient). It is race-free, holds the row lock only for the statement, and needs no retry loop. Use `@Modifying @Query` in Spring Data.

</details>

### JPA-L3-013 · why
Why is sending an email or publishing a Kafka message inside a `@Transactional` method risky?

<details><summary>Answer</summary>

They are not part of the database transaction: if the transaction later rolls back (a constraint at commit, an exception after the send), the message has already gone out about something that never happened. And the send makes the transaction longer (holding locks and a connection during network I/O).

</details>

### JPA-L3-014 · recall
What does `@TransactionalEventListener(phase = AFTER_COMMIT)` give you, and what is its limitation?

<details><summary>Answer</summary>

The listener runs only after the publishing transaction commits, so you never notify about rolled-back work. Limitation: it runs in memory; if the process crashes between commit and the listener, the event is lost. Also, by then the transaction is over, so database writes in the listener need a new transaction (`REQUIRES_NEW`).

</details>

### JPA-L3-015 · design
Explain the transactional outbox pattern.

<details><summary>Answer</summary>

In the same database transaction as the business change, insert an event row into an `outbox` table. A separate relay (a poller with `FOR UPDATE SKIP LOCKED`, or CDC like Debezium) publishes outbox rows to the broker and marks them sent. Business change and event commit atomically; delivery is at-least-once, so consumers must be idempotent (dedupe by event id).

</details>

### JPA-L3-016 · recall
Which isolation level does PostgreSQL use by default, and how do you change it for one Spring method?

<details><summary>Answer</summary>

READ COMMITTED. Per method: `@Transactional(isolation = Isolation.REPEATABLE_READ)` (or SERIALIZABLE, which then requires retrying on serialization failures, SQLSTATE 40001).

</details>

### JPA-L3-017 · recall
What is the Hibernate second-level cache, and why is it often not the first choice?

<details><summary>Answer</summary>

A cache of entity state shared across sessions/transactions (and optionally query results), backed by e.g. Caffeine/Ehcache/Infinispan. Useful for read-mostly reference data. Often not first choice: invalidation across multiple instances needs a distributed or clustered cache, bulk updates and native SQL bypass it, and its behaviour is subtle; caching DTOs at the service level (Spring Cache) is easier to reason about.

</details>

### JPA-L3-018 · scenario
A service method calls a slow payment API (2 s) inside `@Transactional`. Under load, requests start failing with "Connection is not available, request timed out". Explain and fix.

<details><summary>Answer</summary>

Each request holds a pooled connection (and possibly row locks) for the whole 2 s call, so a pool of 10 sustains only ~5 such requests/second; others wait and time out. Move the remote call outside the transaction: short transaction to reserve/record intent, call the provider, short transaction to record the result (with idempotency keys and a status column to recover from crashes in between).

</details>

### JPA-L3-019 · interview
"Walk me through what happens when a Spring `@Transactional` service method saves an entity with JPA and returns."

<details><summary>Answer</summary>

The caller hits the proxy; the interceptor begins a transaction through `JpaTransactionManager`: gets a connection from Hikari, sets autocommit false, creates an EntityManager bound to the thread. `save` persists (an INSERT immediately for IDENTITY, otherwise queued) and the entity becomes managed; later changes are tracked. On return, Hibernate flushes (dirty checking → UPDATEs, queued INSERTs), the connection commits, the EntityManager closes (entities become detached), the connection returns to the pool, and after-commit synchronizations (e.g. `@TransactionalEventListener`) run. An unchecked exception instead rolls everything back.

</details>

### JPA-L3-020 · dsa
Two transactions lock accounts A and B in opposite orders and deadlock. What graph concept describes the deadlock, and what ordering rule prevents it?

<details><summary>Answer</summary>

A cycle in the wait-for graph (T1 waits for T2's lock, T2 waits for T1's); PostgreSQL detects the cycle and aborts one transaction. Prevent it by imposing a total order on lock acquisition, e.g. always lock the smaller account id first (`select ... where id in (:a, :b) order by id for update`), so a cycle cannot form.

</details>
