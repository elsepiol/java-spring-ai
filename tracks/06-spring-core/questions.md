# Spring core: question bank

Drill with `java quiz/Quiz.java --track spring-core`.

## L1

### SPRC-L1-001 · recall
What are inversion of control and dependency injection?

<details><summary>Answer</summary>

**DI**: a class receives its dependencies (usually through its constructor) instead of creating them. **IoC**: control over object creation and wiring is handed to a framework (the container), which creates objects, injects dependencies and calls into your code. Result: loosely coupled, testable classes (inject fakes), one place for wiring and lifecycles.

</details>

### SPRC-L1-002 · recall
What is a Spring bean, and what is the ApplicationContext?

<details><summary>Answer</summary>

A bean is an object created, configured and managed by the Spring container (dependencies injected, lifecycle callbacks, scope, destruction). The ApplicationContext is the container: it reads bean definitions (configuration classes, scanning, auto-configuration), creates beans in dependency order, and adds events, resource loading, the Environment and internationalisation on top of the basic BeanFactory.

</details>

### SPRC-L1-003 · scenario
Startup fails with "Parameter 0 of constructor in OrderService required a bean of type PaymentGateway that could not be found." What are the likely causes?

<details><summary>Answer</summary>

No bean of that type exists in the context: the implementation is not annotated (`@Component`/`@Service`), it lives outside the scanned packages (below the `@SpringBootApplication` class), its `@Bean` method's configuration is not loaded, a `@Profile` or `@Conditional...` excluded it, or its auto-configuration did not apply (missing dependency/property). Run with `--debug` to see the conditions report.

</details>

### SPRC-L1-004 · recall
Two beans implement `PaymentGateway`. How does Spring choose one for a constructor parameter?

<details><summary>Answer</summary>

It fails with `NoUniqueBeanDefinitionException` unless: one is marked `@Primary`; the parameter has `@Qualifier("beanName")`; or the parameter name matches a bean name (fallback). Injecting `List<PaymentGateway>` or `Map<String, PaymentGateway>` gives all of them.

</details>

### SPRC-L1-005 · why
Why prefer constructor injection over field injection?

<details><summary>Answer</summary>

Dependencies become explicit and required, fields can be `final` (immutable, thread-safe publication), the class can be created in a unit test with `new` and fakes without Spring or reflection, and a constructor with ten parameters makes a design problem visible. Field injection hides dependencies and allows half-initialised objects.

</details>

### SPRC-L1-006 · recall
`@Component` vs `@Bean`?

<details><summary>Answer</summary>

`@Component` (and `@Service`, `@Repository`, `@Controller`) on a class you own: found by component scanning, Spring calls its constructor. `@Bean` on a method in a `@Configuration` class: you write the construction code yourself; used for third-party classes you cannot annotate or when construction needs logic or configuration values.

</details>

### SPRC-L1-007 · recall
What do `@Service`, `@Repository` and `@Controller` add over `@Component`?

<details><summary>Answer</summary>

