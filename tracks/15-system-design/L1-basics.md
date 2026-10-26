# System design, L1: the vocabulary and the method

**Goal:** run a design interview (or a real design review) with a method: clarify requirements, estimate the scale, sketch the components, then go deep. Know the building blocks (load balancers, caches, databases, queues, CDNs) and the vocabulary (latency, throughput, availability, scaling, partitioning).

Prerequisite: most L1-L2 levels of tracks 04, 05, 07, 11. Katas: `cd katas && ./mvnw test -Dgroups="design & L1"`. Drill: `java quiz/Quiz.java --track system-design --level 1`.

---

## 1. The method (45 minutes)

1. **Requirements (5 min)**: functional (what users do: "shorten a URL, redirect, see click stats") and **non-functional** (scale, latency, availability, consistency, durability, security, cost). Ask; write them down; agree on what is out of scope.
2. **Estimates (5 min)**: users, requests per second (average and peak), read/write ratio, data size per year, bandwidth. They decide whether one database is enough.
3. **API and data model (5 min)**: the main endpoints (`POST /urls`, `GET /{code}`), the entities and their access patterns.
4. **High-level design (10 min)**: boxes and arrows for the main flows: clients → CDN/load balancer → stateless services → cache → database, plus queues and workers for asynchronous work.
5. **Deep dives (15 min)**: the hardest parts: the hot path, the bottleneck from the estimates, consistency, failure handling. Discuss trade-offs and alternatives explicitly.
6. **Wrap-up (5 min)**: bottlenecks, monitoring, what you would do next with more scale or time.

Interviewers look for: structured thinking, asking before assuming, numbers that justify choices, trade-offs ("we accept X because Y"), and knowing where it breaks. There is no single right answer.

---

## 2. Back-of-the-envelope estimates

| Fact | Value |
|---|---|
| seconds per day | 86,400 (≈ 10^5) |
| 1 million requests/day | ≈ 12 per second |
| 100 million requests/day | ≈ 1,200/s average; ×2-3 at peak |
| main memory reference | 100 ns |
| SSD random read | ~100 µs |
| round trip within a data centre | ~0.5 ms |
| round trip across a continent / the Atlantic | ~40 ms / ~80-150 ms |
| one PostgreSQL instance | thousands to tens of thousands of simple queries per second |
| one app server (simple JSON API) | hundreds to a few thousand requests per second |

Storage: records/day × size × retention × replication. Example: 5 M orders/day × 2 KB × 5 years × 3 replicas ≈ **55 TB**: a single database server will not hold it comfortably, so the design needs partitioning or archiving.

Servers: peak QPS / (capacity per server × target utilisation, e.g. 60%) + 1 spare (N+1), at least 2.

> **Kata `Estimates`**: QPS, storage, fleet size, human-readable units.

---

## 3. Building blocks

| Block | What it gives you | Watch out for |
|---|---|---|
| **Load balancer** (L4/L7) | spreads traffic, health checks, TLS termination | a single point of failure unless redundant; sticky sessions hurt scaling |
| **Stateless app servers** | horizontal scaling: add more | state must live elsewhere (database, cache, object storage) |
| **Cache** (Redis, in-process) | fast reads, offloads the database | invalidation, stampedes, consistency, memory limits |
| **CDN** | static assets and cacheable responses close to users | cache keys, purging, private data |
| **Relational database** | transactions, joins, constraints | write scaling, big tables, migrations |
| **Read replicas** | read scaling | replication lag (read-your-writes) |
| **NoSQL** (key-value, document, wide-column) | simple access patterns at huge scale | no joins, weaker consistency, data modelled per query |
| **Object storage** (S3) | cheap durable files (images, exports, backups) | latency, eventual listing, access control |
| **Message queue / log** (RabbitMQ, Kafka, SQS) | asynchronous work, buffering spikes, decoupling, fan-out | at-least-once delivery, ordering, idempotent consumers, poison messages |
| **Search index** (Elasticsearch/OpenSearch) | full-text and faceted search | a second copy to keep in sync |

---

## 4. Scaling and partitioning

- **Vertical scaling** (a bigger machine) first: simplest; it has limits and one failure domain.
- **Horizontal scaling**: more instances behind a load balancer; needs stateless services.
- **Databases** scale reads with replicas and caches; writes with **partitioning (sharding)**: split data by a key (user id, tenant id) so each shard holds a part. The shard key decides everything: queries that include it are cheap; queries across shards are expensive; a skewed key creates hot shards.
- **How to map keys to shards**: ranges (good for range scans, risk of hot ranges), `hash(key) % n` (even, but changing n moves almost every key), or **consistent hashing**: nodes and keys on a ring, a key belongs to the next node clockwise, adding a node moves only ~1/n of the keys; **virtual nodes** even out the distribution. Used by caches (memcached clients), Cassandra, DynamoDB-like stores.
- **Rebalancing** is the hard part operationally: plan for it (more logical partitions than machines).

> **Kata `HashRing`**: a consistent-hash ring with virtual nodes, O(log n) lookups, minimal movement.

---

## 5. Availability and latency vocabulary

- **Availability**: 99.9% ("three nines") = 8.8 hours of downtime a year; 99.99% = 53 minutes. Each dependency in series multiplies: three 99.9% services in a chain give ~99.7%.
- **Redundancy** removes single points of failure (two of everything, multiple availability zones); **failover** must be tested.
- **Latency percentiles** (p50, p95, p99), not averages; **tail latency** compounds when one request fans out to many services.
- **Throughput** vs latency: queueing theory says latency explodes as utilisation approaches 100%: keep headroom.
- **SLI/SLO/SLA**: what you measure, the target you aim for, the contract you promise.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `Estimates` | the arithmetic of design interviews, overflow-safe storage maths |
| `HashRing` | consistent hashing, virtual nodes, TreeMap ceiling lookups |

```
cd katas && ./mvnw test -Dgroups="design & L1"
```

## 7. The shop as a case study

The shop project is a small but complete system: a stateless Spring Boot service behind a load balancer (Kubernetes Service/Ingress), PostgreSQL with a read replica, a cache for product reads, an outbox and worker for asynchronous notifications, rate limiting, a React SPA served by nginx with hashed assets, and observability (metrics, traces). Every L2/L3 topic in this track can be explained with it.

## 8. Interview checklist

1. Walk through your method for a design question.
2. How many requests per second is 100 million per day? At peak?
3. Estimate the storage for a service storing 5 million 2 KB records a day for 5 years with 3 replicas.
4. Vertical vs horizontal scaling.
5. Why must app servers be stateless to scale horizontally?
6. Range vs hash vs consistent-hash partitioning.
7. What do 99.9% and 99.99% availability mean in downtime? What happens to availability with dependencies in series?
8. Why look at p99 latency rather than the average?
