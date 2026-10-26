# Spring core, L2: scopes, lifecycle, configuration, profiles, events

**Goal:** control how many instances exist and when they start and stop, configure the same artifact for every environment, and decouple modules with events.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="spring & L2"`. Drill: `java quiz/Quiz.java --track spring-core --level 2`.

---

## 1. Scopes

| Scope | Instances | Use |
|---|---|---|
| `singleton` (default) | one per container | stateless services, repositories, clients: almost everything |
| `prototype` | a new one every time it is requested from the container | stateful helpers used once (a report builder) |
| `request` / `session` (web) | one per HTTP request / HTTP session | per-request context (rarely needed: prefer method parameters) |
| `application`, `websocket`, custom | ... | rare |

**Singletons must be thread-safe**: every request thread uses the same instance concurrently. No per-request state in fields (concurrency track L1).

**The classic trap: a prototype injected into a singleton.** Injection happens once, when the singleton is created, so the singleton keeps **one** prototype instance forever, shared by every call and thread. Ask the container for a new instance each time instead:

```java
private final ObjectProvider<ReportBuilder> builders;     // or jakarta.inject.Provider<ReportBuilder>
ReportBuilder builder = builders.getObject();             // a NEW prototype instance
```

(Alternatives: `@Lookup` methods, or a scoped proxy `@Scope(value = "prototype", proxyMode = TARGET_CLASS)`, which creates a new target per method call: rarely what you want.) The same problem appears for request-scoped beans in singletons, solved by scoped proxies.

> **Kata `ReportService`**: the bug, and the fix.

---

## 2. Bean lifecycle

```
instantiate (constructor) -> inject dependencies -> Aware callbacks (BeanNameAware, ApplicationContextAware)
-> BeanPostProcessor.postProcessBeforeInitialization
-> @PostConstruct  /  InitializingBean.afterPropertiesSet  /  @Bean(initMethod)
-> BeanPostProcessor.postProcessAfterInitialization        (proxies are created here: L3)
-> bean in use
-> on context close: @PreDestroy / DisposableBean.destroy / @Bean(destroyMethod)   (singletons only!)
```

- Do work that needs the fully configured bean (open a pool, warm a cache, validate configuration) in `@PostConstruct`, not in the constructor.
- Release resources in `@PreDestroy`. For `@Bean` methods, Spring **infers** a public `close()` or `shutdown()` method as the destroy method.
- Spring **does not destroy prototypes**: whoever requested one owns it.
- For "start after everything is ready, stop in order" (message listeners, schedulers), implement `SmartLifecycle` (phases), or react to `ApplicationReadyEvent`.
- Spring Boot's graceful shutdown happens before destruction: in-flight requests finish, then beans are destroyed.

> **Kata `ConnectionPool`**: `@PostConstruct` and `@PreDestroy`, and why prototypes are never closed for you.

---

## 3. Externalised configuration

The same artifact runs on a laptop, in CI and in production; only configuration differs. Spring's `Environment` merges **property sources** in a priority order (Boot: command-line args > environment variables > `application-{profile}.properties` > `application.properties` > defaults; and more, e.g. config trees for Kubernetes secrets).

```java
@Value("${payment.timeout-ms:2000}") int timeout;        // with a default
@Value("${payment.api-key}") String apiKey;               // required: startup FAILS if missing
```

- **Relaxed binding** (Boot): `payment.api-key` can be set by the environment variable `PAYMENT_APIKEY` (dots become underscores, dashes are removed, upper case). Environment variables are how containers configure apps.
- `@ConfigurationProperties(prefix = "payment")` binds a whole group into a typed, validatable object (record), better than many `@Value`s (track 07).
- **Plain Spring resolves `${...}` leniently** and injects the literal text `"${payment.api-key}"` when the property is missing, unless a `PropertySourcesPlaceholderConfigurer` bean exists. Spring Boot registers one, so in Boot a missing required property fails the start. (The kata's test registers it explicitly.)
- **Secrets** come from the environment or a secret manager at runtime, never from files committed to git.

---

## 4. Profiles

A **profile** is a named switch that includes or excludes bean definitions and property files:

```java
@Bean @Profile("prod")  PaymentGateway stripe(...)
@Bean @Profile("!prod") PaymentGateway fake()
```

Activate with `spring.profiles.active=prod` (property, env var `SPRING_PROFILES_ACTIVE`, or `-Dspring.profiles.active`). `application-prod.properties` is loaded when `prod` is active.

Guidelines:
- Profiles for **environment differences in wiring** (fake vs real gateway, local vs cloud storage); properties for **values**.
- Few profiles (`local`, `test`, `prod`), not one per server.
- Prefer fail-fast: production without its required configuration must not start.

> **Kata `PaymentConfig`**: fake everywhere except `prod`, a required key, a defaulted timeout, exactly one bean.

---

## 5. Application events

Decouple a module from the things that react to it:

```java
public record OrderPlaced(long orderId, long customerId) {}

@Service class OrderService {
    private final ApplicationEventPublisher events;
    void place(...) { ...; events.publishEvent(new OrderPlaced(id, customerId)); }
}

@Component class LoyaltyPoints {
    @EventListener void on(OrderPlaced e) { ... }               // synchronous, same thread, same transaction
}
```

- Listeners run **synchronously** in the publisher's thread by default: an exception in a listener propagates to the publisher. `@Async` makes them asynchronous (and loses the transaction).
- `@TransactionalEventListener(phase = AFTER_COMMIT)` runs only if the transaction commits: the right place to trigger "send email" (but it runs in memory: if the process dies after commit, the event is lost; the outbox pattern fixes that, shop Level 5).
- `@Order` orders listeners; built-in events: `ContextRefreshedEvent`, `ApplicationReadyEvent`, `ContextClosedEvent`.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `ReportService` (bug) | prototype vs singleton, `ObjectProvider` |
| `ConnectionPool` | `@PostConstruct`, `@PreDestroy`, prototype destruction |
| `PaymentConfig` | `@Profile`, `@Value` with and without defaults, fail-fast configuration |

```
cd katas && ./mvnw test -Dgroups="spring & L2"
```

## 7. Seen in the shop project

- Typed configuration: `RateLimitProperties`, `OutboxProperties`, `JwtProperties` (`@ConfigurationProperties` records).
- A `prod` profile file (`application-prod.properties`) that, among other things, turns on trusting forwarded headers from proxies: the same jar everywhere.
- Spring Modulith events between modules (`OrderEvent`, `OrderPlacedNotifier`) and the outbox for durability.

## 8. Interview checklist

1. Name the bean scopes. Which is the default, and what must be true of a singleton's code?
2. What happens when a prototype bean is injected into a singleton? How do you fix it?
3. Walk through the lifecycle of a singleton bean, from constructor to destruction.
4. `@PostConstruct` vs the constructor: what is the difference?
5. Are prototype beans destroyed by Spring?
6. In which order does Spring Boot read configuration sources?
7. `@Value` vs `@ConfigurationProperties`.
8. What does a profile do? When would you use a profile, and when just a property?
9. Are Spring events synchronous? What does `@TransactionalEventListener` add, and what does it not guarantee?