All are stereotypes of `@Component` (scanned the same way). They document the role. `@Repository` additionally enables persistence exception translation (vendor SQL exceptions → Spring's `DataAccessException` hierarchy). `@Controller`/`@RestController` are detected by Spring MVC as request handlers.

</details>

### SPRC-L1-008 · why
Why does Spring create singleton beans eagerly at startup?

<details><summary>Answer</summary>

To detect configuration and wiring errors immediately: missing beans, cycles, invalid properties, failing connections fail the start, so a broken deployment never receives traffic. It also moves expensive initialisation out of the first request. Lazy initialisation trades that safety for a faster start.

</details>

### SPRC-L1-009 · predict
A `@Configuration` class has `@Bean A a() { return new A(b()); }` and `@Bean B b() { return new B(); }`. How many B instances exist?

<details><summary>Answer</summary>

One. `@Configuration` classes are CGLIB-proxied (full mode): the call to `b()` is intercepted and returns the singleton bean. With `@Configuration(proxyBeanMethods = false)` (lite mode), `b()` is a plain method call and creates a **second** B that is not a bean. Prefer injecting B as a method parameter: `A a(B b)`.

</details>

### SPRC-L1-010 · recall
Where does Spring Boot look for components to scan by default?

<details><summary>Answer</summary>

The package of the class annotated `@SpringBootApplication` and all its sub-packages. Put that class in the root package (`dev.elsepiol.shop`), or components in sibling packages are silently not found.

</details>

### SPRC-L1-011 · recall
What is the default bean scope, and what does it imply for your code?

<details><summary>Answer</summary>

Singleton: one instance per container, shared by all threads. The bean must be thread-safe: no per-request mutable state in fields; dependencies and configuration in final fields only.

</details>

### SPRC-L1-012 · scenario
Startup fails: "The dependencies of some of the beans in the application context form a cycle: orderService -> paymentService -> orderService". How do you fix it?

<details><summary>Answer</summary>

By design: extract the logic both need into a third bean that both depend on; or reverse one dependency through an event (paymentService publishes PaymentCompleted, orderService listens); or pass the needed data as a method argument instead of a dependency. Workarounds (`@Lazy` on one parameter, `allow-circular-references`) hide the coupling.

</details>

### SPRC-L1-013 · why
Why keep domain classes free of Spring annotations when practical?

<details><summary>Answer</summary>

They stay plain Java: unit-testable without a container, reusable outside Spring, and the domain does not depend on the framework. Wiring lives in configuration classes (`@Bean`) or thin `@Service` adapters. It is a trade-off: for application services, `@Service` with constructor injection is perfectly fine.

</details>

### SPRC-L1-014 · dsa
How does a DI container decide the order in which to create beans, and detect a cycle?

<details><summary>Answer</summary>

Beans and their dependencies form a directed graph. Creating a bean first creates its dependencies recursively (a depth-first traversal): dependencies are built before dependents (a topological order). Keeping a set of beans "currently in creation" detects cycles: meeting a bean that is already in creation means a back edge, i.e. a cycle, and the set gives the path.

</details>

### SPRC-L1-015 · recall
What does `@Autowired` do, and when can it be omitted?

<details><summary>Answer</summary>

It marks a constructor, setter, field or method for injection. Since Spring 4.3, a class with a **single** constructor gets it injected without `@Autowired`. With several constructors, mark the one to use. `@Autowired(required = false)` or `Optional<X>` for optional dependencies.

</details>

### SPRC-L1-016 · predict
You inject `List<Notifier>` and three beans implement Notifier. In which order are they in the list?

<details><summary>Answer</summary>

In the order defined by `@Order` / `Ordered` (lowest value first) if present; otherwise in registration order, which you should not rely on. Use `@Order` when the order matters (e.g. a chain of validators).

</details>

### SPRC-L1-017 · scenario
A unit test needs `OrderService` but the developer starts the whole Spring context with `@SpringBootTest`. What is the cost, and what is the alternative?

<details><summary>Answer</summary>

Seconds per context start, database and other infrastructure needed, slow and brittle tests. With constructor injection: `new OrderService(new InMemoryProducts(), fakePayments)` in a plain JUnit test, milliseconds. Use Spring test slices or full contexts only for testing the wiring and integration (track 10).

</details>

### SPRC-L1-018 · recall
What is the difference between BeanFactory and ApplicationContext?

<details><summary>Answer</summary>

BeanFactory is the basic container (creating and wiring beans, lazily by default). ApplicationContext extends it with eager singleton creation, automatic registration of post-processors, events, the Environment and profiles, message sources and resource loading. Applications always use an ApplicationContext.

</details>

### SPRC-L1-019 · recall
What is `@Qualifier` for, and can you create your own qualifier annotation?

<details><summary>Answer</summary>

It selects a specific bean among several of the same type, by name (`@Qualifier("noTax")`) or by a custom qualifier annotation: define `@Qualifier @Retention(RUNTIME) @interface Export {}`, put `@Export` on the bean and on the injection point. Custom qualifiers are refactoring-safe (no strings).

</details>

### SPRC-L1-020 · why
Why is "new-ing" a dependency inside a Spring bean (e.g. `new RestClient()` in a service) a problem?

<details><summary>Answer</summary>

The object is not a bean: it gets no configuration (timeouts, interceptors, metrics, tracing), no lifecycle management (never closed), cannot be replaced in tests, and duplicates setup. Define it as a bean (or use the builder bean Boot provides, e.g. `RestClient.Builder`) and inject it.

</details>

## L2

### SPRC-L2-001 · recall
Name Spring's bean scopes.

<details><summary>Answer</summary>

singleton (default), prototype, and web scopes: request, session, application, websocket. Custom scopes can be registered (e.g. thread, refresh scope in Spring Cloud).

</details>

### SPRC-L2-002 · bug
A singleton injects a prototype `ReportBuilder` through its constructor and uses it per call. What goes wrong and how do you fix it?

<details><summary>Answer</summary>

The prototype is created once, when the singleton is created, so all calls (and threads) share one builder: data from previous calls leaks into new ones and concurrent calls corrupt each other. Fix: inject `ObjectProvider<ReportBuilder>` (or `Provider<ReportBuilder>`, a `@Lookup` method) and call `getObject()` per use.

</details>

### SPRC-L2-003 · recall
Walk through the lifecycle of a singleton bean.

<details><summary>Answer</summary>

Instantiate (constructor injection) → field/setter injection → Aware callbacks → BeanPostProcessors before initialisation → `@PostConstruct` / `afterPropertiesSet` / init method → BeanPostProcessors after initialisation (proxies created) → in use → on context close: `@PreDestroy` / `destroy()` / destroy method.

</details>

### SPRC-L2-004 · why
Why use `@PostConstruct` instead of doing work in the constructor?

<details><summary>Answer</summary>

With setter or field injection, dependencies are not set yet in the constructor; even with constructor injection, other post-processing (configuration binding, Aware callbacks) happens after it. `@PostConstruct` runs when the bean is fully configured. Also: constructors should be cheap; initialisation that can fail belongs to a clearly defined phase.

</details>

### SPRC-L2-005 · predict
Is `@PreDestroy` called on prototype beans when the context closes?

<details><summary>Answer</summary>

No. Spring initialises prototypes but does not track them; the client that obtained one is responsible for releasing its resources.

</details>

### SPRC-L2-006 · recall
In which priority order does Spring Boot read configuration (highest first, main ones)?

<details><summary>Answer</summary>

Command-line arguments > Java system properties > OS environment variables > profile-specific `application-{profile}.properties` (outside the jar, then inside) > `application.properties` (outside, then inside) > `@PropertySource` > defaults. Plus test properties and others at the top in tests. Higher sources override lower ones.

</details>

### SPRC-L2-007 · recall
`@Value` vs `@ConfigurationProperties`?

<details><summary>Answer</summary>

`@Value("${x.y:default}")` injects single values, anywhere, with SpEL support. `@ConfigurationProperties(prefix = "x")` binds a whole group into a typed object (a record), with relaxed binding, validation (`@Validated`), IDE metadata and one place to see all settings. Prefer `@ConfigurationProperties` for anything beyond a value or two.

</details>

### SPRC-L2-008 · scenario
In plain Spring (no Boot), a missing property `${payment.api-key}` does not fail the start; the bean gets the string `"${payment.api-key}"`. Why, and how does Boot differ?

<details><summary>Answer</summary>

Without a `PropertySourcesPlaceholderConfigurer`, the container's embedded value resolver resolves placeholders leniently, leaving unresolvable ones as literal text. Registering a `static @Bean PropertySourcesPlaceholderConfigurer` makes unresolvable placeholders an error. Spring Boot registers one automatically, so missing required properties fail the start.

</details>

### SPRC-L2-009 · recall
How do you map the property `payment.api-key` to an environment variable in Spring Boot?

<details><summary>Answer</summary>

`PAYMENT_APIKEY`: replace dots with underscores, remove dashes, upper-case (relaxed binding). Lists use indexes: `my.list[0]` → `MY_LIST_0_`. This is how containers and Kubernetes configure Boot apps.

</details>

### SPRC-L2-010 · recall
What does `@Profile("!prod")` mean, and how do you activate profiles?

<details><summary>Answer</summary>

The bean is registered when the `prod` profile is NOT active. Expressions support `!`, `&`, `|`. Activate with `spring.profiles.active=prod` (properties, env var `SPRING_PROFILES_ACTIVE`, command line), or in tests with `@ActiveProfiles`. `application-prod.properties` is loaded for that profile.

</details>

### SPRC-L2-011 · design
When should a difference between environments be a profile, and when a property?

<details><summary>Answer</summary>

Different **wiring** (a fake vs a real payment gateway, local files vs S3) → a profile selecting beans. Different **values** (URLs, timeouts, pool sizes, feature flags) → properties set per environment. Keep profiles few and meaningful; avoid a profile per server.

</details>

### SPRC-L2-012 · predict
A listener method annotated `@EventListener` throws an exception. What happens to the code that called `publishEvent`?

<details><summary>Answer</summary>

The exception propagates to the publisher: event listeners run synchronously in the publisher's thread by default (and inside its transaction). If the publisher is `@Transactional`, that transaction rolls back. Use `@Async` listeners or `@TransactionalEventListener` (after commit) to decouple failures.

</details>

### SPRC-L2-013 · recall
What does `@TransactionalEventListener(phase = AFTER_COMMIT)` guarantee and not guarantee?

<details><summary>Answer</summary>

It runs only if the publishing transaction commits (not on rollback), after the commit. It does not guarantee delivery: the event lives in memory, so if the process crashes after commit and before the listener runs, the reaction is lost; and listener failures do not roll back the (already committed) transaction. For guaranteed side effects use the transactional outbox.

</details>

### SPRC-L2-014 · recall
Which interfaces or annotations let a bean take part in startup and shutdown ordering beyond init/destroy?

<details><summary>Answer</summary>

`SmartLifecycle` (start/stop with phases, auto-startup, used for message listener containers and schedulers), `ApplicationRunner`/`CommandLineRunner` (run after startup), listening to `ApplicationReadyEvent`, `ContextClosedEvent`, and `@DependsOn` for initialisation order without a real dependency.

</details>

### SPRC-L2-015 · scenario
A request-scoped bean `CurrentTenant` must be used by a singleton service. How is that possible?

<details><summary>Answer</summary>

Inject a **scoped proxy**: `@RequestScope` (= `@Scope(value = "request", proxyMode = TARGET_CLASS)`). The singleton holds a proxy; each call on it is routed to the instance of the current request (found through a ThreadLocal of the request). Outside a request it throws. Often simpler: pass the tenant as a method parameter.

</details>

### SPRC-L2-016 · recall
What is `@Conditional`, and name three Boot conditions.

<details><summary>Answer</summary>

`@Conditional(SomeCondition.class)` registers a bean only if the condition matches. Boot's: `@ConditionalOnClass` (a class is on the classpath), `@ConditionalOnMissingBean` (the user did not define one), `@ConditionalOnProperty` (a property has a value), `@ConditionalOnWebApplication`, `@ConditionalOnBean`. They are the basis of auto-configuration.

</details>

### SPRC-L2-017 · why
Why should secrets not be in `application.properties` committed to git?

<details><summary>Answer</summary>

Everyone with repository access (and every fork, CI log, and backup) has them, and they cannot be rotated without a commit and a deploy. Provide secrets at runtime: environment variables from a secret store, Kubernetes secrets mounted as files (config tree), Vault/cloud secret managers; reference them as `${PAYMENT_API_KEY}` in configuration.

</details>

### SPRC-L2-018 · recall
How do you get all beans of a type, or one by name, optionally and lazily?

<details><summary>Answer</summary>

Inject `List<T>`/`Map<String, T>` for all; `ObjectProvider<T>` for optional or lazy access (`getIfAvailable()`, `getIfUnique()`, `getObject()`, `stream()`), or `Optional<T>`; by name with `@Qualifier("name")`. `ApplicationContext.getBean` only in framework-level code.

</details>

### SPRC-L2-019 · scenario
A config property `app.cache.ttl=10m` should become a `Duration`. How?

<details><summary>Answer</summary>

Bind it into `@ConfigurationProperties` with a `Duration` field: Boot converts `10m`, `30s`, `PT10M` (and plain numbers with `@DurationUnit`, default milliseconds). Same for `DataSize` (`10MB`). Validate it with `@Validated` and constraints so a bad value fails the start.

</details>

### SPRC-L2-020 · design
How would you add a new notification channel (SMS next to email) without changing the order code?

<details><summary>Answer</summary>

Define an interface `Notifier`; implement `EmailNotifier`, `SmsNotifier` as beans; the order code publishes an event (or injects `List<Notifier>` and calls each). Adding a channel = adding a bean (open/closed principle), optionally behind `@ConditionalOnProperty` to toggle it per environment.

</details>

## L3

### SPRC-L3-001 · interview
How does `@Transactional` work under the hood?

<details><summary>Answer</summary>

At startup the auto-proxy creator (a BeanPostProcessor) wraps beans with `@Transactional` methods in a proxy. A call through the proxy goes to `TransactionInterceptor`: it asks the `PlatformTransactionManager` to begin (or join) a transaction according to propagation and isolation, binds the connection/EntityManager to the current thread (ThreadLocal), invokes the target method, then commits, or rolls back on a RuntimeException/Error (checked exceptions commit by default unless `rollbackFor`).

</details>

### SPRC-L3-002 · recall
JDK dynamic proxy vs CGLIB proxy?

<details><summary>Answer</summary>

JDK proxies implement the bean's interfaces; only interface methods are advised; the proxy cannot be cast to the class. CGLIB creates a runtime subclass of the class; works without interfaces (Boot's default), but cannot advise final classes or final, private or static methods, and calls the class constructor rules (objenesis avoids double construction).

