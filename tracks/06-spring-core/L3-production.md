# Spring core, L3: proxies, AOP and the container's internals

**Goal:** know how `@Transactional`, `@Cacheable`, `@Async` and `@PreAuthorize` actually work (proxies), why they silently do nothing in some cases, how Spring extends itself (post-processors), and the startup and performance consequences. These are the senior Spring interview questions and the source of many production bugs.

Prerequisite: L1, L2, Java core L2 (interfaces, reflection basics). Katas: `cd katas && ./mvnw test -Dgroups="spring & L3"`. Drill: `java quiz/Quiz.java --track spring-core --level 3`.

---

## 1. Proxies

When a bean has cross-cutting behaviour (transactions, caching, security, retries, async), Spring does not change your class. It creates a **proxy**: another object with the same type that runs extra code ("advice") around calls and delegates to your object (the "target"). Other beans get the **proxy** injected.

```
caller --> [ proxy: begin transaction | cache lookup | security check ] --> target.placeOrder()
                    <-- commit/rollback | store in cache <--
```

Two kinds:
- **JDK dynamic proxies** (`java.lang.reflect.Proxy`): implement the bean's **interfaces**; only interface methods can be advised; the proxy is not an instance of the target class.
- **CGLIB proxies**: a generated **subclass** of the bean's class, overriding its methods; used when there is no interface, and by default in Spring Boot (`spring.aop.proxy-target-class=true`). Consequence: `final` classes and `final`/`private`/`static` methods cannot be advised (they cannot be overridden).

**Several proxies on one bean nest, and the order matters.** A method with both `@Cacheable` and `@Transactional` is wrapped twice; the advice with the smaller order number is the outer one. The shop project's `CacheConfig` sets `@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)` so the cache is *outside* the transaction: a cache hit returns before a transaction (and a database connection) is taken, and `@CacheEvict` runs after the commit. With equal orders, the nesting depends on registration order, which is an accident waiting to happen.

> **Kata `TimingProxy`**: a JDK dynamic proxy that records each call and rethrows the target's original exceptions (unwrapping `InvocationTargetException`), the core of every Spring advice.

---

## 2. The self-invocation trap

```java
@Service
public class PriceService {
    @Cacheable("basePrices")
    public long basePrice(String sku) { ... }

    public long priceWithVat(String sku) {
        return Math.round(basePrice(sku) * 1.19);   // this.basePrice(): NOT cached
    }
}
```

`priceWithVat` runs on the **target**, so `basePrice(sku)` is a plain Java call on `this`, which never passes through the proxy. No cache, no transaction, no security check, no async. The same for:
- `@Transactional` on a method called from the same class (the most common "my transaction does not roll back" bug);
- `@Async` called internally (runs synchronously);
- advice on `private` methods (never proxied) or `final` methods (CGLIB cannot override).

Fixes, best first: move the advised method to **another bean** and inject it; restructure so the entry point itself carries the annotation; inject a lazy reference to yourself (`@Lazy` self-injection or `ObjectProvider<PriceService>`), ugly but works; or AspectJ compile/load-time weaving (advice woven into the bytecode, works on self-calls), rarely worth it.

> **Kata `PriceService`**: the bug with a real Spring cache, and the fix.

---

## 3. How Spring extends itself: post-processors

- A **BeanFactoryPostProcessor** changes bean **definitions** before any bean is created (e.g. `PropertySourcesPlaceholderConfigurer` resolves `${...}`; `ConfigurationClassPostProcessor` reads `@Configuration`, `@ComponentScan`, `@Import`).
- A **BeanPostProcessor** sees each bean **instance** after creation and may **return a different object**. `AutowiredAnnotationBeanPostProcessor` performs `@Autowired` injection; `CommonAnnotationBeanPostProcessor` calls `@PostConstruct`; and the **auto-proxy creator** wraps beans that need advice (transactions, caching, security...) in proxies in `postProcessAfterInitialization`.
- Post-processors are created **before** normal beans (declare their `@Bean` methods `static`), and beans they depend on are created too early to be post-processed themselves (the "is not eligible for getting processed by all BeanPostProcessors" log message).

> **Kata `AuditPostProcessor`**: your own BeanPostProcessor that wraps `@Audited` beans in proxies, so every bean that injects them gets the audited version.

---

## 4. AOP vocabulary

