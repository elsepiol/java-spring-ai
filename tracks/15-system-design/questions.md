# System design: question bank

Drill with `java quiz/Quiz.java --track system-design`.

## L1

### SD-L1-001 · recall
What are the steps of a system design interview?

<details><summary>Answer</summary>

Clarify functional and non-functional requirements and scope; estimate scale (QPS, storage, bandwidth); define the API and data model; draw the high-level design for the main flows; deep-dive into the hardest parts (bottlenecks, consistency, failures) discussing trade-offs; wrap up with bottlenecks, monitoring and next steps.

</details>

### SD-L1-002 · recall
Functional vs non-functional requirements? Give examples of non-functional ones.

<details><summary>Answer</summary>

Functional: what the system does (create a short URL, redirect, show stats). Non-functional: how well: scale (users, QPS), latency targets, availability, consistency, durability, security and privacy, cost, compliance. Non-functional requirements drive most architecture decisions.

</details>

### SD-L1-003 · predict
How many requests per second are 100 million requests per day, on average and at a 3× peak?

<details><summary>Answer</summary>

100,000,000 / 86,400 ≈ 1,157/s on average; ≈ 3,500/s at a 3× peak. Rule of thumb: 1 million per day ≈ 12 per second.

</details>

### SD-L1-004 · predict
A service stores 5 million new 2 KB records per day, keeps them 5 years, with 3 replicas. Roughly how much storage?

<details><summary>Answer</summary>

5 M × 2 KB = 10 GB/day; × 1,825 days ≈ 18 TB; × 3 replicas ≈ 55 TB. Too much for one comfortable database node: plan partitioning, archiving or cheaper storage tiers.

</details>

### SD-L1-005 · recall
Roughly how long do these take: a memory access, an SSD read, a round trip in a data centre, a transatlantic round trip?

<details><summary>Answer</summary>

About 100 ns, 100 µs, 0.5 ms and 80-150 ms. The orders of magnitude matter: network round trips dominate, so batching, caching and keeping chatty calls inside one data centre matter more than micro-optimising code.

</details>

### SD-L1-006 · recall
Vertical vs horizontal scaling?

<details><summary>Answer</summary>

Vertical: a bigger machine (more CPU, RAM, faster disks): no code changes, but hard limits, cost grows steeply, and one machine is one failure domain. Horizontal: more machines behind a load balancer: near-linear growth and redundancy, but requires stateless services or partitioned data and adds coordination complexity.

</details>

### SD-L1-007 · why
Why must application servers be stateless to scale horizontally?

<details><summary>Answer</summary>

Any instance must be able to serve any request, so the load balancer can spread traffic, instances can be added or removed, and a crash loses nothing. State (sessions, uploads, caches that must be shared) moves to databases, Redis or object storage, or into tokens carried by the client.

</details>

### SD-L1-008 · recall
What does a load balancer do?

<details><summary>Answer</summary>

Distributes requests across healthy backends (round robin, least connections, hashing), runs health checks and removes failing instances, terminates TLS, and at L7 routes by host or path, adds headers and can retry. It must itself be redundant.

</details>

### SD-L1-009 · recall
What is a CDN, and what does it help with?

<details><summary>Answer</summary>

A content delivery network: edge servers close to users that cache static assets (JS, CSS, images, video) and cacheable API responses. It cuts latency and offloads the origin; also absorbs traffic spikes and some DDoS. Needs correct cache headers, cache keys and purging.

</details>

### SD-L1-010 · recall
What are read replicas for, and what is their main caveat?

<details><summary>Answer</summary>

They scale reads (and provide failover candidates) by serving queries from copies of the primary. Replication is usually asynchronous, so replicas lag: a user may not see their own write. Route reads that must be fresh (read-your-writes) to the primary.

</details>

### SD-L1-011 · recall
What is sharding (partitioning), and what decides whether it works well?

<details><summary>Answer</summary>

