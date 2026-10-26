# JPA and transactions, L1: entities, repositories, queries

**Goal:** map tables to Java objects with JPA/Hibernate, use Spring Data repositories, write JPQL and native queries, and understand the persistence context: the one idea that explains almost all JPA behaviour (and bugs).

Prerequisite: SQL track L1-L2, Spring Boot L1. Katas: `cd katas && ./mvnw test -Dgroups="jpa & L1"` (real PostgreSQL, schema from Flyway). Drill: `java quiz/Quiz.java --track jpa-transactions --level 1`.

---

## 1. The layers

```
your code -> Spring Data repository (interface)      CustomerRepository.findByEmail(...)
          -> JPA (jakarta.persistence API)            EntityManager, @Entity, JPQL
          -> Hibernate (the JPA implementation)        SQL generation, caching, dirty checking
          -> JDBC -> HikariCP pool -> PostgreSQL
```

- **JPA** is a specification (annotations + `EntityManager` API); **Hibernate** implements it; **Spring Data JPA** generates repository implementations on top.
- An **ORM** maps objects to rows. It saves you boilerplate, and it hides SQL: every performance problem in JPA is a SQL problem you did not see. Turn on SQL logging (`logging.level.org.hibernate.SQL=debug`) while learning; the katas do.
- **Flyway** owns the schema (versioned migrations, SQL track L3); Hibernate only **validates** it (`spring.jpa.hibernate.ddl-auto=validate`). Never use `create`/`update` against a real database.

---

## 2. Entities

```java
@Entity
@Table(name = "jpa_customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // the database assigns the id
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)                           // store "GOLD", never the ordinal 1
    private Tier tier;

    protected Customer() {}                                // JPA needs a no-arg constructor (may be protected)
    public Customer(String fullName, Tier tier) { ... }    // your real constructor
}
```

- An entity has an identity (`@Id`) and a lifecycle; it is **mutable** and managed (not a record).
- **Id generation:** `IDENTITY` (PostgreSQL identity column): Hibernate must `INSERT` immediately at `persist`/`save` to learn the id (so constraint violations surface at `save()`, and batch inserts are disabled). `SEQUENCE` (with `allocationSize`) lets Hibernate fetch ids in blocks and batch inserts. UUIDs assigned in Java avoid the round trip entirely.
- `@Enumerated(EnumType.STRING)` always: `ORDINAL` (the default) stores positions, and reordering the enum silently changes the meaning of existing rows.
- `equals`/`hashCode` on entities: base them on the id carefully (it is null before persist), or do not override them; never on mutable business fields used in hash sets.
- Field access (annotations on fields) is simplest; keep setters for real business operations (`upgrade()`), not for every field.

---

## 3. Relationships

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)     // many orders -> one customer; FK column on this table
@JoinColumn(name = "customer_id")
private Customer customer;