</details>

### SPRC-L3-003 · bug
Why is no transaction started here, and give three fixes?

```java
@Service class OrderService {
    public void placeAll(List<Order> orders) { orders.forEach(this::place); }
    @Transactional public void place(Order o) { ... }
}
```

<details><summary>Answer</summary>

`this.place(o)` is a call on the target, not through the proxy, so the transaction interceptor never runs (self-invocation). Fixes: call `place` from another bean (move it to `OrderPlacer`), put `@Transactional` on the entry point if one transaction for all is intended, inject a lazy self-reference / `ObjectProvider<OrderService>` and call through it, or use `TransactionTemplate` programmatically inside the loop.

</details>

### SPRC-L3-004 · predict
`@Transactional` on a `private` method: what happens?

<details><summary>Answer</summary>

Nothing: proxies cannot intercept private methods (neither JDK nor CGLIB), so the annotation is silently ignored (some versions log nothing at all). The same for `final` methods with CGLIB. Annotate public methods called from other beans.

</details>

### SPRC-L3-005 · recall
BeanFactoryPostProcessor vs BeanPostProcessor: give one built-in example of each.

<details><summary>Answer</summary>

BeanFactoryPostProcessor runs before beans are created and modifies bean **definitions**: `PropertySourcesPlaceholderConfigurer` (resolves `${}`), `ConfigurationClassPostProcessor` (processes `@Configuration`/`@ComponentScan`/`@Import`). BeanPostProcessor runs on each bean **instance** and may replace it: `AutowiredAnnotationBeanPostProcessor` (injection), `CommonAnnotationBeanPostProcessor` (`@PostConstruct`), the AOP auto-proxy creator (proxies).

