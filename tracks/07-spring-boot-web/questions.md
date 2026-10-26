# Spring Boot web: question bank

Drill with `java quiz/Quiz.java --track spring-boot-web`.

## L1

### BOOT-L1-001 · recall
What does Spring Boot add on top of the Spring Framework?

<details><summary>Answer</summary>

Starters (curated dependency sets with managed versions), conditional auto-configuration (sensible beans created when libraries and properties are present, backing off for your own), an embedded server (run as `java -jar`), externalised configuration with profiles and relaxed binding, and Actuator for production operations.

</details>

### BOOT-L1-002 · recall
What does `@SpringBootApplication` combine?

<details><summary>Answer</summary>

`@SpringBootConfiguration` (a `@Configuration`), `@EnableAutoConfiguration` (apply auto-configurations from the classpath) and `@ComponentScan` (the annotated class's package and sub-packages).

</details>

### BOOT-L1-003 · recall
What happens when `SpringApplication.run` is called?

<details><summary>Answer</summary>

It determines the application type (servlet, reactive, none), prepares the Environment (property sources, profiles), creates the ApplicationContext, loads your configuration and auto-configurations, refreshes the context (beans created, post-processors, proxies), starts the embedded web server, runs `ApplicationRunner`/`CommandLineRunner` beans, and publishes `ApplicationReadyEvent`.

</details>

### BOOT-L1-004 · recall
Trace an HTTP request through Spring MVC to a controller method and back.

<details><summary>Answer</summary>

Tomcat accepts the connection and assigns a worker thread → servlet filters (security, logging, CORS) → `DispatcherServlet` → `HandlerMapping` finds the `@RequestMapping` method (404/405 otherwise) → interceptors' `preHandle` → argument resolvers bind path variables, parameters, headers and the body (Jackson), plus validation → the method runs → the return value is written by an `HttpMessageConverter` (JSON) with status/headers from `ResponseEntity` → exceptions go to `@ExceptionHandler`/advice → interceptors' `afterCompletion` → filters → response.

</details>

### BOOT-L1-005 · recall
`@Controller` vs `@RestController`?

<details><summary>Answer</summary>

`@RestController` = `@Controller` + `@ResponseBody` on every method: return values are written to the response body (JSON) instead of being interpreted as view names (templates).

</details>

### BOOT-L1-006 · recall
How do you return 201 Created with a Location header, 204 No Content and 404 Not Found from a controller?

<details><summary>Answer</summary>

`ResponseEntity.created(URI.create("/api/books/" + id)).body(book)`; `ResponseEntity.noContent().build()`; `ResponseEntity.notFound().build()` (or `ResponseEntity.of(optional)`, which returns 404 for an empty Optional), or throw an exception mapped to 404 by an advice / `ResponseStatusException`.

</details>

### BOOT-L1-007 · predict
A controller method has `@PathVariable long id`, and the client calls `/api/books/abc`. What is the response?

<details><summary>Answer</summary>

400 Bad Request: the conversion of "abc" to long fails with a `MethodArgumentTypeMismatchException`, which Spring MVC maps to 400. Not a 500, and the method never runs.

</details>

### BOOT-L1-008 · scenario
After moving a project off the Spring Boot parent POM, every request fails with "Name for argument of type [long] not specified, and parameter name information not available via reflection". Why?

<details><summary>Answer</summary>

The Boot parent compiles with `javac -parameters`, which keeps parameter names in the bytecode; Spring MVC uses them to bind `@PathVariable long id` and `@RequestParam String name`. Without the flag, set `<maven.compiler.parameters>true</maven.compiler.parameters>` or name every binding explicitly: `@PathVariable("id")`.

</details>

### BOOT-L1-009 · why
Why should controllers accept and return DTOs (records) instead of JPA entities?

<details><summary>Answer</summary>

The API contract stays independent of the database schema; you control exactly which fields are exposed (no password hashes or internal flags) and which may be set by clients (no mass assignment of `role` or `id`); serialising entities triggers lazy loading (N+1, LazyInitializationException) and can loop on bidirectional relations.

</details>

### BOOT-L1-010 · recall
`@RequestParam` vs `@PathVariable` vs `@RequestBody`?

<details><summary>Answer</summary>

`@PathVariable`: a segment of the URL path (`/books/{id}`), identifying a resource. `@RequestParam`: a query (or form) parameter (`?size=20`), for filters, options, pagination; required unless `required=false` or a default is set. `@RequestBody`: the request body, deserialised from JSON by Jackson into an object.

</details>

### BOOT-L1-011 · recall
What is a Spring Boot starter? Give two examples and what they bring.

<details><summary>Answer</summary>

A dependency (usually without code) that pulls in everything needed for a capability, with versions managed by Boot. `spring-boot-starter-webmvc`: Spring MVC, embedded Tomcat, Jackson. `spring-boot-starter-data-jpa`: Spring Data JPA, Hibernate, HikariCP, transactions. `spring-boot-starter-test`: JUnit, AssertJ, Mockito, Spring test support.

</details>

### BOOT-L1-012 · recall
`@WebMvcTest` vs `@SpringBootTest`?

<details><summary>Answer</summary>

`@WebMvcTest(Controller.class)` loads only the MVC slice (the controller, advice, filters, converters, MockMvc), no services, repositories or database: fast and focused on HTTP behaviour; dependencies are imported or mocked. `@SpringBootTest` loads the full application context (optionally with a real server via `webEnvironment = RANDOM_PORT`): for end-to-end/integration tests.

</details>

### BOOT-L1-013 · why
Why must a controller be stateless?

<details><summary>Answer</summary>

It is a singleton bean called concurrently by many Tomcat threads. Any mutable field is shared between requests: race conditions and data leaking between users. Keep state in method locals, the request, or a database.

</details>

### BOOT-L1-014 · predict
A required `@RequestParam String to` is missing from the request. What happens?

<details><summary>Answer</summary>

400 Bad Request (`MissingServletRequestParameterException`), before the method runs. Make it optional with `required = false` (then `null`) or `defaultValue = "EUR"`, or use `Optional<String>`.

</details>

### BOOT-L1-015 · recall
How does Spring Boot turn a returned record into JSON?

<details><summary>Answer</summary>

The `@ResponseBody` return value is handled by `HttpMessageConverter`s; for `application/json` it is Jackson's converter, using the auto-configured JSON mapper (Jackson 3 `JsonMapper` in Boot 4, `ObjectMapper` in Boot 3). Records are serialised by component names. The `Accept` header and `produces` decide the content type.

</details>

### BOOT-L1-016 · scenario
A request to `POST /api/books` with `Content-Type: text/plain` and a JSON body fails. Which status and why?

<details><summary>Answer</summary>

415 Unsupported Media Type: the `@RequestBody` needs a converter that can read `text/plain` into the record, and Jackson only reads JSON media types. The client must send `Content-Type: application/json`.

</details>

### BOOT-L1-017 · recall
What does `server.port=0` do, and when is it useful?

<details><summary>Answer</summary>

Picks a random free port at startup. Useful for running several instances locally or for integration tests (`@SpringBootTest(webEnvironment = RANDOM_PORT)` with `@LocalServerPort` to find it).

</details>

### BOOT-L1-018 · recall
How do you see which auto-configurations were applied and why?

<details><summary>Answer</summary>

Start with `--debug` (or `debug=true`): the conditions evaluation report lists positive matches, negative matches with the failed condition, exclusions and unconditional classes. At runtime: the actuator `conditions` endpoint.

</details>

### BOOT-L1-019 · design
Design the controller signature for "list products, filtered by category, cursor-paginated, max 100 per page".

<details><summary>Answer</summary>

`@GetMapping("/api/v1/products") CursorPage<ProductResponse> list(@RequestParam Optional<String> category, @RequestParam Optional<String> cursor, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size)`, delegating to a service; the response record holds `items` and `nextCursor`; an invalid cursor maps to 400.

</details>

### BOOT-L1-020 · recall
What is the difference between an embedded server and deploying a war?

<details><summary>Answer</summary>

Embedded: the application contains and starts the server (Tomcat/Jetty) itself; one executable jar, one process per app, configured like any other part of the app, ideal for containers. War: the application is deployed into a separately installed and configured application server, possibly with several apps sharing it; the server's version and configuration are managed outside the app.

</details>

## L2

### BOOT-L2-001 · recall
How does Bean Validation work in a controller, and what happens on failure?

<details><summary>Answer</summary>

Constraints (`@NotBlank`, `@Email`, `@Size`...) are declared on the DTO; `@Valid` on the `@RequestBody` parameter makes Spring validate it (Hibernate Validator) after deserialisation and before the method is called. Failures throw `MethodArgumentNotValidException`, by default a 400; an advice can turn it into a problem detail with field errors.

</details>

### BOOT-L2-002 · recall
`@NotNull` vs `@NotEmpty` vs `@NotBlank`?

<details><summary>Answer</summary>

`@NotNull`: not null (any type). `@NotEmpty`: not null and not empty (strings, collections, maps, arrays). `@NotBlank`: strings only; not null and at least one non-whitespace character. For a required name, `@NotBlank`; for a required list, `@NotEmpty`.

</details>

### BOOT-L2-003 · bug
A request record has `@Min(1) int quantity`. A client omits `quantity`. What happens, and how do you require the field?

<details><summary>Answer</summary>

Jackson leaves the primitive at its default 0, and `@Min(1)` fails with "must be greater than or equal to 1": misleading, and if the constraint were `@Min(0)`, the missing field would be silently accepted as 0. Use `@NotNull @Min(1) Integer quantity` to distinguish "missing" from a value.

</details>

### BOOT-L2-004 · recall
How do you validate nested objects and the elements of a list?

<details><summary>Answer</summary>

Cascade with `@Valid`: `@Valid @NotNull Address address` and `@NotEmpty List<@Valid Item> items` (container element constraints). Without `@Valid` on the nested type, its constraints are not checked.

</details>

### BOOT-L2-005 · recall
`@ExceptionHandler` in a controller vs in a `@RestControllerAdvice`?

<details><summary>Answer</summary>

In a controller it handles exceptions from that controller only. In a `@RestControllerAdvice` it applies to all controllers (or a selected subset via `basePackages`/`assignableTypes`), giving one consistent error format for the whole API.

</details>

### BOOT-L2-006 · recall
What does `ResponseEntityExceptionHandler` give you?

<details><summary>Answer</summary>

A base class for an advice that already handles Spring MVC's standard exceptions (missing parameters, type mismatches, unreadable bodies, validation, 404 no handler, 405, 406, 415...) and turns them into `ProblemDetail` responses with the right status. You override specific methods (e.g. validation) and add handlers for your domain exceptions.

</details>

### BOOT-L2-007 · bug
What is wrong with this handler?

```java
@ExceptionHandler(Exception.class)
ResponseEntity<String> any(Exception e) { return ResponseEntity.status(500).body(e.getMessage()); }
```

<details><summary>Answer</summary>

It leaks internal details (SQL fragments, file paths, other users' data, library messages) to clients, it is not a structured problem detail, and it does not log the stack trace. It may also swallow client errors as 500s. Log the exception with a trace id, return a generic problem detail ("Internal error, reference X") with 500, and map known exceptions to specific statuses before this catch-all.

</details>

### BOOT-L2-008 · recall
Filter vs HandlerInterceptor vs aspect: what does each see, and where does each run?

<details><summary>Answer</summary>

A servlet Filter wraps every request reaching the container (before DispatcherServlet), seeing the raw request/response, including static resources and requests rejected by security. A HandlerInterceptor runs inside Spring MVC around handler execution and knows which controller method handles the request. An aspect wraps calls to bean methods anywhere (services, repositories), via proxies.

</details>

### BOOT-L2-009 · design
Where would you implement: request ids, authentication, per-endpoint feature flags, transactions?

<details><summary>Answer</summary>

Request ids: a filter (first in the chain). Authentication: a filter (Spring Security's filter chain). Per-endpoint feature flags: an interceptor (it knows the handler and its annotations) or a check in the controller. Transactions: an aspect on service methods (`@Transactional`), not in the web layer.

</details>

### BOOT-L2-010 · bug
A filter puts the user id into the MDC but never removes it. What will you see in the logs?

<details><summary>Answer</summary>

Tomcat reuses threads: log lines of later requests on the same thread (anonymous requests, or other users') carry the previous user's id, making logs misleading, and the value is never released. Always `MDC.remove(...)` (or `MDC.clear()`) in a `finally` block.

</details>

### BOOT-L2-011 · why
Why validate an incoming `X-Request-Id` header instead of trusting it?

<details><summary>Answer</summary>

It is user input: a value with CR/LF can inject fake log lines or headers (log/response splitting), a huge value bloats logs, special characters break log parsing or dashboards. Accept only a safe pattern and length, or generate your own id (keeping the client's as a separate, sanitised field).

</details>

### BOOT-L2-012 · recall
What does `OncePerRequestFilter` guarantee?

<details><summary>Answer</summary>

That `doFilterInternal` runs exactly once per request, even when the request is dispatched internally several times (forwards, includes, error dispatches, async dispatches), which a plain Filter would see repeatedly.

</details>

### BOOT-L2-013 · predict
A filter sets a response header after calling `chain.doFilter(...)`, when the controller has already written a large body. Does the client get the header?

<details><summary>Answer</summary>

Often not: once the response is committed (the buffer was flushed with the status and headers), headers can no longer be changed and the call is silently ignored. Set headers before the chain, or wrap the response.

</details>

### BOOT-L2-014 · recall
Which JSON library does Spring Boot 4 use, and what changed from Boot 3?

<details><summary>Answer</summary>

Jackson 3 (packages `tools.jackson.*`, `JsonMapper` as the main entry point), while Boot 3 used Jackson 2 (`com.fasterxml.jackson.*`, `ObjectMapper`). Annotations stayed in `com.fasterxml.jackson.annotation`. Code that customised `ObjectMapper` beans or used Jackson 2 classes must be migrated.

</details>

### BOOT-L2-015 · recall
Does Spring Boot fail on unknown JSON properties by default? Why does that matter?

<details><summary>Answer</summary>

No: Boot configures Jackson to ignore unknown properties when deserialising. That lets clients send fields your version does not know (and servers add response fields clients do not know), which is essential for evolving APIs without lockstep releases.

</details>

### BOOT-L2-016 · scenario
A frontend on another origin calls your API and the browser reports a CORS error, but curl works. What do you configure?

<details><summary>Answer</summary>

Allow that origin for the API paths: `@CrossOrigin` or a global `CorsConfigurationSource` (with Spring Security: `http.cors(...)` so preflight OPTIONS requests are handled before authentication), listing allowed origins, methods, headers (`Authorization`, `Content-Type`, custom ones), exposed headers (`Location`, `ETag`), and max age. curl ignores CORS, which is why it works.

</details>

### BOOT-L2-017 · recall
How do you validate a `@RequestParam` or `@PathVariable` (e.g. size between 1 and 100)?

<details><summary>Answer</summary>

Put constraints on the parameter: `@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size`. Spring Framework 6.1+ applies method validation to controller parameters with constraints (earlier: annotate the controller with `@Validated`). Failures raise `HandlerMethodValidationException` → 400.

</details>

### BOOT-L2-018 · design
How should an API report a business rule violation like "insufficient stock"?

<details><summary>Answer</summary>

The service throws a domain exception (`InsufficientStockException` with product id and available quantity); the advice maps it to 409 Conflict (or 422) with a problem detail: a stable `type`, a human `detail`, and machine-readable properties (`productId`, `available`), plus the trace id. The controller contains no error-mapping logic.

</details>

### BOOT-L2-019 · recall
`RestClient` vs `RestTemplate` vs `WebClient`?

<details><summary>Answer</summary>

`RestTemplate`: the old synchronous client, in maintenance. `RestClient` (Spring 6.1+): the modern synchronous, fluent client, the default choice for blocking (and virtual-thread) code. `WebClient`: non-blocking/reactive (WebFlux), for reactive applications or massive concurrency. All need explicit timeouts on the underlying request factory or connector.

</details>

### BOOT-L2-020 · scenario
A JPA entity with a bidirectional `@OneToMany` is returned from a controller and the request fails with a `StackOverflowError` or a huge response. Why, and what is the fix?

<details><summary>Answer</summary>

Jackson serialises `order.items[].order.items[]...` endlessly (or triggers lazy loading of whole object graphs). Fix: map to response DTOs that contain exactly what the client needs; annotations like `@JsonIgnore` or `@JsonManagedReference` only patch the symptom.

</details>

## L3

### BOOT-L3-001 · recall
How does auto-configuration work, and how is it registered in Spring Boot 3/4?

<details><summary>Answer</summary>

Auto-configurations are `@AutoConfiguration` classes in jars, listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (replacing `spring.factories` from Boot 2). `@EnableAutoConfiguration` loads them after user configuration; each is guarded by conditions (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`, ...) so it applies only when appropriate and backs off when the user defines their own beans.

</details>

### BOOT-L3-002 · why
Why are auto-configurations processed after the application's own configuration?

<details><summary>Answer</summary>

So conditions like `@ConditionalOnMissingBean` can see the user's beans: if you define a `DataSource` or `JsonMapper`, Boot's default is skipped. "User configuration wins" is the core contract of Boot; it only fills gaps.

</details>

### BOOT-L3-003 · design
How would you write your own Spring Boot starter for an internal library?

<details><summary>Answer</summary>

An autoconfigure module with `@AutoConfiguration` classes (beans guarded by `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`), typed `@ConfigurationProperties` with metadata, registered in `AutoConfiguration.imports`, tested with `ApplicationContextRunner`; plus a starter POM that depends on it and the library. Use your own property prefix and do not component-scan from the library.

</details>

### BOOT-L3-004 · recall
What is `ApplicationContextRunner`, and why use it to test auto-configurations?

<details><summary>Answer</summary>

A Boot test utility that builds a small application context with given configurations and properties for one assertion, then closes it. It makes testing conditions cheap: run with and without a property, with and without a user bean, and assert `hasSingleBean`, `doesNotHaveBean`, `hasFailed` in milliseconds.

</details>

### BOOT-L3-005 · why
Why prefer `@ConfigurationProperties` records with `@Validated` over `@Value`?

<details><summary>Answer</summary>

Typed and grouped (one object per concern), immutable (records), conversion of `Duration`/`DataSize`, relaxed binding, IDE metadata, defaults in one place, and validation that fails the **startup** with a clear message instead of a runtime error later. `@Value` strings scatter configuration across the codebase.

</details>

### BOOT-L3-006 · recall
Which Actuator endpoints are dangerous to expose publicly, and how do you protect them?

<details><summary>Answer</summary>

`env` and `configprops` (configuration values, possibly secrets despite sanitising), `heapdump` (all memory, including secrets and user data), `threaddump`, `loggers` (changing levels), `mappings`, `beans`, `shutdown`. Expose only `health`, `info`, `prometheus` over HTTP; secure the rest with Spring Security; or serve actuator on a separate internal port not routed by the ingress.

</details>

### BOOT-L3-007 · interview
Liveness vs readiness: what goes into each, and what happens if the database health is part of liveness?

<details><summary>Answer</summary>

Liveness: is the process healthy (not deadlocked, not broken)? Failing it makes Kubernetes restart the container. Readiness: can it serve traffic now (started, required dependencies available, not shutting down)? Failing it removes the pod from the Service's endpoints. If the database is in liveness, a database outage makes every pod fail liveness and get restarted in a loop: more load and no recovery, while readiness would simply have stopped traffic until the database returned.

</details>

### BOOT-L3-008 · recall
Walk through graceful shutdown of a Spring Boot app in Kubernetes.

<details><summary>Answer</summary>

Kubernetes marks the pod terminating and removes it from endpoints (asynchronously), runs the `preStop` hook (e.g. sleep a few seconds so load balancers stop routing), then sends SIGTERM. Boot (`server.shutdown=graceful`) sets readiness to refusing traffic, stops accepting new requests, waits for in-flight requests up to `spring.lifecycle.timeout-per-shutdown-phase`, then closes the context (destroying beans: pools, consumers). `terminationGracePeriodSeconds` must exceed preStop + drain time, or SIGKILL cuts requests.

</details>

### BOOT-L3-009 · scenario
Under load, response times jump to seconds while CPU is low. Tomcat has 200 threads, Hikari has 10 connections. What is happening?

<details><summary>Answer</summary>

Up to 200 requests run concurrently, but only 10 can hold a database connection; the rest wait in Hikari's queue (up to `connection-timeout`), so latency is dominated by waiting for connections (visible in `hikaricp_connections_pending` and thread dumps in `getConnection`). Fix: shorter transactions and faster queries, a correctly sized pool (often small is right), limiting concurrency or failing fast, caching; do not just raise both numbers.

</details>

### BOOT-L3-010 · recall
What does `spring.threads.virtual.enabled=true` change, and what must you watch?

<details><summary>Answer</summary>

Tomcat (and Boot's task executors and schedulers) run work on virtual threads: blocking I/O no longer pins scarce platform threads, so concurrency is no longer capped by the thread pool size. Watch: downstream limits (DB pool, HTTP client pools) now receive much more concurrency; pinning in `synchronized` blocks on Java 21-23; ThreadLocal-heavy libraries.

</details>

### BOOT-L3-011 · recall
How do you make a Spring Boot service observable?

<details><summary>Answer</summary>

Actuator + Micrometer metrics exported to Prometheus (HTTP requests by URI template/status/latency histograms, JVM, pools, caches, business counters), distributed tracing (Micrometer Tracing / OpenTelemetry) with trace ids propagated and added to logs and error responses, structured JSON logs, health groups for probes, and dashboards plus SLO-based alerts on top.

</details>

### BOOT-L3-012 · recall
How do you exclude an auto-configuration, and why might you?

<details><summary>Answer</summary>

`@SpringBootApplication(exclude = X.class)` or `spring.autoconfigure.exclude=...`. Reasons: a library on the classpath triggers configuration you do not want (e.g. a `DataSource` in a service that does not use a database), or you replace the feature entirely. Usually defining your own bean is enough because of back-off.

</details>

### BOOT-L3-013 · why
Why is failing at startup better than failing at the first request?

<details><summary>Answer</summary>

A pod that fails to start never becomes ready, so the rolling deploy stops and the old version keeps serving: the problem is caught by the deploy pipeline. A pod that starts with bad configuration receives traffic and fails customers' requests (often only some code paths), and the problem is found by users or at 3 a.m.

</details>

### BOOT-L3-014 · recall
What does `management.endpoint.health.show-details` control, and what is a safe setting?

<details><summary>Answer</summary>

Whether the health response includes component details (each indicator's status and details like database type, disk space, errors). `when-authorized` (or `never` for public endpoints) is safe: anonymous callers and load balancers see only UP/DOWN; operators see details.

</details>

### BOOT-L3-015 · design
A payment provider health indicator: liveness group, readiness group, or neither?

<details><summary>Answer</summary>

Not liveness (restarting does not fix the provider). Readiness only if the service cannot do anything useful without it; if only checkout needs it, a failing provider should not take the whole service (product browsing) out of rotation: keep it out of readiness, expose it in the general health/metrics, and handle it with circuit breakers and fallbacks. Also mind health checks calling the provider too often (cache the result).

</details>

### BOOT-L3-016 · recall
How does Spring Boot read configuration in Kubernetes, typically?

<details><summary>Answer</summary>

Environment variables (from ConfigMaps and Secrets via `env`/`envFrom`), mounted files via `spring.config.import=configtree:/etc/config/` (each file name becomes a property), or `spring.config.import` of an external config server. Profiles are activated with `SPRING_PROFILES_ACTIVE`. The image stays identical across environments.

</details>

### BOOT-L3-017 · scenario
`/actuator/prometheus` has thousands of `http_server_requests` series and the metrics system is struggling. What is the likely cause?

<details><summary>Answer</summary>

High-cardinality tags: requests tagged with the raw URI (`/orders/12345`) instead of the route template (`/orders/{id}`), often from requests that did not match a handler or from custom tags with user ids, query strings or exception messages. Boot tags by template; check custom metrics and filters that set `uri`, and cap or drop unknown URIs.

</details>

### BOOT-L3-018 · recall
Which `@Conditional...` annotations would make a bean appear only on web applications, only if a class is present, and only if a property is set?

<details><summary>Answer</summary>

`@ConditionalOnWebApplication`, `@ConditionalOnClass(SomeClass.class)` (use `name = "..."` strings to avoid loading the class), and `@ConditionalOnProperty(prefix = "...", name = "...", havingValue = "...", matchIfMissing = ...)`. Others: `@ConditionalOnBean`, `@ConditionalOnMissingBean`, `@ConditionalOnResource`, `@ConditionalOnExpression`.

</details>

### BOOT-L3-019 · why
Why should outbound HTTP clients in a Boot app be beans built from Boot's builders (e.g. `RestClient.Builder`)?

<details><summary>Answer</summary>

The auto-configured builder applies shared settings: message converters, observation (metrics and tracing propagation of the trace id), customizers. A `new RestClient` per call loses those, creates connection pools repeatedly and usually has no timeouts. Build each client once as a bean with explicit timeouts and a base URL from properties.

</details>

### BOOT-L3-020 · interview
Your Boot service takes 25 seconds to start in Kubernetes and sometimes gets killed during startup. What do you check and change?

<details><summary>Answer</summary>

Use a startup probe (or generous initial delay) so liveness does not kill a slow start. Measure where time goes (startup actuator endpoint / `ApplicationStartup` events, logs): heavy bean initialisation, eager remote calls, Flyway migrations, large classpath scanning, little CPU during startup (CPU limits throttle JIT and class loading: give more CPU or fewer limits). Then: move remote calls out of startup, lazy-initialise rarely used beans, CDS/AOT, or a native image if startup is critical.

</details>