| Term | Meaning |
|---|---|
| Aspect | a module of cross-cutting behaviour (`@Aspect` class) |
| Advice | the code to run: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@After`, `@Around` |
| Join point | a point where advice can run; in Spring AOP always a **method execution** on a proxied bean |
| Pointcut | an expression selecting join points: `execution(* dojo..*Service.*(..))`, `@annotation(Audited)` |
| Weaving | linking aspects with targets: Spring AOP at runtime via proxies; AspectJ at compile/load time |

```java
@Aspect @Component
class SlowCallLogger {
    @Around("@annotation(dojo.Timed)")
    Object time(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        try { return call.proceed(); }
        finally { log.info("{} took {} ms", call.getSignature(), (System.nanoTime() - start) / 1_000_000); }
    }
}
```

Use AOP for genuinely cross-cutting concerns (transactions, metrics, security, auditing). Business logic in aspects is hard to find and debug.

---

## 5. Startup, memory and performance

- **Startup cost** grows with the number of beans, classpath scanning, auto-configuration conditions and reflection. Typical Spring Boot services start in 2-10 s. Options: narrower scanning, lazy initialisation (`spring.main.lazy-initialization=true`: faster start, but errors move to the first request), **Spring AOT** + **GraalVM native images** (start in tens of ms, smaller memory, longer builds, reflection hints needed), CDS/AppCDS, CRaC (checkpoint/restore).
- **Proxy overhead** per call is small (nanoseconds to a microsecond) compared with I/O; the real costs are what the advice does (a transaction, a cache lookup).
- **Fail at startup, not at the first request**: eager singletons, `@ConfigurationProperties` with validation, required properties without defaults. A pod that does not start never receives traffic.
- **Context hierarchy and testing**: each different test configuration creates a new ApplicationContext (seconds each); Spring's test framework caches contexts by configuration, so keep test configurations uniform (track 10).

---

## 6. Circular dependencies, revisited

With constructor injection a cycle cannot be built; Spring Boot (since 2.6) refuses cycles even with field injection unless `spring.main.allow-circular-references=true`. Old field-injection cycles "worked" by injecting a not-yet-initialised early reference, which is fragile (especially with proxies: the early reference may be the raw target, not the proxy). Break cycles by design: extract a third bean, use events, or pass the needed value as a method parameter.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `TimingProxy` | JDK dynamic proxies, `InvocationHandler`, unwrapping exceptions |
| `PriceService` (bug) | self-invocation, `@Cacheable`, proxies in a real context |
| `AuditPostProcessor` | `BeanPostProcessor`, replacing beans with proxies, static post-processor beans |

```
cd katas && ./mvnw test -Dgroups="spring & L3"
```

## 8. Seen in the shop project

- `@Cacheable` / `@CacheEvict` on `ProductService` methods called from controllers (through the proxy), and `CacheConfig`'s explicit advice order: cache outside the transaction (Level 4).
- `@Transactional` on service methods that are entry points, not on internal helpers.
- Spring Modulith's `ModularityTest` checks module dependencies at build time (Level 6).

## 9. Interview talk track

"Spring adds behaviour like transactions, caching and security by putting a proxy in front of the bean: a JDK dynamic proxy for interfaces, or, by default in Boot, a CGLIB subclass. Callers get the proxy, the proxy runs the advice and delegates. That explains the classic bugs: calling an @Transactional or @Cacheable method from the same class bypasses the proxy, private and final methods are never advised, and an exception swallowed inside the method means no rollback. The proxies are created by the auto-proxy creator, a BeanPostProcessor that replaces beans after initialisation; the container itself is extended through BeanFactoryPostProcessors for definitions and BeanPostProcessors for instances. I keep cross-cutting concerns in aspects and business logic out of them, avoid circular dependencies by design, and make misconfiguration fail at startup."

**Questions you should be able to answer:**
1. How does `@Transactional` work under the hood?
2. JDK dynamic proxy vs CGLIB: differences and limitations.
3. Why does calling an `@Transactional` method from the same class not start a transaction? Three fixes.
4. Can you put `@Transactional` on a private method? On a final class?
5. BeanFactoryPostProcessor vs BeanPostProcessor, with an example of each.
6. Why should `@Bean` methods for post-processors be static?
7. Aspect, advice, pointcut, join point, weaving: define each.
8. When is AOP the right tool, and when is it a bad idea?
9. How do you make a Spring Boot service start faster? What does each option cost?
10. Why are circular dependencies refused, and how do you break one?
