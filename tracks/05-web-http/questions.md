# Web, HTTP and REST: question bank

Drill with `java quiz/Quiz.java --track web-http`.

## L1

### WEB-L1-001 · recall
What happens, step by step, when a client calls `https://shop.example.com/api/v1/products/7`?

<details><summary>Answer</summary>

DNS resolves the host to an IP; a TCP connection is opened to port 443 (handshake); TLS negotiates keys and the client verifies the server certificate for that host name; the HTTP request is sent inside TLS; the server (often a load balancer or proxy first) routes it to the application, which answers; the connection may be kept alive for more requests. Each step has its own errors and timeouts.

</details>

### WEB-L1-002 · recall
What are the parts of an HTTP/1.1 request? Why is the Host header mandatory?

<details><summary>Answer</summary>

Request line (`METHOD target HTTP/1.1`), header lines (`Name: value`), an empty line, and an optional body whose length is given by `Content-Length` (or chunked encoding). Host is mandatory because one IP address (one server or load balancer) serves many domains (virtual hosting); without it the server cannot know which site you mean.

</details>

### WEB-L1-003 · recall
401 vs 403?

<details><summary>Answer</summary>

401 Unauthorized actually means **unauthenticated**: no valid credentials (missing, expired, invalid token); the client should authenticate (with a `WWW-Authenticate` header). 403 Forbidden: the server knows who you are, and you are not allowed. Re-authenticating does not help a 403.

</details>

### WEB-L1-004 · recall
What do 200, 201, 202 and 204 mean? Which headers or bodies go with them?

<details><summary>Answer</summary>

200 OK: success with a body. 201 Created: a resource was created; include `Location` with its URL (and often the representation). 202 Accepted: the request was accepted for later processing (return a status URL to poll). 204 No Content: success, no body (DELETE, some PUTs).

</details>

### WEB-L1-005 · recall
What is the difference between a safe and an idempotent method? Classify GET, POST, PUT, PATCH, DELETE.

<details><summary>Answer</summary>

Safe: the request does not ask for any state change (GET, HEAD, OPTIONS). Idempotent: the effect of N identical requests is the same as one (all safe methods plus PUT and DELETE). POST and PATCH are neither in general. Safety allows caching and prefetching; idempotency allows automatic retries.

</details>

### WEB-L1-006 · scenario
A link `GET /orders/42/cancel` in an email cancels the order. Customers complain orders get cancelled without them clicking. Why?

<details><summary>Answer</summary>

GET is safe by definition, so email clients, link previewers, antivirus scanners and browsers prefetch it, and each fetch cancels the order. State changes must use POST (or DELETE/PUT), e.g. `POST /orders/42/cancel`, typically behind a confirmation page.

</details>

### WEB-L1-007 · why
Is DELETE idempotent if the first call returns 204 and the second 404?

<details><summary>Answer</summary>

Yes. Idempotency is about the effect on server state, not the response: after one or two DELETEs the resource is gone. Clients retrying a DELETE should treat 404 as "already done".

</details>

### WEB-L1-008 · recall
PUT vs PATCH vs POST for updating a product?

<details><summary>Answer</summary>

PUT replaces the whole representation at a known URL (fields you omit are reset), idempotent. PATCH applies a partial change (JSON Merge Patch or JSON Patch), not necessarily idempotent ("increment stock" is not). POST is for creating in a collection or running an action; not idempotent.

</details>

### WEB-L1-009 · bug
Why is this API response a problem?

```
HTTP/1.1 200 OK
{"success": false, "error": "Product not found"}
```

<details><summary>Answer</summary>

The status says success. Caches may store it, monitoring counts it as healthy, retry logic and client libraries do not see an error, and every client must parse the body to find out. Use the right status (404) with a standard error body (problem details).

</details>

### WEB-L1-010 · recall
What do 502, 503 and 504 mean, and which side caused them?

<details><summary>Answer</summary>

All are server-side, usually seen at a proxy/gateway. 502 Bad Gateway: the upstream returned an invalid response or the connection failed. 503 Service Unavailable: overloaded, starting up, or in maintenance (often with `Retry-After`). 504 Gateway Timeout: the upstream did not answer in time. All three are usually transient and retryable for idempotent requests.

</details>

### WEB-L1-011 · recall
What is the difference between `Content-Type` and `Accept`?

<details><summary>Answer</summary>

