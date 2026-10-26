# Web, HTTP and REST, L1: the protocol under every framework

**Goal:** see HTTP as it is on the wire, know what methods, status codes and headers mean, and design simple resource URLs. Spring MVC (track 07) is a convenient layer over exactly this; when something goes wrong in production (a proxy, a cache, a 403 nobody understands) you debug at this level.

Katas: `cd katas && ./mvnw test -Dgroups="web & L1"`. Drill: `java quiz/Quiz.java --track web-http --level 1`.

---

## 1. One request, end to end

`curl -v https://shop.example.com/api/v1/products/7` does, in order:
1. **DNS**: resolve `shop.example.com` to an IP address.
2. **TCP**: open a connection to port 443 (three-way handshake).
3. **TLS**: negotiate encryption and verify the server's certificate (HTTPS = HTTP inside TLS).
4. **HTTP**: send the request text, read the response.
5. Keep the connection open for the next request (keep-alive), or close it.

Each step has its own failures and timeouts: "could not resolve host", "connection refused" (nothing listens), "connection timed out" (a firewall drops packets), certificate errors, then HTTP-level errors. Knowing which step failed is half of debugging.

---

## 2. What a request and a response look like

```
POST /api/v1/orders?coupon=SPRING26 HTTP/1.1          <- method, target (path + query), version
Host: shop.example.com                                <- headers: Name: value
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json
Content-Length: 41
Idempotency-Key: 6f1c2a9e-...
                                                      <- empty line: end of headers
{"items":[{"productId":7,"quantity":2}]}              <- body (Content-Length bytes)
```

```
HTTP/1.1 201 Created                                  <- version, status code, reason phrase
Location: /api/v1/orders/99
Content-Type: application/json
Content-Length: 58

{"id":99,"status":"PLACED","totalCents":2400,"items":[...]}
```

- Lines end with CRLF; header names are **case-insensitive**; `Host` is mandatory in HTTP/1.1 (one IP serves many sites).
- Query parameters are **percent-encoded**: space is `%20` (or `+` in query strings), `/` is `%2F`.
- The body's length comes from `Content-Length` (or chunked transfer encoding).
- **HTTP/2** sends the same semantics (methods, headers, status codes) as binary frames, many requests multiplexed on one connection; **HTTP/3** does it over QUIC (UDP). Your code does not change; performance and proxies do.

> **Kata `HttpRequestParser`**: parse a raw HTTP/1.1 request, with the rules above.

---

## 3. Status codes

| Range | Meaning | The ones you must know |
|---|---|---|
| 2xx success | it worked | **200** OK, **201** Created (+ `Location`), **202** Accepted (will be done later), **204** No Content |
| 3xx redirection | look elsewhere | **301**/**308** moved permanently, **302**/**307** temporarily, **304** Not Modified (your cached copy is fine) |
| 4xx client error | the request is wrong; retrying it unchanged will not help | **400** Bad Request, **401** Unauthorized (= not authenticated), **403** Forbidden (authenticated, not allowed), **404** Not Found, **405** Method Not Allowed, **409** Conflict, **412** Precondition Failed, **415** Unsupported Media Type, **422** Unprocessable Content, **429** Too Many Requests |
| 5xx server error | we failed; maybe retry later | **500** Internal Server Error (a bug), **502** Bad Gateway, **503** Service Unavailable, **504** Gateway Timeout |

Rules that matter in practice:
- **401 vs 403:** 401 = "who are you?" (missing or expired credentials; log in again). 403 = "I know who you are, and no".
- **404 vs 403 for other people's data:** returning 404 for "exists, but not yours" avoids revealing that it exists.
- **400 vs 422:** 400 for malformed requests (bad JSON); many APIs also use 400 for validation errors, others 422. Pick one and be consistent.
- **409:** the request conflicts with the current state (email already registered, order already shipped).
- **Never return 200 with `{"error": ...}`**: clients, proxies, monitoring and retry logic all read the status code.
- 5xx means "our fault": it pages someone. Client mistakes must not become 500s (validate input, map exceptions properly).

---

## 4. Methods: safe and idempotent