</details>

### SPRC-L3-006 · why
Why should `@Bean` methods that return post-processors be `static`?

<details><summary>Answer</summary>

Post-processors must be created before ordinary beans. A non-static `@Bean` method needs an instance of its configuration class first, forcing that configuration (and its dependencies) to be created too early, so those beans miss post-processing (no proxies, no `@Value` resolution) and Spring logs warnings. A static method needs no instance.

</details>

### SPRC-L3-007 · recall
Define aspect, advice, join point, pointcut and weaving.

<details><summary>Answer</summary>

Aspect: a module of cross-cutting behaviour. Advice: the code that runs (before, after returning, after throwing, after, around). Join point: a point in execution where advice can apply (in Spring AOP: a method execution on a proxied bean). Pointcut: an expression selecting join points. Weaving: linking aspects to targets (Spring: runtime proxies; AspectJ: compile/load-time bytecode weaving).

</details>

### SPRC-L3-008 · scenario
A method has both `@Cacheable` and `@Transactional`. Which runs first, and why does it matter?

<details><summary>Answer</summary>

Both are proxies/interceptors ordered by their advisor order: the smaller order value is the outer one. If the cache is outside, a cache hit returns before a transaction is opened (no connection borrowed) and evictions happen after commit. If the transaction is outside, every cache hit still opens a transaction. Set the order explicitly (`@EnableCaching(order = ...)`); with equal orders it depends on registration order.

