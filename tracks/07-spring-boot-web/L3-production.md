# Spring Boot web, L3: auto-configuration, configuration, actuator and running in production

**Goal:** know how Boot decides what to configure (and how to write your own starter), make configuration typed and fail-fast, expose the right operational endpoints safely, and run a Boot service well: threads, timeouts, graceful shutdown, observability.

Prerequisite: L1, L2, Spring core L2-L3. Katas: `cd katas && ./mvnw test -Dgroups="boot & L3"`. Drill: `java quiz/Quiz.java --track spring-boot-web --level 3`.

---

## 1. Auto-configuration, from the inside

An auto-configuration is a `@Configuration` class in a jar, registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, guarded by **conditions**:

```java
@AutoConfiguration
@ConditionalOnClass(Greeter.class)                                   // only if the library is on the classpath
@ConditionalOnProperty(prefix = "dojo.greeter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreeterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean                                        // back off if the application defines its own
    Greeter greeter(@Value("${dojo.greeter.prefix:Hello}") String prefix) { ... }
}
```

- Auto-configurations run **after** your configuration, so `@ConditionalOnMissingBean` can see your beans: "user beans win" is the core contract.
- Order between auto-configurations: `@AutoConfiguration(after = ..., before = ...)`.
- Diagnose with `--debug` (conditions report) or the `conditions` actuator endpoint; exclude with `spring.autoconfigure.exclude=...`.
- A **starter** = an auto-configure module + a starter POM that depends on it and the library.
- Spring Boot 4 split auto-configuration into many small modules (one per technology); the idea is unchanged.

> **Kata `GreeterAutoConfiguration`**: conditions, back-off, a property switch, and the imports file; tested with `ApplicationContextRunner`.

---

## 2. Typed, validated configuration

```java
@ConfigurationProperties(prefix = "shipping")
@Validated
public record ShippingProperties(
        @NotBlank @Pattern(regexp = "https://.+") String carrierUrl,
        @DefaultValue("2s") Duration timeout,
        @DefaultValue("10") @Min(1) @Max(50) int maxParcels) {}

@Configuration
@EnableConfigurationProperties(ShippingProperties.class)          // or @ConfigurationPropertiesScan
class ShippingConfig {}
```

- One typed object per concern, injected where needed; `Duration` and `DataSize` parse `500ms`, `10MB`.
- `@Validated`: invalid or missing values **fail the startup** with a clear message, not the first request.
- `spring-boot-configuration-processor` generates metadata so IDEs autocomplete your properties.
- Secrets: bind them like any property, supply them from the environment or a secret store, and never log the properties object as a whole.

> **Kata `ShippingProperties`**: constructor binding, defaults, validation that stops the start.

---

## 3. Actuator: health, metrics, info

`spring-boot-starter-actuator` adds operational endpoints under `/actuator`:
- `health` (with groups: **`liveness`** and **`readiness`** for Kubernetes probes), `info`, `metrics` and `prometheus`, `loggers` (change log levels at runtime), `threaddump`, `heapdump`, `env`, `configprops`, `mappings`, `conditions`.
- **Expose only what you need over HTTP** (`management.endpoints.web.exposure.include=health,info,prometheus`), secure the rest, or serve actuator on a separate internal port (`management.server.port`). `env`, `heapdump` and `configprops` can leak secrets.
- `management.endpoint.health.show-details=when-authorized`: details (which database is down) only for operators.

**Custom health indicators** contribute to `/actuator/health`. The important decision is **which group**:
- **Liveness** = "is this process broken beyond repair?" (deadlocked, out of memory). Failing it **restarts** the pod. It should include almost nothing external: if the database is down, restarting every pod does not help and makes it worse.
- **Readiness** = "can this instance serve traffic right now?" Failing it removes the pod from the load balancer until it recovers. Dependencies that are *required* for every request (the database) can be here; optional ones (a recommendation service) should not, or one flaky dependency takes the whole service out of rotation.