| Method | Meaning | Safe | Idempotent | Body |
|---|---|---|---|---|
| GET | read | yes | yes | no |
| HEAD | GET without the body | yes | yes | no |
| POST | create / process ("do something") | no | **no** | yes |
| PUT | replace the resource with this representation | no | yes | yes |
| PATCH | change part of it | no | not necessarily | yes |
| DELETE | remove | no | yes | usually no |
| OPTIONS | which methods/headers are allowed (CORS preflight) | yes | yes | no |

- **Safe:** the client does not request any state change. Crawlers, prefetchers and caches may call safe methods freely, so **never change data on GET** (`GET /orders/7/cancel` will eventually be called by a link preview bot).
- **Idempotent:** doing it twice has the same effect as once. `PUT /users/7 {name: "Ann"}` twice → same result. `DELETE` twice → still deleted (the second may return 404; the *state* is the same). `POST /orders` twice → **two orders**. This decides what may be retried automatically (L3) and why POSTs need idempotency keys (L2).

> **Kata `HttpSemantics`**: the right status for each outcome; safe and idempotent methods.

---

## 5. Headers you meet every day

| Header | Direction | Purpose |
|---|---|---|
| `Content-Type` | both | the body's format: `application/json`, `text/html; charset=utf-8`, `multipart/form-data` |
| `Accept` | request | formats the client wants (content negotiation); 406 if none possible |
| `Authorization` | request | credentials: `Bearer <token>`, `Basic <base64>` |
| `Cookie` / `Set-Cookie` | request / response | browser state; `HttpOnly`, `Secure`, `SameSite` attributes |
| `Location` | response | the new resource (201) or the redirect target (3xx) |
| `Cache-Control` | both | `no-store`, `no-cache`, `max-age=60`, `private`, `public` |
| `ETag` / `If-None-Match` / `If-Match` | both | versions for caching and safe updates (L2) |
| `Retry-After` | response | with 429/503: how long to wait |
| `X-Request-Id`, `traceparent` | both | correlating logs and traces across services |
| `Origin`, `Access-Control-*` | both | CORS (L2) |

---

## 6. REST resource design basics

REST = model your API as **resources** (nouns) identified by URLs, manipulated with the standard methods.

```
GET    /api/v1/products?category=kitchen&size=20   list (filter, paginate)
GET    /api/v1/products/7                          one
POST   /api/v1/products                            create -> 201 + Location: /api/v1/products/8
PUT    /api/v1/products/7                          replace
PATCH  /api/v1/products/7                          partial update
DELETE /api/v1/products/7                          delete -> 204
GET    /api/v1/customers/3/orders                  a sub-collection
POST   /api/v1/orders/99/cancel                    an action that does not fit CRUD: a POST to a sub-resource
```

- Plural nouns, no verbs in paths (`/getProducts` is RPC, not REST); lower-case, hyphens.
- Filtering, sorting and pagination in the query string.
- Version the API (`/api/v1`) so you can evolve it without breaking clients (L3).
- JSON bodies with consistent naming (`camelCase`), ISO-8601 timestamps (`2026-10-26T09:00:00Z`), money as integer minor units or strings, never floats.
- Errors in a standard shape: RFC 9457 **problem details** (`{"type", "title", "status", "detail", "instance"}`), which Spring produces natively (track 07).

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `HttpRequestParser` | request line, headers, percent-decoding, Content-Length, rejecting malformed input |
| `HttpSemantics` | status codes, safe and idempotent methods |

```
cd katas && ./mvnw test -Dgroups="web & L1"
```

Try by hand: `curl -v http://localhost:8080/api/v1/products` against the shop project, and read every line `curl` prints.

## 8. Seen in the shop project

- 201 + `Location` on create, 204 on delete, 404/409/412/429 mapped from domain exceptions: `error/ApiExceptionHandler.java`.
- `POST /api/v1/orders/{id}/cancel` as an action sub-resource.
- `/api/v1` versioned paths; problem details with a `traceId`.

## 9. Interview checklist

1. What happens between typing a URL and seeing a JSON response (DNS, TCP, TLS, HTTP)?
2. What does a raw HTTP request look like? Why is the Host header required?
3. 401 vs 403. 400 vs 422. When 409? When 412?
4. Safe vs idempotent. Is DELETE idempotent if the second call returns 404?
5. Why must GET never change state?
6. PUT vs PATCH vs POST.
7. Design the URLs for products, orders, and cancelling an order.
8. Why is "200 with an error body" bad?
