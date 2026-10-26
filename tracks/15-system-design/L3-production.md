# System design, L3: distributed systems in production

**Goal:** reason about the hard parts that decide whether a large system works: CAP and PACELC, replication and quorums, consensus and leader election, distributed transactions with sagas, multi-region design, resilience patterns at system level, and observability. Finish with full design interviews you can practise against the shop.

Prerequisite: L1, L2, and the L3 levels of tracks 02, 04, 05, 08, 11. Katas: `cd katas && ./mvnw test -Dgroups="design & L3"`. Drill: `java quiz/Quiz.java --track system-design --level 3`.

---

## 1. CAP, PACELC and what they really say

- **CAP**: during a network **partition** a replicated system must choose between **consistency** (refuse some requests) and **availability** (answer, possibly with stale or conflicting data). Without a partition you can have both. "CA" is not an option for a distributed system: partitions happen.
- **PACELC** adds the everyday trade-off: **else**, when there is no partition, choose between **latency** and **consistency** (synchronous replication across regions costs latency).
- Real systems are tunable: PostgreSQL with a single leader (consistent, the leader is a dependency), Cassandra/DynamoDB (tunable quorums), Spanner (consistent with special clocks and higher write latency).

---

## 2. Replication and quorums

| Model | How | Trade-offs |
|---|---|---|
| **Single leader** | one primary accepts writes, replicas follow (async or sync) | simple, consistent on the leader; failover is the hard part; async replicas lag |
| **Multi-leader** | several leaders (e.g. one per region) | local writes; **conflicts** must be resolved (last-write-wins loses data; CRDTs; app-level merge) |
| **Leaderless** | clients/coordinators write to and read from several replicas | no failover; quorums decide consistency; read repair and anti-entropy fix replicas |

**Quorums**: N replicas, a write waits for W acknowledgements, a read asks R replicas and takes the newest version. If **R + W > N**, every read set overlaps every write set, so a read sees the latest acknowledged write (N=3, W=2, R=2 tolerates one replica down for both). R=1/W=1 is fast and available but only eventually consistent. Versions (or vector clocks, to detect concurrent writes) decide what "newest" means; **read repair** writes the newest value back to stale replicas; sloppy quorums and hinted handoff keep writes available during failures at the cost of guarantees.

**Failover** for single-leader databases: detect the failure (timeouts: too short causes false failovers, too long causes downtime), promote a replica (the most up to date), redirect clients, and **fence** the old leader so it cannot accept writes if it comes back (split brain). Asynchronous replication means failover can **lose** the last acknowledged writes: RPO > 0.

> **Kata `QuorumStore`**: N/W/R quorums, versions, failed writes that are not rolled back, read repair, and a stale read when R + W ≤ N.

---

## 3. Distributed transactions and sagas