Splitting data across several databases by a shard key so each holds a subset and handles part of the load. The shard key decides everything: queries including it go to one shard; others scatter to all shards; a skewed key (a celebrity, a big tenant) creates hot shards; changing it later is very expensive.

</details>

### SD-L1-012 · why
Why is `hash(key) % n` a bad way to assign keys to cache nodes when nodes come and go?

<details><summary>Answer</summary>

Changing n changes the result for almost every key (about (n-1)/n of them move), so adding one cache node invalidates nearly the whole cache and floods the database. Consistent hashing moves only about 1/n of the keys.

</details>

### SD-L1-013 · recall
How does consistent hashing work, and what are virtual nodes for?

<details><summary>Answer</summary>

Nodes and keys are hashed onto a ring; a key belongs to the first node clockwise from its position. Adding or removing a node only affects the keys between it and its predecessor. Each physical node is placed many times (virtual nodes) so the load is spread evenly and a leaving node's keys are shared among many others.

</details>

### SD-L1-014 · predict
How much downtime per year do 99.9% and 99.99% availability allow?

<details><summary>Answer</summary>

99.9%: about 8.8 hours per year (43 minutes per month). 99.99%: about 53 minutes per year (4.3 minutes per month).

</details>

### SD-L1-015 · predict
A request path calls three services in series, each 99.9% available. What is the availability of the path?

<details><summary>Answer</summary>

0.999³ ≈ 99.7%: dependencies in series multiply their unavailability. Reduce the chain, make calls optional (degrade gracefully), add redundancy, or cache.

</details>

### SD-L1-016 · recall
SLI vs SLO vs SLA?

<details><summary>Answer</summary>

SLI: a measured indicator (share of requests under 300 ms, error rate). SLO: an internal target for it (99.9% of requests under 300 ms over 30 days), with an error budget. SLA: a contractual promise to customers with consequences, usually looser than the SLO.

</details>

### SD-L1-017 · why
Why does latency grow sharply as a server approaches 100% utilisation?

<details><summary>Answer</summary>

Requests arrive randomly; as utilisation approaches 1 queues form and waiting time grows non-linearly (in simple queueing models proportional to 1/(1 - utilisation)). At 90% busy, the wait is roughly ten times that at 50%. Keep headroom (target 50-70%) and autoscale before saturation.

</details>

### SD-L1-018 · recall
Relational database vs key-value or document store: when would you choose each?

<details><summary>Answer</summary>

Relational: structured data with relationships, transactions, constraints, ad-hoc queries (orders, payments, accounts): the default. Key-value/document/wide-column: very high scale with simple, known access patterns (sessions, carts, events, user profiles by id), flexible schemas, and easier horizontal scaling, at the cost of joins and often consistency.

</details>

### SD-L1-019 · interview
"Design a system to handle 10× the current traffic of our Spring Boot app. Where do you start?"

<details><summary>Answer</summary>

Measure where the bottleneck is (CPU, database, external calls) and estimate the new load. Make the app tier stateless and scale it horizontally behind the load balancer; take read load off the database with caching and read replicas; move slow or non-critical work to queues; optimise the hot queries and indexes; add rate limiting and backpressure; plan partitioning if writes outgrow one database; load test and add autoscaling and monitoring with SLOs.

</details>

### SD-L1-020 · dsa
Which data structure gives O(log n) lookups on a consistent-hash ring, and how is wrap-around handled?

<details><summary>Answer</summary>

A sorted map from ring position to node (Java TreeMap, a balanced BST): `ceilingEntry(hash(key))` finds the first position ≥ the key's hash in O(log n); if there is none, wrap around to `firstEntry()`. A sorted array with binary search works too when the ring rarely changes.

</details>

## L2

### SD-L2-001 · design
Design the core of a URL shortener: how are codes generated?

<details><summary>Answer</summary>

Base62-encode a unique numeric id (7 characters ≈ 3.5 trillion codes): from a database sequence, pre-allocated ranges per app server, or Snowflake ids. Alternatives: random codes with a uniqueness check and retry (hides volume) or a hash of the URL (deterministic, needs collision handling). Custom aliases share the namespace and must not collide with generated codes.

