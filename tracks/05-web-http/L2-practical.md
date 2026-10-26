# Web, HTTP and REST, L2: caching, idempotency, pagination, CORS, auth headers

**Goal:** design APIs that are safe to retry, cheap to cache, stable to paginate and usable from a browser: ETags and conditional requests, idempotency keys, cursors, CORS, cookies vs tokens, and API error contracts.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="web & L2"`. Drill: `java quiz/Quiz.java --track web-http --level 2`.

---

## 1. HTTP caching

Caches sit in browsers, CDNs and reverse proxies. The server controls them with `Cache-Control`:

| Directive | Meaning |
|---|---|
| `max-age=60` | fresh for 60 s: reuse without asking |
| `no-cache` | may store, but must revalidate with the server before each use (with an ETag: a cheap 304) |
| `no-store` | never store (personal or sensitive data) |
| `private` / `public` | only the user's browser / shared caches (CDN) too |
| `immutable` | never changes (hashed static files: cache for a year) |
| `s-maxage`, `stale-while-revalidate` | shared-cache lifetime; serve stale while refreshing in the background |

Rules: responses with personal data are `private` or `no-store`; `Authorization`-bearing requests are not stored by shared caches by default; `Vary: Accept-Encoding, Accept-Language` tells caches which request headers change the response.

---

## 2. Content negotiation and compression

The client says `Accept: application/json` (and `Accept-Encoding: gzip, br`); the server picks a representation, sets `Content-Type` (and `Content-Encoding: gzip`), or answers **406** (cannot produce any accepted type) / **415** (cannot consume the sent `Content-Type`). JSON responses compress 5-10x; let the server or proxy compress anything above ~1 KB.

---

## 3. ETags and conditional requests

An **ETag** is a version label of a representation (`ETag: "v7"`, or weak `W/"v7"` = semantically equal, not byte-equal).

**Saving bandwidth (revalidation):**
```
GET /products/7                       -> 200, ETag: "v7", body
GET /products/7  If-None-Match: "v7"  -> 304 Not Modified, no body (nothing changed)
```

**Preventing lost updates (optimistic concurrency over HTTP):**
```
GET /products/7                       -> ETag: "v7"
PUT /products/7  If-Match: "v7"       -> 200, ETag: "v8"
PUT /products/7  If-Match: "v7"       -> 412 Precondition Failed (someone else saved v8 first)
```

`If-Match` uses **strong** comparison, `If-None-Match` **weak**; `*` means "any current version" (`If-None-Match: *` on PUT = "create only if it does not exist"). APIs can require `If-Match` on updates (`428 Precondition Required` without it). The ETag is usually derived from the row's version column (SQL track L3) or a hash of the body.

> **Kata `ConditionalRequests`**: the server's decision table.

---

## 4. Idempotency keys

POST is not idempotent, but clients must retry (timeouts, dropped connections). The **Idempotency-Key** pattern (Stripe, payment APIs, the shop's orders):
1. The client generates a unique key (a UUID) per **attempt** of an operation and sends it with every retry of that attempt.
2. The server stores key → (request fingerprint, response) when the operation completes, in the same transaction as the operation (a unique constraint makes it race-free across instances).
3. A retry with the same key and the same body gets the **stored response** (marked e.g. `Idempotent-Replayed: true`), and nothing runs twice.
4. Same key, different body → **422** (client bug). Same key while the first is still running → **409** (or wait).
5. Failures that did not change anything (5xx before commit) are not stored, so a retry can run.
6. Keys expire after a while (24 h).

> **Kata `IdempotencyStore`**: the in-memory version, including concurrent duplicates.

---

## 5. Pagination contracts

```json
GET /api/v1/products?size=20&cursor=eyIxMjAwOjgifQ.mL2...
{
  "items": [ ... ],
  "nextCursor": "eyIzNTAwOjIifQ.Xk9..."      // null on the last page
}
```

- **Cursor (keyset) pagination** for feeds and large tables (SQL track L2); the cursor is **opaque**: the client cannot parse or build it, so you can change its format, and **signed** (HMAC) or encrypted so it cannot be forged to bypass filters.
- Offset pagination (`page=3&size=20`, with `totalElements`) only for small, stable lists; counting all rows is itself expensive on big tables.
- Cap `size` server-side (e.g. max 100), or one request can ask for a million rows.
- Return links (`Link: <...>; rel="next"`) or the cursor; never make clients compute URLs.

> **Kata `CursorCodec`**: Base64URL + HMAC-SHA256, tamper-proof, constant-time comparison.

---

## 6. CORS: browsers and other origins

A browser page from `https://shop.example.com` calling `https://api.example.com` is **cross-origin** (origin = scheme + host + port). The browser enforces the **same-origin policy**: it lets the page read the response only if the API allows it with CORS headers.