A business operation spanning services (order, payment, inventory, shipping) cannot use one ACID transaction:
- **Two-phase commit (2PC)**: a coordinator asks all participants to prepare, then to commit. Atomic, but blocking (a coordinator failure leaves participants holding locks), slow, and rarely supported across modern services and brokers.
- **Saga**: a sequence of local transactions, each publishing an event or called by an orchestrator; when a step fails, **compensating actions** undo the previous steps in reverse order (refund, release stock). Sagas give **eventual** consistency without isolation: other users can see intermediate states ("payment taken, order pending"), so design states explicitly and make every step and compensation **idempotent** (they will be retried).
- **Orchestration** (a coordinator service drives the steps: easy to follow and monitor) vs **choreography** (services react to each other's events: decoupled, but the flow is implicit and hard to see).
- Compensations can fail too: retry, then park and alert for manual repair. Some actions cannot be undone (an email was sent): order steps so irreversible ones come last.
- Implementation: a saga state table, the outbox pattern for reliable events, timeouts per step; frameworks such as Temporal or Camunda run durable workflows for you.

> **Kata `Saga`**: an orchestrator with reverse-order compensation, retries and a "needs attention" outcome.

---

## 4. Consensus, coordination and time

- **Consensus** (Raft, Paxos): a cluster agrees on a value or a log despite failures, as long as a **majority** is up (3 nodes tolerate 1 failure, 5 tolerate 2). Used for leader election, configuration and metadata: etcd (Kubernetes), ZooKeeper, Consul, KRaft in Kafka. Do not build it yourself; use these services.
- **Leader election and locks** with leases (a lock with an expiry): a paused process (GC, VM pause) can wake up believing it still holds the lease, so protected resources must check a **fencing token** (a monotonically increasing number) and reject older ones.
- **Time**: wall clocks drift and jump; never order events across machines by timestamps alone. Use logical clocks (Lamport, vector clocks), database sequences, or hybrid logical clocks; Snowflake ids are only roughly ordered.

---

## 5. Multi-region and resilience at system level

- Reasons: latency for global users, disaster recovery, data residency. Patterns: **active-passive** (one region serves, the other is a warm standby with replicated data; failover takes minutes; simpler) and **active-active** (both serve; writes either partitioned by user/region or multi-leader with conflict resolution).
- **RPO** (how much data you can lose) and **RTO** (how long recovery may take) drive the choice; backups must be **restored** regularly to count.
- **Cell-based architecture**: split users into independent cells (each a full copy of the stack) to limit the blast radius of failures and deployments.
- System-level resilience: timeouts, retries with budgets and jitter, circuit breakers, bulkheads, load shedding and graceful degradation (serve cached or partial results, turn off non-essential features), and **backpressure** (Web L3, Concurrency L3). Avoid retry storms and thundering herds after recovery.
- **Observability**: metrics (RED/USE, SLOs and error budgets), structured logs with correlation ids, distributed traces across services; alert on symptoms users feel, not every cause.

---

## 6. Practice interviews (use the shop as your reference)

For each: requirements, estimates, API, data model, high-level diagram, then two deep dives.

1. **An e-commerce checkout** (the shop at 100×): catalogue reads at 20 k/s with caching and a CDN; orders and inventory in PostgreSQL with transactions and conditional stock updates; payments via a saga with idempotency keys; events via the outbox to a broker; notifications asynchronous; read replicas with read-your-writes; flash sales (hot products, queueing, rate limiting).
2. **A ticket booking system** (concerts, seats): seat holds with expiry, no double booking (row locks or conditional updates on a seat table; one database partition per event), a virtual waiting room for on-sale spikes, payment saga, releasing holds on timeout.
3. **A distributed rate limiter** for a public API: per-key token buckets in Redis with atomic scripts, local pre-limiting to cut Redis round trips, fail-open vs fail-closed, multi-region quotas.
4. **A notification platform**: multi-channel, user preferences, dedupe, priorities, provider failover, retries and DLQs, delivery analytics.
5. **A metrics/logging pipeline**: high-volume ingestion through Kafka, partitioning by tenant, stream aggregation, hot/warm/cold storage tiers, retention, querying recent vs historical data.
6. **A collaborative document editor**: WebSocket sessions, operational transforms or CRDTs for concurrent edits, presence, snapshots plus an operation log, offline editing.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `QuorumStore` | leaderless replication, quorum overlap, versions, read repair |
| `Saga` | orchestrated sagas, compensation order, retries, partial failure handling |

```
cd katas && ./mvnw test -Dgroups="design & L3"
```

## 8. Interview talk track

"I start from requirements and numbers, then keep the core simple: stateless services, one strongly consistent database for money and stock, caches and replicas for reads, and asynchronous work through queues with idempotent consumers and an outbox for reliable events. When one database is not enough, I partition by the key the main queries use. Across services I use sagas with idempotent steps and compensations instead of distributed transactions. I know the CAP and PACELC trade-offs, size quorums so R + W > N where reads must be fresh, and let consensus systems like etcd handle leader election, with fencing tokens. For availability I remove single points of failure, define RPO/RTO, practise failover and restores, and protect the system with timeouts, retry budgets, circuit breakers and load shedding, all watched through SLO-based alerts, traces and structured logs."

**Questions you should be able to answer:**
1. Explain CAP precisely. Why is "CA" not a real choice? What does PACELC add?
2. Single-leader vs multi-leader vs leaderless replication.
3. N=3: which R and W give consistent reads? What happens when one node is down? Two?
4. What is split brain, and how does fencing prevent damage?
5. 2PC vs sagas. Orchestration vs choreography. What if a compensation fails?
6. Why do you need a majority for consensus? How many failures do 5 nodes tolerate?
7. Active-passive vs active-active multi-region. What are RPO and RTO?
8. Design the checkout of the shop for 100× its traffic.