</details>

### SD-L2-002 · recall
301 vs 302 redirects for a URL shortener?

<details><summary>Answer</summary>

301 (permanent) is cached by browsers, so repeat visits skip your servers: less load, but you lose click analytics and cannot change the target. 302/307 (temporary) sends every click through you: accurate analytics and editable targets at the cost of load. Most shorteners with analytics use 302.

</details>

### SD-L2-003 · design
Where do click analytics go in a URL shortener, and why not in the redirect path?

<details><summary>Answer</summary>

The redirect must be fast and highly available, so it only reads (cache, then database) and emits a click event asynchronously (to a queue or log) without waiting. Consumers aggregate counts into an analytics store. A slow or failing analytics database then never slows redirects.

</details>

### SD-L2-004 · recall
Compare database sequences, UUID v4, UUID v7 and Snowflake ids.

<details><summary>Answer</summary>

Sequences: compact, ordered, simple, but need the database and a single writer, and reveal volume. UUID v4: generated anywhere without coordination, 128 bits, random, so B-tree inserts scatter. UUID v7: time-ordered UUIDs, index-friendly, no coordination. Snowflake: 64-bit time + worker + sequence, compact and roughly ordered, needs unique worker ids and careful clock handling.

</details>

### SD-L2-005 · why
Why are random UUIDs poor primary keys for B-tree indexes in large tables?

<details><summary>Answer</summary>

Each insert lands on a random leaf page, so the working set is the whole index: pages are read and split everywhere, caches thrash, write amplification grows and the index fragments. Time-ordered keys (sequences, UUID v7, Snowflake) append near the right edge of the index.

</details>

### SD-L2-006 · recall
How does a Snowflake id generator handle more than 4096 ids per millisecond and a clock that jumps backwards?

<details><summary>Answer</summary>

The 12-bit sequence allows 4096 ids per millisecond per worker; when it runs out, the generator waits for the next millisecond. If the clock moves backwards, it must not reuse timestamps: wait for small jumps to pass, and refuse (fail) on large ones rather than risk duplicate ids.

</details>

### SD-L2-007 · recall
Cache-aside vs write-through vs write-behind?

<details><summary>Answer</summary>

Cache-aside: the app reads the cache, on a miss loads from the database and fills the cache; writes go to the database and invalidate the cache. Write-through: writes update cache and database together synchronously. Write-behind: writes go to the cache and are flushed to the database later: fast but risks data loss. Cache-aside is the common default.

</details>

### SD-L2-008 · why
After updating the database, why delete the cache entry rather than writing the new value into it?

<details><summary>Answer</summary>

Two concurrent writers can update the database in one order and the cache in the other, leaving the cache permanently stale. Deleting makes the next read reload the current value. Races are still possible (a reader caching an old value just before the delete), so add TTLs as a safety net.

</details>

### SD-L2-009 · recall
What is a cache stampede, and how do you prevent it?

<details><summary>Answer</summary>

A popular key expires and many requests miss at once, all hitting the database together. Prevent with single-flight loading (one request recomputes, the others wait or get the stale value), early probabilistic refresh before expiry, stale-while-revalidate, and jittered TTLs so keys do not expire together.

</details>

### SD-L2-010 · recall
Queue vs log (stream)?

<details><summary>Answer</summary>

A queue (RabbitMQ, SQS) hands each message to one consumer and deletes it after acknowledgement: work distribution. A log (Kafka, Kinesis) appends messages and retains them; consumers track their own offsets, several consumer groups read independently, and messages can be replayed; ordering holds within a partition.

</details>

### SD-L2-011 · recall
At-most-once, at-least-once, exactly-once delivery?

<details><summary>Answer</summary>

At-most-once: never redelivered, may be lost. At-least-once: retried until acknowledged, may be duplicated: the practical default. Exactly-once: only within specific systems (Kafka transactions); end to end you get "effectively once" with at-least-once delivery plus idempotent processing.