`Content-Type` describes the body being sent (request or response). `Accept` (request only) lists the formats the client wants back. A server that cannot read the sent type answers 415; one that cannot produce any accepted type answers 406.

</details>

### WEB-L1-012 · design
Design REST endpoints for: listing a customer's orders, creating an order, getting one order, cancelling an order.

<details><summary>Answer</summary>

`GET /api/v1/customers/{id}/orders?status=PAID&cursor=...` (or `GET /api/v1/orders?customerId=` for admins / `GET /api/v1/me/orders` for the current user), `POST /api/v1/orders` → 201 + Location, `GET /api/v1/orders/{id}` → 200 or 404, `POST /api/v1/orders/{id}/cancel` → 200 with the updated order, or 409 if it cannot be cancelled. Nouns, plural, an action as a POST sub-resource.

</details>

### WEB-L1-013 · recall
How is a space encoded in a URL query? What about `/` and `&` inside a parameter value?

<details><summary>Answer</summary>

Percent-encoding: space is `%20` (or `+` in `application/x-www-form-urlencoded` query strings), `/` is `%2F`, `&` is `%26`, `=` is `%3D`. Decode with UTF-8. Always build URLs with an encoder (UriComponentsBuilder, URLEncoder for values) instead of string concatenation.

</details>

### WEB-L1-014 · recall
What is HTTP/2's main improvement over HTTP/1.1, and does your REST code change?

<details><summary>Answer</summary>

Binary framing and **multiplexing**: many concurrent requests on one TCP connection without head-of-line blocking at the HTTP level, plus header compression (HPACK). Semantics (methods, status codes, headers) are unchanged, so application code does not change. HTTP/3 runs over QUIC (UDP), avoiding TCP head-of-line blocking.

</details>

### WEB-L1-015 · predict
`curl -v http://localhost:8080/api` prints `connect to 127.0.0.1 port 8080 failed: Connection refused`. What does it tell you, compared with a timeout?

<details><summary>Answer</summary>

Refused: the machine answered immediately with a TCP reset: nothing is listening on that port (app not started, wrong port, bound to another interface). A connect **timeout** means no answer at all: a firewall dropping packets, a wrong IP, a network route problem.

</details>

### WEB-L1-016 · recall
What are 301, 302, 307 and 308, and what is the difference between the pairs?

<details><summary>Answer</summary>

Redirects with a `Location` header. 301 (permanent) and 302 (temporary) historically let clients change POST to GET when following; 308 (permanent) and 307 (temporary) require keeping the method and body. Use 308/307 for APIs; browsers and caches remember 301/308.

</details>

### WEB-L1-017 · why
Why version an API (e.g. `/api/v1`) from day one?

<details><summary>Answer</summary>

Clients you do not control (mobile apps that users do not update, partners) keep calling the old contract. A version gives you a place to introduce breaking changes as v2 while v1 keeps working during a deprecation period. Without it, every breaking change breaks someone.

</details>

### WEB-L1-018 · recall
What is RFC 9457 problem details?

<details><summary>Answer</summary>

A standard JSON error format (`application/problem+json`) with `type` (a URI identifying the error kind), `title`, `status`, `detail` and `instance`, extensible with your own fields (field errors, trace id). Spring returns it via `ProblemDetail`/`ErrorResponse`. It gives clients one shape to parse for all errors.

</details>

### WEB-L1-019 · scenario
A client sends a POST, the connection drops before the response arrives. What does the client know about whether the order was created?

<details><summary>Answer</summary>

Nothing: the request may never have arrived, may have been processed with the response lost, or may still be running. That is why non-idempotent operations need an idempotency key, so the client can safely retry and the server returns the original result if it already ran.

</details>

### WEB-L1-020 · recall
When should an API answer 404 instead of 403 for a resource the user is not allowed to see?

<details><summary>Answer</summary>