- Simple requests (GET without custom headers) are sent; the response is readable only with `Access-Control-Allow-Origin: https://shop.example.com`.
- Others (JSON body, `Authorization`, custom headers, PUT/DELETE) first send a **preflight** `OPTIONS` with `Access-Control-Request-Method/-Headers`; the server answers with `Access-Control-Allow-Methods`, `-Allow-Headers`, `-Max-Age`.
- Response headers are hidden from JavaScript unless listed in `Access-Control-Expose-Headers` (`ETag`, `Location`, `Retry-After`).
- Credentials (cookies) cross-origin need `Access-Control-Allow-Credentials: true` **and** an explicit origin (not `*`).
- CORS protects **users' browsers**, not your API: curl ignores it. It is not authentication.
- The simplest design: serve the frontend and API from the **same origin** (a reverse proxy), and no CORS is needed (the shop project's frontend level does this).

---

## 7. Authentication over HTTP: tokens and cookies

| | Bearer token in `Authorization` | Session cookie |
|---|---|---|
| Sent | by your code, explicitly | automatically by the browser |
| CSRF risk | none (other sites cannot add the header) | yes: needs `SameSite` and/or CSRF tokens |
| XSS risk | if stored in JS-readable storage, can be stolen | `HttpOnly` cookies cannot be read by JS |
| Revocation | stateless JWTs live until expiry: keep them short, use refresh tokens | delete the server-side session |
| Typical | APIs, mobile apps, service-to-service | server-rendered web apps, BFFs |

Always HTTPS. Never put tokens or passwords in URLs (they end up in logs and browser history). Security track 09 goes deep.

---

## 8. Error contracts

Clients need errors they can act on:

```json
HTTP/1.1 422 Unprocessable Content
Content-Type: application/problem+json

{ "type": "https://shop.example.com/errors/insufficient-stock",
  "title": "Insufficient stock", "status": 422,
  "detail": "Only 3 units of MUG-1 left", "instance": "/api/v1/orders",
  "productId": 1, "traceId": "4bf92f3577b34da6a3ce929d0e0e4736" }
```

- One shape for all errors (RFC 9457 problem details), a stable machine-readable `type` (or code), a human `detail`, field errors for validation, and a trace id for support.
- Never leak stack traces, SQL or internal class names.

---

## 9. Katas for this level

| Kata | Practises |
|---|---|
| `ConditionalRequests` | ETags, If-Match / If-None-Match, 304 and 412, strong vs weak comparison |
| `IdempotencyStore` | idempotency keys, replay, 409/422, not storing failures, thread safety |
| `CursorCodec` | opaque, signed cursors, Base64URL, HMAC, constant-time comparison |

```
cd katas && ./mvnw test -Dgroups="web & L2"
```

## 10. Seen in the shop project

- ETag + `If-None-Match` (304) and `If-Match` (412) on products, from the version column (Level 4).
- Idempotency keys on `POST /orders` with replay, 422 for reuse, unique constraint (Level 3).
- Signed keyset cursors (`product/Cursor.java`), CORS policy with exposed headers (frontend level), problem details with trace ids (Level 6).

## 11. Interview checklist

1. `no-cache` vs `no-store` vs `max-age`. What should a response with personal data say?
2. How do ETags save bandwidth? How do they prevent lost updates? Strong vs weak.
3. How does an idempotency key make POST retries safe? What do you store, where, and for how long?
4. Same key, different body: what do you answer? Same key while the first is running?
5. Cursor vs offset pagination in an API. Why should cursors be opaque and signed?
6. What is CORS, what is a preflight, and why is CORS not a security boundary for your API?
7. Bearer tokens vs cookies: CSRF, XSS, revocation.
8. What should an error response contain?