</details>

### SD-L2-012 · recall
How do you make a message consumer idempotent?

<details><summary>Answer</summary>

Deduplicate by a unique message or event id (a processed-ids table written in the same transaction as the effect), use natural idempotency (upserts, set-to-value instead of increment), or conditional updates with versions. Then redelivery has no extra effect.

</details>

### SD-L2-013 · recall
How do you keep related events in order in Kafka?

<details><summary>Answer</summary>

Use a partition key (order id, user id) so all events of one entity go to the same partition, which is consumed in order by one consumer of the group. There is no ordering across partitions; retries must not reorder (one in-flight message per key, or idempotent producers).

</details>

### SD-L2-014 · recall
What is a dead-letter queue?

<details><summary>Answer</summary>

Where messages go after failing processing a number of times (poison messages: bad data, bugs), so they stop blocking or looping in the main queue. Monitor it, alert, inspect and replay after fixing the cause.

</details>

### SD-L2-015 · recall
Strong vs eventual consistency? Name two in-between guarantees.

<details><summary>Answer</summary>

Strong (linearizable): every read sees the latest completed write, as if there were one copy. Eventual: replicas converge if writes stop, but reads may be stale meanwhile. In between: read-your-writes (users see their own writes), monotonic reads (no going back in time), causal consistency (causes are seen before effects).

</details>

### SD-L2-016 · design
How do you provide read-your-writes with asynchronous read replicas?

<details><summary>Answer</summary>

Route a user's reads to the primary for a short time after they write (track the last write time in the session or token), or for the specific entities they changed; or wait until the replica has replayed the write's log position (LSN) before reading from it. The shop reads a just-placed order from the primary for this reason.

</details>

### SD-L2-017 · design
News feed: fan-out on write vs fan-out on read?

<details><summary>Answer</summary>

Fan-out on write: when someone posts, push the post id into every follower's feed list: reads are cheap, but a celebrity with 50 M followers creates huge write bursts. Fan-out on read: build the feed at read time by merging followees' recent posts: cheap writes, slower reads. Hybrid: push for normal accounts, pull for celebrities, cache the result.

</details>

### SD-L2-018 · design
Design a distributed rate limiter for an API with 20 instances.

<details><summary>Answer</summary>

A shared counter per key (API key, user, IP) in Redis with an atomic token-bucket or sliding-window script (Lua) and TTLs; optionally local pre-limiting to reduce Redis calls; return 429 with Retry-After and rate-limit headers; decide fail-open or fail-closed when Redis is unavailable; limits per plan configured centrally; monitor rejections.

</details>

### SD-L2-019 · interview
"Design a notification service that sends email, SMS and push notifications."

<details><summary>Answer</summary>

An API (and event consumers) accepting notification requests with an idempotency key; a preferences service (channels, opt-outs, quiet hours); templating; one queue per channel and priority; workers calling providers with rate limits, retries with backoff and failover to a secondary provider; a DLQ; deduplication; delivery status tracking via provider callbacks; metrics per channel; and never sending from inside the business transaction (outbox).

</details>

### SD-L2-020 · dsa
How many distinct 7-character Base62 codes are there, and how long would they last at 1,000 new links per second?

<details><summary>Answer</summary>

62^7 ≈ 3.52 × 10^12. At 1,000 per second (8.64 × 10^7 per day ≈ 3.15 × 10^10 per year) they last about 110 years. With random codes, collisions follow the birthday paradox: expect the first collision around sqrt(62^7) ≈ 1.9 million codes, so random generation needs a uniqueness check.

</details>

## L3

### SD-L3-001 · recall
State the CAP theorem precisely.

<details><summary>Answer</summary>

In a replicated system, when a network partition separates nodes, you must choose between consistency (every read sees the latest write, so some requests must be refused or wait) and availability (every non-failed node answers, possibly with stale data). It says nothing about the system without partitions, and "consistency" means linearizability, not ACID consistency.

</details>

