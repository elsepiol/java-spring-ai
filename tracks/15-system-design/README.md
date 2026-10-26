# Track 15: System design

How to design systems and talk about them: the method, estimates and building blocks; then classic designs, ids, caching, queues and consistency; then distributed systems in production (CAP/PACELC, replication and quorums, sagas, consensus, multi-region, resilience) and full practice interviews using the shop as the reference system.

| Level | Lesson | Katas (`katas/src/main/java/dojo/design/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): the interview method, back-of-the-envelope numbers, building blocks, scaling and partitioning, availability and latency | `l1/Estimates`, `l1/HashRing` (consistent hashing) |
| L2 Practical | [L2-practical.md](L2-practical.md): a URL shortener end to end, unique ids, caching strategies, queues and delivery guarantees, consistency in practice, classic designs | `l2/Shortener`, `l2/Snowflake` |
| L3 Production | [L3-production.md](L3-production.md): CAP and PACELC, replication and quorums, sagas vs 2PC, consensus and fencing, multi-region, RPO/RTO, six practice interviews | `l3/QuorumStore`, `l3/Saga` |

```
cd katas && ./mvnw test -Dgroups="design & L1"
java quiz/Quiz.java --track system-design
```

For timed, mixed practice across all tracks, see [interview/README.md](../../interview/README.md).

Question bank: [questions.md](questions.md) (60 questions).
