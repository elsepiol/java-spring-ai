# Track 14: Git, Linux, networking

The ground every full-stack developer stands on: Git as a graph of snapshots, the Linux shell, permissions and processes, and the network under every request; then team workflows, log slicing and connection debugging; then bisecting regressions, production debugging, TLS and connections under load.

| Level | Lesson | Katas (`katas/src/main/java/dojo/ops/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): Git workflow and object model, the shell, permissions, processes and signals, IP/CIDR, ports, DNS, TCP/UDP | `l1/GitObjects`, `l1/Permissions`, `l1/Cidr` |
| L2 Practical | [L2-practical.md](L2-practical.md): merge vs rebase on the commit DAG, recovering with reflog, team workflows, text processing for incidents, debugging connections | `l2/CommitGraph`, `l2/AccessLogStats` |
| L3 Production | [L3-production.md](L3-production.md): git bisect, debugging live JVMs and Linux, TLS and certificates, timeouts, keep-alive, TIME_WAIT, load balancers, HTTP/2 and 3 | `l3/Bisect`, `l3/CertificateCheck` |

```
cd katas && ./mvnw test -Dgroups="ops & L1"
java quiz/Quiz.java --track git-linux-networking
```

The katas tie the tools to data structures: Git ids are hashes, history is a DAG (merge base = lowest common ancestor), bisect is binary search, CIDR is bit masking.

Question bank: [questions.md](questions.md) (60 questions).