</details>

### SPRC-L3-009 · predict
A `@Transactional` method catches an exception from the repository and returns normally. Is the transaction rolled back?

<details><summary>Answer</summary>

Not by the interceptor: it only sees a normal return, so it commits whatever was done. But if the exception came through another `@Transactional` (REQUIRED) proxy that already marked the shared transaction rollback-only, the commit fails with `UnexpectedRollbackException`. Do not swallow exceptions inside transactional methods; rethrow, or mark rollback explicitly.

</details>

### SPRC-L3-010 · recall
Which checked exceptions roll back a Spring transaction by default?

<details><summary>Answer</summary>

None. By default only unchecked exceptions (RuntimeException) and Errors roll back; checked exceptions commit. Use `@Transactional(rollbackFor = Exception.class)` or wrap checked exceptions in unchecked ones.

</details>

### SPRC-L3-011 · design
When is AOP the right tool, and when a bad idea?

<details><summary>Answer</summary>

Right: genuinely cross-cutting technical concerns applied uniformly: transactions, metrics/timing, tracing, security checks, auditing, retries. Bad: business logic (hidden behaviour no reader expects), anything that depends on call order between aspects without explicit ordering, or behaviour that must also work on self-calls. Prefer explicit code or a decorator when only a few places need it.

</details>