### SD-L3-002 · recall
What does PACELC add to CAP?

<details><summary>Answer</summary>

If there is a Partition, choose between Availability and Consistency; Else (normal operation) choose between Latency and Consistency: synchronous replication (especially across regions) adds latency to every write. It describes the everyday trade-off CAP ignores.

</details>

### SD-L3-003 · recall
Single-leader, multi-leader and leaderless replication: one strength and one weakness each.

<details><summary>Answer</summary>

Single-leader: simple and consistent on the leader; failover is tricky and async replicas lag. Multi-leader: local writes in several regions; concurrent writes conflict and need resolution. Leaderless: no failover needed and tunable quorums; weaker guarantees and repair mechanisms (read repair, anti-entropy) are required.

</details>

### SD-L3-004 · predict
With N = 3 replicas, which W and R values guarantee that a read sees the latest successful write? How many replica failures can reads and writes tolerate then?

<details><summary>Answer</summary>

Any W + R > 3, e.g. W = 2, R = 2: every read set overlaps every write set. Both reads and writes then tolerate one failed replica. W = 3, R = 1 gives fast reads but writes fail if any replica is down; W = 1, R = 3 the reverse.

</details>

### SD-L3-005 · recall
What is read repair?

<details><summary>Answer</summary>

When a quorum read finds replicas with older versions, the coordinator writes the newest version back to them, so frequently read data converges quickly. Rarely read data needs background anti-entropy (Merkle-tree comparisons) instead.

</details>

### SD-L3-006 · recall
What is split brain, and how do fencing tokens prevent damage?

<details><summary>Answer</summary>

Two nodes both believe they are the leader (after a network partition or a paused process waking up) and both accept writes, corrupting data. A fencing token is a monotonically increasing number issued with each new lease or leadership term; storage and downstream systems reject requests carrying an older token, so the stale leader's writes are refused.

</details>

### SD-L3-007 · why
Why does asynchronous replication make database failover lose data?

<details><summary>Answer</summary>

The primary acknowledges commits before replicas have them; if it fails, the promoted replica lacks the last transactions, which are lost (RPO > 0) or conflict if the old primary returns. Synchronous replication to at least one replica avoids it at the cost of write latency and availability.

</details>

### SD-L3-008 · recall
Two-phase commit vs saga?

<details><summary>Answer</summary>

2PC: a coordinator asks participants to prepare (lock and vote), then commit: atomic and isolated, but blocking (participants wait with locks if the coordinator dies), slow and poorly supported across services and brokers. Saga: a sequence of local transactions with compensating actions on failure: available and loosely coupled, but only eventually consistent and without isolation (intermediate states are visible).

</details>

### SD-L3-009 · recall
Orchestrated vs choreographed sagas?

<details><summary>Answer</summary>

Orchestration: a coordinator (a service or a workflow engine such as Temporal) tells each participant what to do and handles failures; the flow is explicit and observable, the coordinator is extra infrastructure. Choreography: each service reacts to events and emits its own; decoupled, but the overall flow is implicit, hard to monitor and prone to cyclic dependencies.

</details>

### SD-L3-010 · design
What must be true of saga steps and compensations?

<details><summary>Answer</summary>

Each step is a local transaction that publishes its outcome reliably (outbox); steps and compensations are idempotent because they will be retried; compensations are semantic undos (refund, release stock), not rollbacks; irreversible actions (sending email, shipping) come last; intermediate states are explicit in the domain model (PENDING_PAYMENT); and a compensation that keeps failing is parked and alerted on.

</details>

### SD-L3-011 · why
Why do consensus algorithms need a majority, and how many failures do 3 and 5 nodes tolerate?

<details><summary>Answer</summary>

Any two majorities overlap in at least one node, so two conflicting decisions (two leaders in the same term) cannot both be accepted. 3 nodes tolerate 1 failure, 5 tolerate 2 (2f + 1 nodes tolerate f). Even numbers add cost without extra tolerance.

</details>

### SD-L3-012 · recall
What are Raft and etcd used for in practice?

