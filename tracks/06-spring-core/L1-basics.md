# Spring core, L1: inversion of control and dependency injection

**Goal:** understand what Spring actually is underneath Spring Boot: a container that creates your objects and connects them. If you can explain this page, "Spring magic" becomes plain Java plus a few rules.

Prerequisite: Java core L1-L2. Katas: `cd katas && ./mvnw test -Dgroups="spring & L1"`. Drill: `java quiz/Quiz.java --track spring-core --level 1`.

---

## 1. The problem Spring solves

Without a container, objects create their own dependencies:

```java
public class OrderService {
    private final ProductRepository products = new JdbcProductRepository(new HikariDataSource(...));  // hard-wired
    private final PaymentGateway payments = new StripeGateway("sk_live_...");                         // in a test? oops
}
```

- You cannot test `OrderService` without a database and a real payment provider.
- Every class knows how to build every other class; changing one constructor ripples everywhere.
- Lifecycles (one connection pool for the whole app, closed at shutdown) have no owner.

**Dependency injection (DI):** a class *declares* what it needs (constructor parameters) and something else supplies it:

```java
public class OrderService {
    private final ProductRepository products;
    private final PaymentGateway payments;

    public OrderService(ProductRepository products, PaymentGateway payments) {   // "give me these"
        this.products = products;
        this.payments = payments;
    }
}
// in a test: new OrderService(new InMemoryProducts(), new FakePayments())
```

**Inversion of control (IoC):** the "something else" is a framework: instead of your code creating and calling the infrastructure, the framework creates your objects and calls them. Spring's **ApplicationContext** is that container.

---

## 2. Beans and the container

- A **bean** is an object whose lifecycle the container manages: created by Spring, its dependencies injected, stored by name and type, shared, and destroyed at shutdown.
- A **bean definition** is the recipe: class (or factory method), scope, dependencies, init/destroy callbacks.
- The container reads definitions (from `@Configuration` classes, component scanning, Boot's auto-configuration), builds a **dependency graph**, creates beans in dependency order, and hands them out.

```java
var context = new AnnotationConfigApplicationContext(ShopConfig.class);   // plain Spring, no Boot
OrderService orders = context.getBean(OrderService.class);                // by type
Object pool = context.getBean("connectionPool");                         // by name
context.close();                                                          // destroy callbacks run
```

In a real application you almost never call `getBean`: beans get each other injected, and the framework (MVC, the scheduler) calls into them.

---

## 3. How injection resolves (and what fails)

For each constructor parameter, Spring looks for a bean of that **type**:
- exactly one candidate → injected;
- **none** → `NoSuchBeanDefinitionException` ("required a bean of type X that could not be found") at **startup**;
- **several** → `NoUniqueBeanDefinitionException`, unless one is `@Primary`, or the parameter says which with `@Qualifier("name")`, or (fallback) the parameter name matches a bean name;
- `List<PaymentGateway>` / `Map<String, PaymentGateway>` → **all** candidates (plugin-style designs);
- `Optional<X>` / `ObjectProvider<X>` → optional or lazy.

Singletons (the default scope) are created **once**, eagerly at startup, so wiring errors fail the start instead of the first request. That is a feature: a pod that does not start is caught by the deploy; a pod that fails on the first payment is caught by a customer.

**Circular dependencies** (A needs B, B needs A) with constructor injection cannot be satisfied: Spring Boot refuses them by default and prints the cycle. It is a design smell: extract the shared part into a third bean, or invert one dependency with an event.

> **Kata `MiniContainer`**: build this yourself: reflection, constructor injection, singletons, interface resolution, ambiguity, and cycle detection with a DFS. After this kata, Spring's error messages read like your own.

---

## 4. Three ways to define beans

1. **Component scanning:** annotate classes `@Component` (or the specialisations `@Service`, `@Repository`, `@Controller`, which add meaning and, for `@Repository`, exception translation) and let Spring find them in a package (`@ComponentScan`; Boot scans the package of the `@SpringBootApplication` class and below). Your own classes.
2. **Java configuration:** `@Bean` methods in a `@Configuration` class. For classes you do not own (a `DataSource`, a client from a library), or when construction needs logic.
3. **Auto-configuration** (Spring Boot): `@Bean` definitions in library jars, applied when conditions hold ("Jackson is on the classpath and you have not defined your own ObjectMapper"). Track 07.

---

## 5. Java configuration in practice

```java
@Configuration
public class ShopConfig {

    @Bean
    @Primary                                         // the default TaxPolicy
    TaxPolicy germanVat() { return new GermanVat(); }

    @Bean
    TaxPolicy noTax() { return new NoTax(); }        // bean name = method name: "noTax"

    @Bean
    PriceCalculator exportPrices(@Qualifier("noTax") TaxPolicy taxes) {   // Spring injects @Bean method parameters
        return new PriceCalculator(taxes);
    }
}
```

- `@Configuration` classes are proxied (CGLIB) so that calling one `@Bean` method from another returns the **same** singleton instead of a new object. Prefer method parameters anyway: clearer and works without the proxy (`@Configuration(proxyBeanMethods = false)`, Boot's default for its own configs).
- Keep domain classes free of Spring annotations where practical (as in the kata): they stay plain Java, easy to test and reuse.

> **Kata `ShopConfig`**: two implementations of the same interface, a default, and an explicit choice.

---

## 6. Constructor, setter or field injection

| | Constructor | Setter | Field (`@Autowired private X x;`) |
|---|---|---|---|
| Dependencies are visible in the API | yes | partly | no |
| `final` fields, immutable bean | yes | no | no |
| Testable without Spring | `new Service(fake)` | yes | needs reflection |
| Required dependency enforced | yes | no | no |
| Too many dependencies stand out | yes (long constructor = smell) | no | no |

Use **constructor injection** (with a single constructor no `@Autowired` is needed). Setter injection only for genuinely optional, changeable dependencies. Field injection only in tests, if at all.

---

## 7. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `MiniContainer` | IoC, constructor injection, singletons, resolution rules, errors | DFS on the dependency graph, cycle detection |
| `ShopConfig` | `@Configuration`, `@Bean`, `@Primary`, `@Qualifier`, a plain `ApplicationContext` | |

```
cd katas && ./mvnw test -Dgroups="spring & L1"
```

## 8. Seen in the shop project

- Constructor injection everywhere, `final` fields, no `@Autowired`: e.g. `OrderService`, `ProductService`.
- `@Configuration` + `@Bean` for infrastructure: `CacheConfig`, `JwtConfig`, `ReadReplicaConfig`, `RateLimitConfig`.
- `@Service`, `@Repository` (Spring Data interfaces), `@RestController` picked up by component scanning from `ShopApplication`'s package.

## 9. Interview checklist

1. What are IoC and dependency injection? What problem do they solve?
2. What is a bean? What is the ApplicationContext?
3. How does Spring choose which bean to inject? What happens with zero or two candidates?
4. `@Primary` vs `@Qualifier`.
5. Constructor vs field injection: which do you use, and why?
6. `@Component` vs `@Bean`: when each?
7. What is the difference between `@Component`, `@Service`, `@Repository` and `@Controller`?
8. Why does Spring create singletons eagerly at startup?
9. How would you handle a circular dependency between two services?