@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)   // the other side: no column here
private List<OrderItem> items = new ArrayList<>();
```

- The side with the **foreign key** (`@ManyToOne` + `@JoinColumn`) **owns** the relationship; `mappedBy` marks the inverse side. Only the owning side's changes are written.
- **Fetch types:** `@ManyToOne` and `@OneToOne` are **EAGER by default** in JPA: always write `fetch = LAZY`. Collections (`@OneToMany`, `@ManyToMany`) are lazy by default: keep them lazy.
- A lazy reference is a **proxy** (or an uninitialised collection) that loads on first access, **if** the persistence context is still open (L2: `LazyInitializationException`).
- **Cascade** (`PERSIST`, `MERGE`, `REMOVE`, `ALL`) propagates operations from parent to children; use it for true parent-child aggregates (order → items), never towards shared entities (item → product). `orphanRemoval` deletes children removed from the collection.
- `@ManyToMany` is rarely right in practice: model the join table as an entity once it needs any attribute (quantity, created_at).

> **Kata `Mapping`**: map Customer and PurchaseOrder to an existing schema; Hibernate validates you.

---

## 4. The persistence context (the key idea)

The `EntityManager` keeps a **persistence context**: a first-level cache of the entities loaded or saved in the current transaction, one Java object per row.

- **Identity:** loading the same id twice in one context returns the **same object**; no second SELECT.
- **Dirty checking:** at flush (before a query that needs it, and at commit), Hibernate compares each managed entity with its snapshot and issues `UPDATE`s for the changed ones. So inside a transaction you **change the object and do not call `save`**; the shop's services rely on this.
- **Flush** writes pending changes to the database (still inside the transaction); **commit** makes them permanent. `em.clear()` forgets everything (the katas use it to force real reads).
- Entity states: **transient** (new, unknown), **managed** (in the context), **detached** (the context closed, e.g. after the transaction, or a copy from a form), **removed**.
- `repository.save(entity)` calls `persist` for new entities and `merge` for existing ones (merge copies a detached entity's state onto a managed copy).

With Spring, one transaction ≈ one persistence context: it opens with `@Transactional` and closes at commit.

---

## 5. Spring Data repositories and queries

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);                                   // derived from the method name
    List<Product> findByCategoryAndPriceCentsLessThanOrderByPriceCents(String c, long max);   // also derived (but long)

    @Query("select p from Product p where p.category = :category and p.priceCents < :max order by p.priceCents")
    List<Product> cheapIn(@Param("category") String category, @Param("max") long max);       // JPQL: entities and fields

    interface CategoryTotal { String getCategory(); long getProducts(); }                     // interface projection
    @Query("select p.category as category, count(p) as products from Product p group by p.category")
    List<CategoryTotal> totals();

    @Modifying(clearAutomatically = true)                                       // bulk UPDATE/DELETE bypasses the context
    @Query("update Product p set p.priceCents = p.priceCents * 110 / 100 where p.category = :c")
    int raise(@Param("c") String c);

    @Query(value = "select sku from jpa_product where name ilike :p", nativeQuery = true)      // real SQL, PostgreSQL features
    List<String> skusLike(@Param("p") String pattern);
}
```

- `JpaRepository` gives `save`, `findById`, `findAll(Sort/Pageable)`, `deleteById`, `count`, ...
- **JPQL** queries entities and fields, not tables and columns; it is portable and type-checked against the mapping at startup. Use **native** queries for database-specific features (window functions, `ILIKE`, JSON operators, `FOR UPDATE SKIP LOCKED`).
- **Projections** (interfaces or records/DTOs) load only the columns you need, without managed entities: ideal for read-only screens and reports.
- **Bulk updates** (`@Modifying`) run directly in the database: entities already loaded keep their old values unless the context is cleared (`clearAutomatically`).
- Parameters are always bound (`:name`), never concatenated: no SQL injection.
- Pagination: `Page<T> findAll(Pageable)` (offset + a count query) or `Slice` (no count); for large tables, keyset queries (SQL track L2).

> **Kata `Queries`**: JPQL, an interface projection, a bulk update, and a native query.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `Mapping` (`l1/mapping`) | `@Entity`, `@Table`, `@Column`, identity ids, `@Enumerated(STRING)`, lazy `@ManyToOne`, dirty checking |
| `Queries` (`l1/queries`) | derived queries, `@Query` JPQL, projections, `@Modifying`, native queries |

```
cd katas && ./mvnw test -Dgroups="jpa & L1"
```

The schema is `katas/src/main/resources/db/migration/V1__jpa_katas.sql`. The test output shows every SQL statement: read them.

## 7. Seen in the shop project

- Entities with `@Enumerated(EnumType.STRING)` (`User.role`), lazy relationships, `@Version` (`Product`).
- Spring Data repositories with `@Query` and `@Modifying` (`InventoryRepository`), native queries for keyset pagination and the outbox (`ProductRepository`, `OutboxWorker`).
- Flyway migrations + `ddl-auto=validate`.

## 8. Interview checklist

1. JPA vs Hibernate vs Spring Data JPA.
2. What is the persistence context? What are dirty checking and flushing?
3. Why do you not need to call `save()` after changing a managed entity?
4. Entity states: transient, managed, detached, removed.
5. IDENTITY vs SEQUENCE id generation: consequences.
6. Why `EnumType.STRING`?
7. Owning side vs inverse side of a relationship. What does `mappedBy` mean?
8. Default fetch types of `@ManyToOne` and `@OneToMany`. What do you set, and why?
9. JPQL vs native SQL. When do you need native?
10. What does `@Modifying(clearAutomatically = true)` protect you from?
11. Why should Hibernate never create or update the production schema?
