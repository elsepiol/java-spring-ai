# Track 05: Web, HTTP and REST

The protocol every backend speaks, and the patterns that keep HTTP APIs correct and available. Plain Java: Spring comes in tracks 06-07.

| Level | Lesson | Katas (`katas/src/main/java/dojo/web/`) | Examples |
|---|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): a request end to end, the wire format, status codes, safe and idempotent methods, headers, REST resource design | `HttpRequestParser`, `HttpSemantics` | TinyHttpServer (raw bytes, ETag/304) |
| L2 Practical | [L2-practical.md](L2-practical.md): caching, content negotiation, ETags and conditional requests, idempotency keys, cursors, CORS, tokens vs cookies, error contracts | `ConditionalRequests`, `IdempotencyStore`, `CursorCodec` | |
| L3 Production | [L3-production.md](L3-production.md): timeouts, retries and jitter, circuit breakers, bulkheads, rate limiting, load shedding, proxies, API evolution, RED metrics | `RetryPolicy`, `CircuitBreaker` (DSA: state machine), `SlidingWindowLimiter` (DSA) | RetryStorm |

```
java tracks/05-web-http/examples/TinyHttpServer.java
cd katas && ./mvnw test -Dgroups="web & L2"
java quiz/Quiz.java --track web-http
```

Question bank: [questions.md](questions.md) (60 questions).