### SPRC-L3-012 · recall
How do you make a Spring Boot application start faster, and what does each option cost?

<details><summary>Answer</summary>

Lazy initialisation (faster start; errors appear on first use), narrower component scanning and fewer auto-configurations (effort), CDS/AppCDS class data sharing (a training run, small gain), Spring AOT + GraalVM native image (start in milliseconds, low memory; long builds, reflection/proxy hints, no runtime class generation), CRaC checkpoint/restore (platform support, state handling).

</details>

### SPRC-L3-013 · why
Why did Spring Boot start refusing circular references by default?

<details><summary>Answer</summary>

Field/setter injection cycles were resolved by injecting an early reference to a not-yet-initialised bean, which can be the raw target instead of its proxy (transactions silently missing) and couples beans tightly. Making cycles an error (`spring.main.allow-circular-references=false`, the default since Boot 2.6) forces a cleaner design.

</details>

### SPRC-L3-014 · scenario
`@Async` on a method does nothing; the code runs synchronously. List the possible reasons.

<details><summary>Answer</summary>

`@EnableAsync` is missing; the method is called from the same class (self-invocation); it is private or final; the bean is not a Spring bean (created with `new`); or it is called before the proxy exists (in a constructor or `@PostConstruct`). Also check the executor: with an unbounded default executor it is async but may not behave as expected.

</details>

### SPRC-L3-015 · recall
What is the "early reference" log message "Bean X is not eligible for getting processed by all BeanPostProcessors" telling you?

<details><summary>Answer</summary>

Bean X was created while post-processors were still being registered (typically because a post-processor depends on it), so some post-processors did not process it: it may lack proxies (no transactions, caching, async) or other processing. Fix: make post-processor `@Bean` methods static and keep their dependencies minimal or lazy.

</details>

### SPRC-L3-016 · dsa
How does a JDK dynamic proxy dispatch a method call, and why must you unwrap InvocationTargetException?

<details><summary>Answer</summary>

The generated proxy class implements the interfaces; each method calls `InvocationHandler.invoke(proxy, Method, args)`. Calling the target with `method.invoke(target, args)` (reflection) wraps any exception the target throws in `InvocationTargetException`. Rethrowing that would change the exception type callers see; rethrowing `getCause()` keeps the original. A checked exception not declared by the interface method would surface as `UndeclaredThrowableException`.

</details>

### SPRC-L3-017 · scenario
A bean is a `final class` with `@Transactional` methods and no interface. What happens at startup or at runtime?

<details><summary>Answer</summary>

CGLIB cannot subclass a final class, so creating the proxy fails at startup (an exception about not being able to generate a subclass of a final class). Kotlin classes are final by default, which is why the Kotlin Spring plugin opens them. Fix: remove `final`, or introduce an interface and use JDK proxies.

</details>

### SPRC-L3-018 · recall
What happens during `ApplicationContext.refresh()`, in order (main steps)?

<details><summary>Answer</summary>

Prepare the environment; load bean definitions; invoke BeanFactoryPostProcessors (configuration classes, scanning, placeholders); register BeanPostProcessors; initialise message source and event multicaster; `onRefresh` (Boot creates and starts the embedded web server here); register listeners; instantiate all non-lazy singletons (injection, init callbacks, proxies); finish refresh (lifecycle start, `ContextRefreshedEvent`; Boot then publishes `ApplicationReadyEvent`).

</details>

### SPRC-L3-019 · why
Why is calling `@Transactional` or `@Async` methods from `@PostConstruct` risky?

<details><summary>Answer</summary>

`@PostConstruct` runs on the target inside its own initialisation, before `postProcessAfterInitialization` has wrapped it in a proxy, so calls on `this` there have no advice (and other beans may not be fully ready). Run such work after startup: `ApplicationReadyEvent`, `ApplicationRunner` or `SmartLifecycle`.

</details>

### SPRC-L3-020 · interview
Explain how you would build a small `@Retryable`-like feature yourself in Spring.

<details><summary>Answer</summary>

Define an annotation `@Retry(attempts = 3)`. Write an `@Aspect` with `@Around("@annotation(retry)")` that calls `proceed()` in a loop, catching retryable exceptions, sleeping with backoff, rethrowing the last failure; enable with `@EnableAspectJAutoProxy` (Boot does). Or a BeanPostProcessor that wraps annotated beans in proxies. Mind self-invocation, idempotency of the retried operation, interaction with `@Transactional` order (retry outside the transaction, so each attempt gets a fresh one), and metrics.

</details>