> **Kata `PaymentProviderHealth`**: UP/DOWN with details, no internals, and the readiness question.

---

## 4. Running Boot in production

- **Threads:** Tomcat's pool (`server.tomcat.threads.max`, default 200) bounds concurrency; combined with the Hikari pool size, it decides throughput and latency under load. Or `spring.threads.virtual.enabled=true` (Java 21+) for virtual threads, with explicit limits on downstream pools (concurrency track L3).
- **Timeouts everywhere:** HTTP clients (`RestClient` request factory connect/read timeouts), database pool `connection-timeout`, `spring.mvc.async.request-timeout`, transaction timeouts.
- **Graceful shutdown:** `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=20s`: on SIGTERM, readiness goes DOWN, Tomcat stops accepting new requests, in-flight requests finish, then beans are destroyed. Kubernetes' `terminationGracePeriodSeconds` must be longer, and a `preStop` sleep lets the load balancer stop routing first.
- **Observability:** Micrometer metrics (HTTP server requests by URI template and status, JVM, pools, caches), tracing (OpenTelemetry), structured JSON logs (`logging.structured.format.console=ecs` in Boot 3.4+), trace ids in logs and responses.
- **Startup:** fail fast on misconfiguration; consider lazy init, AOT and CDS only with measured need (Spring core L3).
- **Security headers and CORS** at the edge or in Spring Security; no stack traces in error responses (`server.error.include-stacktrace=never`, the default).
- **One artifact, many environments:** configuration by environment variables and profiles; the same image in staging and production.

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `ShippingProperties` | `@ConfigurationProperties` records, `@DefaultValue`, `@Validated`, fail-fast |
| `GreeterAutoConfiguration` | `@AutoConfiguration`, conditions, back-off, the imports file, `ApplicationContextRunner` |
| `PaymentProviderHealth` | `HealthIndicator`, details, liveness vs readiness |

```
cd katas && ./mvnw test -Dgroups="boot & L3"
```

## 6. Seen in the shop project

- `@ConfigurationProperties` records with validation (`RateLimitProperties`, `OutboxProperties`, `JwtProperties`).
- Actuator with liveness/readiness groups, Prometheus metrics, and actuator not reachable from the public frontend (Level 6, frontend level).
- `server.shutdown=graceful`, Kubernetes probes and `preStop` in `deploy/k8s/` (Level 6).

## 7. Interview talk track

"Spring Boot is Spring plus starters and conditional auto-configuration: configuration classes in jars, registered in the AutoConfiguration.imports file and guarded by conditions like OnClass, OnProperty and OnMissingBean, so my own beans always win; the conditions report explains any surprise. I keep configuration in validated @ConfigurationProperties records so a bad value stops the start. Controllers are thin, validate input with Bean Validation, and errors go through one RestControllerAdvice that returns problem details. Cross-cutting HTTP concerns like request ids are filters, with the MDC cleaned up in finally. Operationally: Actuator exposes health, info and Prometheus only, liveness checks just the process, readiness the essential dependencies; graceful shutdown drains requests within the pod's grace period; every outbound call has timeouts; and metrics are tagged by URI template, logs are structured with trace ids."

**Questions you should be able to answer:**
1. How does auto-configuration work? How is an auto-configuration registered in Boot 3/4?
2. What does `@ConditionalOnMissingBean` guarantee, and why do auto-configurations run after user configuration?
3. How do you find out why an auto-configuration did or did not apply?
4. How would you write your own starter?
5. Why `@ConfigurationProperties` + `@Validated` instead of `@Value`?
6. Which actuator endpoints are dangerous to expose, and how do you protect them?
7. Liveness vs readiness: what belongs in each, and what goes wrong if the database is in liveness?
8. Walk through graceful shutdown of a Boot app in Kubernetes.
9. How do Tomcat's thread pool and Hikari's pool interact under load? What changes with virtual threads?
10. What do you configure to make a Boot service observable?