<details><summary>Answer</summary>

Raft is a consensus algorithm (leader election plus a replicated log). etcd (Kubernetes' store), Consul and KRaft (Kafka's controller) use it to keep strongly consistent metadata, configuration, leases and leader election for other systems. Applications use these services rather than implementing consensus.

</details>

### SD-L3-013 · why
Why can't you order events across machines by wall-clock timestamps?

<details><summary>Answer</summary>

Clocks drift and are corrected by NTP (jumps, even backwards), leap seconds and VM pauses happen, so a later event can carry an earlier timestamp and last-write-wins silently drops data. Use a single sequencer (database), logical clocks (Lamport, vector clocks), hybrid logical clocks, or special infrastructure (Spanner's TrueTime).

</details>

### SD-L3-014 · recall
Active-passive vs active-active multi-region?

<details><summary>Answer</summary>

Active-passive: one region serves traffic; another holds replicated data and standby capacity and takes over on failure (minutes of RTO, possible data loss with async replication; simpler). Active-active: all regions serve traffic; writes are partitioned by region or user, or replicated multi-leader with conflict resolution; lower latency and faster failover, much more complexity.

</details>

### SD-L3-015 · recall
What are RPO and RTO?

<details><summary>Answer</summary>

Recovery Point Objective: the maximum acceptable data loss, measured in time (RPO 5 min = you may lose the last 5 minutes of writes). Recovery Time Objective: the maximum acceptable time to restore service. They drive backup frequency, replication mode and failover automation.

</details>

### SD-L3-016 · recall
What is load shedding, and how does it differ from rate limiting?

<details><summary>Answer</summary>

Rate limiting enforces per-client quotas regardless of server health. Load shedding rejects (or degrades) work when the server itself is overloaded, preferring to serve some requests well rather than all badly: reject low-priority traffic first, fail fast with 503, keep queues short.

</details>

### SD-L3-017 · recall
What is a cell-based architecture?

<details><summary>Answer</summary>

The system is split into independent cells, each a complete copy of the stack serving a subset of users or tenants, with a thin routing layer. A failure or bad deployment affects only one cell (small blast radius), cells scale by adding more, and capacity is tested per cell.

</details>

### SD-L3-018 · design
Design a ticket booking system that must never sell the same seat twice.

<details><summary>Answer</summary>

Seats as rows (event_id, seat_id, status, hold_expires_at) in a strongly consistent database partitioned by event; holding a seat is a conditional update (`... WHERE status = 'AVAILABLE'` or SELECT ... FOR UPDATE SKIP LOCKED for best-available) with an expiry; payment within the hold through a saga with idempotency keys; expired holds released by a job; a virtual waiting room and rate limiting for on-sale spikes; caching only for the seat map display, never for the booking decision.

</details>

### SD-L3-019 · interview
"Design the checkout of an e-commerce site for Black Friday traffic."

<details><summary>Answer</summary>

Requirements and estimates first (peak orders/s, catalogue reads/s). Catalogue: cached at CDN and Redis, read replicas. Cart: in Redis or the database keyed by user. Checkout: stateless order service; inventory reserved with conditional updates in one transactional database (partitioned if needed), order + outbox event in one transaction; payment via provider with idempotency keys in a saga with compensation (release stock); notifications asynchronous. Protection: rate limiting, queue-based waiting room, load shedding of non-essential features, autoscaling, timeouts and circuit breakers to the payment provider. Observability with SLOs; load tests beforehand.

</details>

### SD-L3-020 · dsa
In a leaderless store, why is choosing the "newest" value by a per-key version counter insufficient when two clients write concurrently, and what structure detects it?

<details><summary>Answer</summary>

Two coordinators can read version 5 and both write version 6 with different values: a counter cannot tell concurrent writes from sequential ones, so one is silently lost. Vector clocks (a counter per writer node) give a partial order: if neither vector dominates the other, the writes are concurrent and both siblings are kept for the application (or a CRDT) to merge.

</details>
