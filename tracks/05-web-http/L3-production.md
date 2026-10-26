# Web, HTTP and REST, L3: resilient APIs in production

**Goal:** keep a service up when its dependencies are slow or down and when clients misbehave: timeouts, retries done right, circuit breakers, bulkheads, rate limiting and load shedding, API evolution without breaking clients, and the proxies between your users and your code.

Prerequisite: L1, L2, concurrency L2. Katas: `cd katas && ./mvnw test -Dgroups="web & L3"`. Drill: `java quiz/Quiz.java --track web-http --level 3`.

---

## 1. Timeouts: the first line of defence

Every remote call needs:
- a **connect timeout** (hundreds of ms: a healthy server accepts quickly),
- a **read/response timeout** (based on the dependency's p99.9, not its average),
- an **overall deadline** for the whole operation, shorter than your own caller's timeout.

Without them, a hung dependency holds a thread (and a DB connection, a request slot) per waiting call until the pool is exhausted, and **your** service stops answering: the failure cascades upstream. Defaults are often infinite (`java.net.http.HttpClient` has no request timeout unless you set one).

**Deadline propagation:** if the user's request has 2 s left, calling a downstream with a 5 s timeout is pointless. gRPC propagates deadlines; with HTTP, pass a header or compute remaining time.

---

## 2. Retries done right

Retries help with **transient** failures (a dropped connection, a pod restarting, a 503 during a deploy). Done wrong they multiply load at the worst moment ("retry storm"): with 3 retries at each of 3 layers, one user request becomes 4³ = 64 calls to the bottom service.

Rules:
1. Retry only what is **safe to repeat**: idempotent methods, or POST with an idempotency key.
2. Retry only **transient** failures: connection errors, timeouts, 502/503/504, 429. Not 4xx (it will fail again), not 500 (usually a bug).
3. **Exponential backoff with jitter**: wait `random(0, min(cap, base × 2^attempt))` ("full jitter"). Without jitter, all clients that failed together retry together, in synchronised waves.
4. Honour **`Retry-After`** from 429/503.
5. **Bound** attempts (2-3) and total time; retry at **one** layer (usually the outermost client or the service mesh), not every layer.
6. A **retry budget** (e.g. retries ≤ 10% of requests) stops retries from amplifying an outage.

> **Kata `RetryPolicy`**: which failures to retry, and the backoff delay.

---

## 3. Circuit breakers and bulkheads

A **circuit breaker** wraps calls to a dependency:
- **Closed:** calls pass; failures are counted (consecutive, or failure rate over a sliding window).
- **Open:** after too many failures, calls fail **immediately** without reaching the dependency (use a fallback: cached data, a default, a degraded page). This saves your threads and gives the dependency room to recover.
- **Half-open:** after a cool-down, a few trial calls; success closes the circuit, failure opens it again.

A **bulkhead** isolates resources per dependency (a separate bounded thread pool or a semaphore of N permits), so a slow recommendation service can consume at most its share of capacity while checkout keeps working.

Libraries: Resilience4j (circuit breaker, retry, bulkhead, rate limiter, time limiter), or a service mesh (Envoy/Istio) doing the same at the network level. Fallbacks must be designed with the business: "show the product without recommendations" is fine; "accept the payment without the fraud check" is not.

> **Kata `CircuitBreaker`**: the three states with a fake clock and a single half-open trial.

---

## 4. Rate limiting and load shedding

**Rate limiting** protects you from individual clients (abuse, bugs, scrapers): limits per API key, user or IP, answering **429** with `Retry-After` (and often `RateLimit-*` headers showing the remaining quota). Algorithms:
- **Token bucket** (concurrency track L2): bursts up to capacity, steady refill.
- **Fixed window counter**: simple, but allows 2× the limit across a boundary.
- **Sliding window log**: exact, stores every timestamp.
- **Sliding window counter**: two counters per client, weighted; close to exact at O(1) memory.

Across many instances, local limiters allow N × the limit; use a shared store (Redis with atomic Lua scripts) or the API gateway if exactness matters.

**Load shedding** protects you from **everyone at once**: when saturated (queue too long, latency too high, CPU pegged), reject some requests early and cheaply (503) instead of accepting all and timing out all. Prioritise: shed analytics before checkout, anonymous before logged-in.

> **Kata `SlidingWindowLimiter`**: the two-counter approximation, and why it stops the boundary burst.

---

## 5. Proxies, gateways and what they change

Between the user and your code there may be a CDN, a load balancer, an API gateway, an ingress controller, a sidecar proxy. Consequences:
- The client's IP and scheme arrive in **`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`** (or `Forwarded`). Trust them only from your own proxies, or anyone can spoof their IP (and your rate limiter, logs and redirect URLs go wrong).
- **Timeouts stack**: the load balancer's idle timeout (often 60 s) must be longer than your server's, or it closes connections the server still uses (sporadic 502s).
- **TLS termination** at the edge: inside, traffic may be plain HTTP; your app must know the original scheme for redirects and cookies (`Secure`).
- **Connection reuse:** keep-alive pools to dependencies avoid a TCP+TLS handshake per request (tens of ms). Size the pools; a pool of 5 connections to a service you call 500 times per second becomes a queue.
- **Health checks:** the load balancer only sends traffic to instances whose readiness endpoint says yes; during shutdown, fail readiness first, then drain.

---

## 6. Evolving an API without breaking clients

- **Additive changes are safe:** new endpoints, new optional request fields, new response fields. Clients must ignore unknown fields (Jackson: `FAIL_ON_UNKNOWN_PROPERTIES` off for clients; Spring Boot's default).
- **Breaking changes:** removing or renaming fields, changing types or meaning, making optional fields required, changing status codes, tightening validation. These need a new version (`/api/v2`, or a media type/version header), a deprecation period (`Deprecation` and `Sunset` headers), monitoring of who still calls the old version, then removal.
- **Expand/contract** works for APIs as for databases: add the new field, migrate clients, then remove the old field.
- Contract tests (consumer-driven: Pact, Spring Cloud Contract) catch breaking changes in CI; an OpenAPI spec diff does too.

---

## 7. Observability at the HTTP layer

- **RED metrics** per endpoint: **R**ate, **E**rrors (by status class), **D**uration (histograms for p50/p95/p99).
- Correlation: accept or generate a request id / `traceparent`, return it in responses, log it with every line, propagate it to every downstream call.
- Access logs with method, route template (not the raw path with ids: cardinality), status, latency, bytes, client.
- Synthetic checks from outside (is the API reachable through DNS, TLS, the CDN?).

---

## 8. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `RetryPolicy` | retry rules, exponential backoff, full jitter, Retry-After | |
| `CircuitBreaker` | state machine, fail fast, half-open trial, thread safety | state machine |
| `SlidingWindowLimiter` | rate limiting per client | sliding window counter |

```
cd katas && ./mvnw test -Dgroups="web & L3"
```

## 9. Seen in the shop project

- Per-client token-bucket rate limiting with 429 + `Retry-After` (`ratelimit/`, Level 5).
- Timeouts on the database pool (`connection-timeout`), graceful shutdown with readiness probes (Level 6).
- Trusting `X-Forwarded-*` only from private networks (`server.forward-headers-strategy=native` in the prod profile), and the nginx fix that forwards the port (frontend level).
- Trace ids in every response and error body (Level 6).

## 10. Interview talk track

"Every remote call gets a connect and a read timeout shorter than my own deadline, because a missing timeout turns a slow dependency into my outage. I retry only idempotent operations, or POSTs with an idempotency key, only on transient failures, two or three times with exponential backoff and full jitter, honouring Retry-After, and at a single layer, so retries cannot multiply. Dependencies sit behind circuit breakers with fallbacks that the business agreed on, and bulkheads so one slow dependency cannot take all threads. Clients are rate-limited with a token bucket or sliding window and get 429 with Retry-After, and under overload we shed load early with 503 instead of timing everything out. Behind proxies I trust forwarded headers only from our own proxies and align the timeouts. APIs evolve additively; breaking changes get a new version, deprecation headers and contract tests."

**Questions you should be able to answer:**
1. Which timeouts should a service-to-service HTTP call have, and how do you choose them?
2. What is a retry storm? Five rules for safe retries.
3. Why jitter? What is full jitter?
4. Explain the circuit breaker states. What is a good fallback, and what is a dangerous one?
5. Bulkhead vs circuit breaker vs rate limiter.
6. Token bucket vs fixed window vs sliding window. How do you rate-limit across 10 instances?
7. Rate limiting vs load shedding.
8. Why must `X-Forwarded-For` only be trusted from your own proxies?
9. Which API changes are breaking? How do you roll out a breaking change?
10. What are RED metrics, and why use the route template as a metric label?
