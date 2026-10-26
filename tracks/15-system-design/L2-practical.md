# System design, L2: classic designs, ids, caching, queues, consistency in practice

**Goal:** design the classic interview systems end to end (URL shortener, rate limiter, news feed, chat, notifications), generate ids at scale, place caches and queues correctly, and reason about consistency, idempotency and delivery guarantees.

Prerequisite: L1, Web L2-L3, Concurrency L3, SQL L3. Katas: `cd katas && ./mvnw test -Dgroups="design & L2"`. Drill: `java quiz/Quiz.java --track system-design --level 2`.

---

## 1. Walkthrough: a URL shortener

**Requirements**: create short links (optionally custom aliases), redirect fast, links never expire unless deleted, analytics (click counts). Read-heavy: 100:1. Scale: 100 M new links/month, 10 B redirects/month.

**Estimates**: writes ≈ 40/s; reads ≈ 4,000/s average, 10-20 k/s peak. 100 M × 12 months × 5 years × 500 bytes ≈ 3 TB.

**Codes**: Base62 (`[0-9a-zA-Z]`) of a unique id: 7 characters give 62^7 ≈ 3.5 trillion codes. Id options: a database sequence (simple; one writer is fine at 40/s), pre-allocated ranges per server, Snowflake ids, or random codes with a uniqueness check (hides volume, needs retries on collisions). Hashing the URL (MD5 prefix) gives collisions to resolve and the same code for the same URL (which may or may not be wanted).

**Design**: `POST /urls` → app → database (`code` primary key, unique index on the URL if creation is idempotent); `GET /{code}` → **cache** (Redis, LRU, hit rate > 90% because traffic is skewed) → database on a miss → **301** (cacheable by browsers: fewer hits but lost analytics) or **302** (every click reaches you). Click analytics go **asynchronously** to a queue → aggregator → analytics store, never on the redirect path.

**Deep dives**: hot links (cache, CDN), custom aliases colliding with generated codes, abuse (validate schemes, malware lists, rate limits per user), deletion and expiry, multi-region reads.

> **Kata `Shortener`**: Base62, idempotent creation, aliases that never collide with generated codes, scheme validation.

---

## 2. Unique ids at scale

| Approach | Pros | Cons |
|---|---|---|
| database sequence / identity | simple, compact, ordered | one writer per sequence; reveals volume; needs the database for every id |
| UUID v4 (random) | no coordination, anywhere | 128 bits, random order hurts B-tree index locality |
| UUID v7 / ULID (time-ordered) | no coordination, roughly sorted, index-friendly | 128 bits |
| Snowflake (64-bit: time, worker, sequence) | compact, sorted by time, ~4 M ids/s per worker | worker ids must be unique; clock skew handling |
| ranges handed out by a central service | compact, fast | the allocator is a dependency |

Clocks: **wall clocks jump** (NTP corrections, leap seconds, VM pauses). Snowflake must never issue an id from a time it already passed; it waits for small backwards jumps and refuses large ones.

> **Kata `Snowflake`**: bit packing, per-millisecond sequences, sequence exhaustion, clock moving backwards.

---

## 3. Caching in system design

- **Cache-aside** (the app reads the cache, falls back to the database and fills the cache) is the default; **write-through** (write to both), **write-behind** (write to the cache, flush later: risky), **read-through** (the cache loads itself).
- **Invalidation**: TTLs (bounded staleness), explicit deletes after writes (delete, don't update, to avoid races), versioned keys, change-data-capture events.
- **Stampedes**: many misses for one hot key at once: single-flight per key, early probabilistic refresh, stale-while-revalidate (Concurrency L3).
- **Hot keys** and **big keys** break even distribution; replicate hot keys, split big ones.
- What to cache: rendered pages at the CDN, API responses (HTTP caching with ETags, Web L2), computed objects in Redis, reference data in process.

---

## 4. Queues, streams and delivery guarantees

- **Queue** (RabbitMQ, SQS): each message is processed by one consumer; good for jobs. **Log/stream** (Kafka): messages are retained, consumers track offsets, many consumer groups can replay; ordering per partition.
- Delivery: **at-most-once** (may lose), **at-least-once** (may duplicate: the practical default), **exactly-once** only within a system (Kafka transactions) or effectively via **idempotent consumers** (dedupe by message id, upserts, conditional updates).
- **Ordering**: only within a partition; choose the partition key (order id, user id) so related events stay ordered.
- **Poison messages**: retry with backoff, then a **dead-letter queue** and an alert.
- **Backpressure**: queues absorb spikes, but a queue that grows forever is an outage in slow motion: monitor lag, scale consumers, shed load.
- Getting events out of a database transaction reliably: the **transactional outbox** (JPA track L3), or change data capture (Debezium).

---

## 5. Consistency in practice

- **Strong consistency**: every read sees the latest write (a single leader database, synchronous replication). **Eventual consistency**: replicas converge; reads may be stale for a while.
- Useful middle grounds: **read-your-writes** (route a user's reads to the leader for a few seconds after they write, as the shop does with read replicas), **monotonic reads** (stick a user to one replica), causal consistency.
- **Idempotency** everywhere a retry can happen: idempotency keys on APIs (Web L2), idempotent consumers, upserts.
- Prefer designs where a stale read is harmless (product catalog) and keep strong consistency where money or stock is involved (orders, payments, inventory: one database, transactions, constraints).

---

## 6. More classic designs, in one paragraph each

- **Rate limiter**: token bucket or sliding window per key (user, IP, API key) in Redis with atomic Lua scripts or in-process with a shared quota; return 429 with `Retry-After`; decide fail-open vs fail-closed when Redis is down (Web L3, the shop's Level 5).
- **News feed**: fan-out on write (push post ids into followers' feed lists: fast reads, expensive for celebrities) vs fan-out on read (merge followees' recent posts at read time: cheap writes, slower reads); hybrid: push for normal users, pull for celebrities; feeds cached in Redis lists; ranking as a separate service.
- **Chat**: persistent connections (WebSocket) to gateway servers, a presence service, messages written to a partitioned store (by conversation id) and delivered through a pub/sub layer to the gateways holding the recipients; ordering per conversation (sequence numbers), offline delivery via push notifications, read receipts as events.
- **Notification service**: an API accepting notification requests, a queue per channel (email, SMS, push), workers calling providers with retries and rate limits, deduplication, user preferences and quiet hours, templates, and delivery tracking.
- **File upload / image service**: clients upload directly to object storage with pre-signed URLs, an event triggers processing (thumbnails) by workers, metadata in a database, delivery through a CDN.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `Shortener` | Base62, idempotency, alias namespaces, input validation |
| `Snowflake` | distributed ids, bit layouts, clocks |

```
cd katas && ./mvnw test -Dgroups="design & L2"
```

## 8. Interview checklist

1. Design a URL shortener: code generation, storage, caching, redirects (301 vs 302), analytics.
2. Compare database sequences, UUID v4, UUID v7 and Snowflake ids.
3. Why is a random UUID a poor primary key for a B-tree index?
4. Cache-aside vs write-through. How do you invalidate? What is a cache stampede?
5. Queue vs log. At-least-once vs exactly-once. How do you make a consumer idempotent?
6. How do you keep events ordered? What is a dead-letter queue?
7. Strong vs eventual consistency; what is read-your-writes and how do you provide it with replicas?
8. Fan-out on write vs fan-out on read for a news feed.