When revealing that the resource **exists** would leak information (another customer's order id, a private repository). Returning 404 for "not yours" is common practice (GitHub does it). Use 403 when existence is not secret and the user needs to know it is a permission problem.

</details>

## L2

### WEB-L2-001 · recall
`Cache-Control: no-cache` vs `no-store` vs `max-age=60`?

<details><summary>Answer</summary>

`max-age=60`: fresh for 60 s, reuse without asking. `no-cache`: may be stored but must be revalidated with the origin every time before use (cheap with ETags: 304). `no-store`: never store anywhere (sensitive data). `no-cache` does not mean "do not cache".

</details>

### WEB-L2-002 · recall
How do ETags reduce bandwidth?

<details><summary>Answer</summary>

The server sends `ETag: "v7"` with the response. Next time the client sends `If-None-Match: "v7"`; if the resource has not changed, the server answers **304 Not Modified** with no body, and the client uses its cached copy. Cost: computing the ETag (cheap if it comes from a version column).

</details>

### WEB-L2-003 · scenario
Two admins edit the same product in their browsers; both save. How do you prevent the second save from silently overwriting the first over HTTP?

<details><summary>Answer</summary>

Optimistic concurrency with ETags: the GET returns `ETag: "v7"`; the PUT sends `If-Match: "v7"`; the server updates only if the current version is still v7 (an atomic `UPDATE ... WHERE version = 7`), otherwise answers **412 Precondition Failed**; the second admin reloads and reapplies. Optionally require If-Match on updates (428 without it).

</details>

### WEB-L2-004 · recall
Strong vs weak ETags, and which comparison does `If-Match` use?

<details><summary>Answer</summary>

A strong ETag (`"abc"`) promises byte-identical representations; a weak one (`W/"abc"`) only semantic equivalence (e.g. same content, different compression). `If-Match` uses **strong** comparison (weak tags never match: you cannot safely update based on "roughly equal"); `If-None-Match` uses weak comparison.

</details>

### WEB-L2-005 · interview
How do idempotency keys work for POST requests? What exactly does the server store?

<details><summary>Answer</summary>

The client creates a unique key per logical attempt and sends it in `Idempotency-Key` on every retry. The server, atomically with the operation (same transaction, unique constraint on the key, scoped per client), stores the key, a fingerprint of the request (hash of the body) and the response. Retries with the same key and fingerprint get the stored response without re-executing; a different fingerprint is a 422; a concurrent duplicate while the first runs gets 409 (or waits). Failed attempts that changed nothing are not stored. Keys expire (e.g. 24 h).

</details>

### WEB-L2-006 · why
Why must the idempotency key be stored in the same transaction as the operation?

<details><summary>Answer</summary>

Otherwise a crash between the two leaves an order without a recorded key (a retry creates a second order) or a recorded key without an order (a retry replays a response for something that did not happen). One transaction makes "operation done" and "key recorded" atomic; the unique constraint makes concurrent duplicates fail cleanly across instances.

</details>

### WEB-L2-007 · why
Why should pagination cursors be opaque, and why sign them?

<details><summary>Answer</summary>

Opaque: clients cannot depend on the format, so you can change it (add a sort field, switch encoding) without breaking them. Signed (HMAC) or encrypted: clients cannot craft cursors that point to arbitrary positions, bypass filters or inject values; a tampered cursor is rejected with 400.

</details>

### WEB-L2-008 · recall
What is CORS, and what triggers a preflight request?

<details><summary>Answer</summary>

Cross-Origin Resource Sharing: how a server tells browsers that pages from another origin (scheme + host + port) may read its responses (`Access-Control-Allow-Origin`). A preflight `OPTIONS` request is sent first for non-simple requests: methods other than GET/HEAD/POST, custom headers like `Authorization` or `Idempotency-Key`, or content types other than form/text (e.g. `application/json`). `Access-Control-Max-Age` caches the preflight answer.

</details>

### WEB-L2-009 · why
Why is CORS not a security mechanism for your API?

<details><summary>Answer</summary>

It is enforced by browsers to protect **users** from malicious pages reading their data from other sites. Any non-browser client (curl, a script, a server) ignores CORS entirely. Your API still needs authentication and authorization on every request.

</details>

### WEB-L2-010 · recall
Response headers like `ETag` or `Retry-After` are present, but the frontend's `response.headers.get('ETag')` returns null on a cross-origin call. Why?

<details><summary>Answer</summary>

For cross-origin responses, browsers expose only "safelisted" headers to JavaScript unless the server lists others in `Access-Control-Expose-Headers` (e.g. `ETag, Location, Retry-After, X-Trace-Id`).

</details>

### WEB-L2-011 · interview
Bearer token in a header vs session cookie: compare CSRF, XSS and revocation.

<details><summary>Answer</summary>

Bearer token: sent explicitly by code, so no CSRF; but if stored in JS-readable storage, an XSS can steal it; stateless JWTs cannot be revoked before expiry (short lifetimes, refresh tokens, deny lists). Cookie: sent automatically by the browser, so CSRF must be prevented (`SameSite=Lax/Strict`, CSRF tokens); `HttpOnly` protects it from XSS theft; server-side sessions are revocable instantly. Both need HTTPS (`Secure`).

</details>

### WEB-L2-012 · recall
What do `Vary` and `private` do for caches?

<details><summary>Answer</summary>

`Vary: Accept-Encoding, Accept-Language` tells caches that the response depends on those request headers, so they store variants separately (and do not serve German to English users). `private` means only the end user's browser may store it, not shared caches like CDNs; use it (or `no-store`) for personalised responses.

</details>

### WEB-L2-013 · design
Design the error response for a validation failure with two invalid fields.

<details><summary>Answer</summary>

`400` (or 422) with `Content-Type: application/problem+json`: `{"type":"https://api.example.com/errors/validation","title":"Validation failed","status":400,"detail":"2 fields are invalid","instance":"/api/v1/products","errors":[{"field":"price","message":"must be at least 0"},{"field":"sku","message":"must match [A-Z0-9-]+"}],"traceId":"..."}`. Machine-readable type and field names, human messages, no internals.

</details>

### WEB-L2-014 · recall
Why should an API cap the page size, and what about returning a total count?

<details><summary>Answer</summary>

Without a cap, one request can ask for a million rows: memory, database load and response time explode (a cheap denial of service). Cap server-side (e.g. max 100) and document it. A total count needs `COUNT(*)` over the filtered set on every request, which is expensive on large tables; offer it only where needed, or approximate it.

</details>

### WEB-L2-015 · recall
What does `Retry-After` contain, and with which status codes is it used?

<details><summary>Answer</summary>

Either a number of seconds (`Retry-After: 30`) or an HTTP date. Used with 429 Too Many Requests and 503 Service Unavailable (and redirects). Well-behaved clients wait at least that long before retrying.

</details>

### WEB-L2-016 · scenario
Your API serves `/api/v1/me/profile` with `Cache-Control: max-age=300` and a CDN in front. What can go wrong?

<details><summary>Answer</summary>

Without `private`, a shared cache may store one user's profile and serve it to other users (the URL is the same for everyone): a data leak. Personalised responses need `Cache-Control: private` (or `no-store`), and the CDN should not cache authenticated requests. Many real incidents are exactly this.

</details>

### WEB-L2-017 · recall
What is content negotiation? Give an example where it matters.

<details><summary>Answer</summary>

Choosing a representation from request headers: `Accept` (JSON vs CSV export vs problem+json), `Accept-Language`, `Accept-Encoding` (gzip/br). Example: `GET /reports/2026-10` with `Accept: text/csv` returns a CSV, with `application/json` JSON; the server sets `Content-Type` and `Vary: Accept`.

</details>

### WEB-L2-018 · why
Why should the same frontend and API origin often be preferred over CORS?

<details><summary>Answer</summary>

With a reverse proxy serving both from one origin, the browser makes no cross-origin requests: no preflights (one round trip less per non-simple call), no CORS configuration to get wrong, cookies work with `SameSite=Strict`, and internal endpoints (actuator) are never exposed by accident.

</details>

### WEB-L2-019 · bug
What is wrong with putting an API key in the URL: `GET /api/v1/orders?apiKey=sk_live_...`?

<details><summary>Answer</summary>

URLs end up in server and proxy access logs, browser history, analytics tools and `Referer` headers sent to other sites. The secret leaks. Send credentials in headers (`Authorization`), over HTTPS only.

</details>

### WEB-L2-020 · recall
What is the difference between `If-None-Match: *` and `If-Match: *` on a PUT?

<details><summary>Answer</summary>

`If-None-Match: *`: proceed only if the resource does **not** exist (create-only; prevents overwriting someone else's creation; otherwise 412). `If-Match: *`: proceed only if it **does** exist (update-only; prevents accidentally creating it).

</details>

## L3

### WEB-L3-001 · recall
Which timeouts should a service-to-service HTTP call have, and how do you pick values?

<details><summary>Answer</summary>

Connect timeout (short: ~100-500 ms), read/response timeout (from the dependency's measured p99/p99.9 plus margin), and an overall deadline including retries, shorter than your caller's timeout. Also pool acquisition timeouts. Measure; revisit when SLOs change. Never rely on defaults, which are often infinite.

</details>

### WEB-L3-002 · scenario
A downstream service starts answering in 30 s instead of 30 ms. Your service has no timeouts on that call. What happens to your service?

<details><summary>Answer</summary>

Each request waits 30 s holding a thread (and maybe a DB connection). With 200 threads and 100 req/s, the pool is exhausted in 2 seconds; new requests queue, then time out at the load balancer; health checks may fail and pods get restarted; your callers back up too: a cascading failure from one slow dependency. Timeouts, bulkheads and circuit breakers contain it.

</details>

### WEB-L3-003 · interview
Give five rules for safe retries.

<details><summary>Answer</summary>

(1) Only idempotent operations or those with idempotency keys. (2) Only transient failures (connection errors, timeouts, 502/503/504, 429), never 4xx or 500. (3) Exponential backoff with jitter. (4) Honour Retry-After. (5) Few attempts, a total deadline, retries at one layer only, and a retry budget so retries cannot amplify an outage.

</details>

### WEB-L3-004 · why
Why add jitter to backoff? What is full jitter?

<details><summary>Answer</summary>

Clients that failed at the same moment (a dependency blip) would otherwise retry at the same moments (100 ms, 200 ms, 400 ms...), hitting the recovering service in synchronised waves. Jitter randomises delays to spread the load. Full jitter: `delay = random(0, min(cap, base × 2^attempt))`, which AWS's analysis found to give the best throughput with low total work.

</details>

### WEB-L3-005 · recall
What is a retry storm, and how big can it get?

<details><summary>Answer</summary>

When a service slows down, all its clients retry, multiplying load exactly when it is weakest, so it never recovers. Nested retries multiply: 3 layers each with 3 retries (4 attempts) turn 1 call into 4³ = 64 at the bottom. Prevent with retries at a single layer, budgets, backoff with jitter and circuit breakers.

</details>

### WEB-L3-006 · recall
Explain the three states of a circuit breaker.

<details><summary>Answer</summary>

Closed: calls pass, failures are counted. Open (after a failure threshold or rate): calls fail immediately without being made, for a cool-down period, so threads are not wasted and the dependency can recover. Half-open (after the cool-down): a limited number of trial calls; success closes the circuit, failure reopens it.

</details>

### WEB-L3-007 · design
A product page calls a recommendation service. What should happen when it is down?

<details><summary>Answer</summary>

The page still renders without recommendations (or with a cached/static "popular items" list): a fallback. Behind it: a short timeout, a circuit breaker so failed calls cost nothing, a bulkhead so recommendation calls cannot consume checkout's threads, and a metric/alert for the degraded mode. Product details and checkout must not depend on it.

</details>

### WEB-L3-008 · recall
Bulkhead vs circuit breaker vs rate limiter?

<details><summary>Answer</summary>

Bulkhead: limits **concurrency** per dependency (separate pool or semaphore), isolating failures. Circuit breaker: stops calling a **failing** dependency for a while. Rate limiter: limits how **often** something is allowed (protecting a server from clients, or a dependency from you). They are complementary.

</details>

### WEB-L3-009 · dsa
Why does a fixed-window rate limiter allow twice the limit, and how does the sliding window counter fix it?

<details><summary>Answer</summary>

Fixed windows reset at boundaries: `limit` requests at 12:00:59 and `limit` more at 12:01:00 are both allowed, 2× the limit within one second. The sliding window counter estimates the count over the last full window: `previous_count × (overlap fraction) + current_count`. Just after the boundary the previous window still counts almost fully, so the burst is refused. O(1) memory per client (two counters).

</details>

### WEB-L3-010 · design
How do you rate-limit an API that runs on 10 instances?

<details><summary>Answer</summary>

Local limiters allow 10× the limit (or limit/10 each, unfair with uneven balancing). For exact limits use a shared store: Redis with an atomic script (token bucket or sliding window per key, with TTL), or enforce at the API gateway/edge. Trade-offs: an extra network hop per request, and what to do when Redis is down (fail open with local limits, usually).

</details>

### WEB-L3-011 · recall
Rate limiting vs load shedding?

<details><summary>Answer</summary>

Rate limiting caps individual clients (fairness, abuse protection; 429). Load shedding protects the service as a whole when it is saturated, regardless of who sends the traffic: reject some requests early and cheaply (503), preferably low-priority ones, so the rest still succeed instead of everything timing out.

</details>

### WEB-L3-012 · why
Why must `X-Forwarded-For` only be trusted from your own proxies?

<details><summary>Answer</summary>

Any client can send the header. If the application trusts it from anywhere, an attacker can fake their IP: bypass IP rate limits and allow-lists, poison logs and audits. Configure the server to accept forwarded headers only from known proxy addresses (Spring: `server.forward-headers-strategy` with trusted proxy ranges), and have the edge proxy overwrite, not append, client-supplied values.

</details>

### WEB-L3-013 · scenario
Users see occasional 502 errors that you cannot reproduce. The load balancer's idle timeout is 60 s and your server's keep-alive timeout is 75 s... or the other way round. Which way causes 502s?

<details><summary>Answer</summary>

When the **server's** idle timeout is shorter than the load balancer's (e.g. server 30 s, LB 60 s): the server closes an idle connection while the LB still considers it usable, sends the next request on it, and gets a reset: 502. The server's keep-alive timeout must be longer than the proxy's idle timeout.

</details>

### WEB-L3-014 · recall
Which API changes are backwards compatible, and which are breaking?

<details><summary>Answer</summary>

Compatible: new endpoints, new optional request fields, new response fields (if clients ignore unknown fields), new enum values only if clients handle unknown ones. Breaking: removing/renaming fields or endpoints, changing types or meaning, making a field required, stricter validation, changing status codes or error formats, changing default behaviour.

</details>

### WEB-L3-015 · design
How do you roll out a breaking API change?

<details><summary>Answer</summary>

Prefer expand/contract (add the new field alongside the old, migrate clients, remove later). If a break is unavoidable: release v2 next to v1, announce, mark v1 with `Deprecation`/`Sunset` headers and docs, monitor v1 traffic per client, help the remaining clients migrate, then remove v1. Contract tests and OpenAPI diffs in CI catch accidental breaks.

</details>

### WEB-L3-016 · recall
What are RED metrics, and why label by route template rather than raw path?

<details><summary>Answer</summary>

Rate (requests/s), Errors (by status class), Duration (latency histograms, p50/p95/p99), per endpoint. Raw paths contain ids (`/orders/123`, `/orders/124`...), creating a new time series per id: unbounded cardinality that overloads the metrics system. The template (`/orders/{id}`) keeps a fixed number of series.

</details>

### WEB-L3-017 · why
Why should connections to dependencies be pooled and kept alive?

<details><summary>Answer</summary>

A new connection costs a TCP handshake plus a TLS handshake (one or two round trips each, tens of milliseconds across zones) and CPU for crypto. Reusing pooled keep-alive connections avoids that per request. But size pools: too small and requests queue for a connection; too large and the dependency is flooded.

</details>

### WEB-L3-018 · scenario
After a deploy, clients get 503s for a few seconds from each restarting pod. How do you make deploys invisible?

<details><summary>Answer</summary>

Readiness probes so new pods get traffic only when ready (and warmed up); on shutdown, fail readiness first, wait for the load balancer to stop routing (preStop hook), then let the server drain in-flight requests (graceful shutdown) before exiting; rolling updates with maxUnavailable 0; client retries on 503 for idempotent calls as a safety net.

</details>

### WEB-L3-019 · recall
What is deadline propagation?

<details><summary>Answer</summary>

Passing the remaining time budget of the original request to downstream calls, so each layer uses at most what is left instead of its own fixed timeout. If the user's request has 300 ms left, a downstream call with a 2 s timeout wastes work whose result nobody will see. gRPC does it natively; with HTTP, use a header and compute timeouts from it.

</details>

### WEB-L3-020 · interview
Your service depends on five other services. Its availability target is 99.9%. Each dependency has 99.9%. What is the problem, and what do you do?

<details><summary>Answer</summary>

If every request needs all five synchronously, availability is roughly 0.999⁵ ≈ 99.5%, below your target (and latencies add up). Mitigations: make dependencies optional with fallbacks and graceful degradation, cache their data, call them asynchronously (events, queues) where the user does not need the result immediately, call them in parallel with timeouts, and use retries plus circuit breakers for transient failures.

</details>
