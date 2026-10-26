# Git, Linux, networking, L3: debugging production, TLS, the network under load

**Goal:** debug systematically in production (bisect, logs, JVM and OS tools), understand TLS and certificates well enough to fix handshake failures, and reason about connection-level problems: timeouts, keep-alive, ephemeral ports, load balancers.

Prerequisite: L1, L2, Java core L3 (JVM diagnostics), Web L3. Katas: `cd katas && ./mvnw test -Dgroups="ops & L3"`. Drill: `java quiz/Quiz.java --track git-linux-networking --level 3`.

---

## 1. Finding the change that broke it: git bisect

A regression appeared somewhere in the last 300 commits. Bisect binary-searches history: mark a known good and a known bad commit; Git checks out the middle; you test and mark it; repeat. 300 commits need about **9 tests** (log2 300).

```
git bisect start
git bisect bad HEAD
git bisect good v1.8.0
git bisect run ./mvnw -q test -Dtest=PriceTest      # automatic: exit 0 = good, 1-127 = bad, 125 = skip
git bisect reset
```

- `git bisect skip` for commits that cannot be tested (do not compile); if only skipped commits remain near the boundary, Git lists all candidates.
- It works best with small commits that each build and pass tests: one more reason for good commit hygiene.
- Similar search applies elsewhere: bisecting dependency upgrades, configuration flags, or data (which input row triggers the bug).

> **Kata `Bisect`**: binary search with skips and the "could be any of" result.

---

## 2. Debugging a live system

A method that works: **observe, hypothesise, test one thing, repeat**, and write down what you checked. First stop the bleeding (rollback, feature flag, scale up), then find the cause.

Linux tools worth knowing:

| Symptom | Look at |
|---|---|
| high CPU | `top -H -p <pid>` (threads), then `jstack`/`jcmd <pid> Thread.print` and match thread ids (hex), async-profiler flame graphs |
| memory growing | `jcmd <pid> GC.heap_info`, heap dump + Eclipse MAT, `jcmd VM.native_memory`, container OOMKills (track 11) |
| "too many open files" | `ls /proc/<pid>/fd | wc -l`, `ulimit -n`, leaked connections or streams |
| disk full | `df -h`, `du -sh * | sort -h`, deleted-but-open files (`lsof +L1`), log rotation |
| slow requests | traces and metrics first (p99, saturation), thread dumps for blocked threads, database `pg_stat_activity` |
| network issues | `ss -s`, `ss -tan state time-wait | wc -l`, `tcpdump -i any port 5432` |

**JFR** (`jcmd <pid> JFR.start duration=60s filename=rec.jfr`) records CPU, allocations, locks, GC and I/O with low overhead, safe in production.

---

## 3. TLS and certificates

The handshake (TLS 1.3, simplified): client hello (supported ciphers, **SNI**: the hostname) → server hello + certificate chain + key share → both derive session keys (ephemeral Diffie-Hellman: forward secrecy) → encrypted data. One round trip (0-RTT resumption is possible).

The client verifies the **certificate**:
1. **Chain of trust**: leaf → intermediate(s) → a root in the client's trust store. Servers must send the intermediates; a missing intermediate is a classic "works in the browser (it caches intermediates), fails in Java".
2. **Validity period**: not before / not after. Expired certificates are among the most common outages: automate renewal (ACME / Let's Encrypt, cert-manager) and **alert weeks before expiry**.
3. **Hostname**: the requested name must match a **Subject Alternative Name** (the Common Name is ignored by modern clients). Wildcards cover exactly one leftmost label: `*.example.com` matches `api.example.com`, not `example.com` or `a.b.example.com`.
4. Revocation (OCSP, CRLs), with mixed support.

Java specifics: the trust store is `$JAVA_HOME/lib/security/cacerts` (or one you pass with `-Djavax.net.ssl.trustStore`); corporate proxies that intercept TLS need their CA added; `-Djavax.net.debug=ssl:handshake` shows the handshake; `PKIX path building failed` means the chain could not be validated against the trust store. **Never "fix" it by trusting all certificates.**

**mTLS** (mutual TLS): the client presents a certificate too, used for service-to-service authentication (service meshes do it automatically).

> **Kata `CertificateCheck`**: SAN and wildcard matching rules, IP entries, validity with an early warning.

---

## 4. Connections under load

- **Timeouts everywhere**: connect timeout (seconds), read/response timeout (based on the backend's p99.9), and an overall deadline; no timeout means a hung thread and eventually a hung service (Web track L3).
- **Connection pools and keep-alive**: TCP + TLS handshakes cost round trips, so clients reuse connections. Load balancers and NATs drop idle connections (AWS ALB 60 s, NAT gateways 350 s): set client idle timeouts lower, or you get sporadic `Connection reset`.
- **TIME_WAIT and ephemeral ports**: the side that closes first keeps the socket in TIME_WAIT (2 × MSL, ~60 s on Linux). A client opening a new connection per request to the same destination can exhaust its ~28,000 ephemeral ports: use pooling.
- **Backlog and SYN queues**: a server that cannot accept fast enough drops new connections (clients see timeouts); thread pool and accept queue sizes matter.
- **Load balancers**: L4 (TCP: NLB) vs L7 (HTTP: ALB, Nginx, Envoy, ingress controllers: routing by path/host, TLS termination, retries, health checks). The real client IP arrives in `X-Forwarded-For`/`Forwarded` headers, which you must only trust from your own proxies (Spring: `server.forward-headers-strategy`).
- **HTTP/2** multiplexes many requests over one connection (no head-of-line blocking at the HTTP level, still at the TCP level); **HTTP/3** runs over QUIC (UDP) to remove that too.
- **MTU** problems (a VPN or tunnel shrinking packet size): small requests work, large ones hang. Rare, memorable.

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `Bisect` | binary search over history, skips, minimal tests |
| `CertificateCheck` | SAN matching, wildcard rules, IP SANs, expiry windows |

```
cd katas && ./mvnw test -Dgroups="ops & L3"
```

## 6. Interview talk track

"I work in small commits on short-lived branches, rebase my own branch before merging but never rewrite shared history, and use revert to undo on main. When a regression appears I bisect, ideally with `git bisect run` and a test. In production I first mitigate, then follow the layers: metrics and traces for where time goes, thread dumps and JFR for the JVM, `ss` and `/proc` for sockets and file descriptors, DNS, ports and TLS for connectivity, where refused and timed out mean different things. For TLS I check the chain, the expiry and the SAN match with `openssl s_client`, automate renewal and alert well before expiry. And I make every network call time out, reuse connections with idle timeouts below the load balancer's, and only trust forwarded headers from my own proxies."

**Questions you should be able to answer:**
1. How many steps does bisecting 1,000 commits take? What does `git bisect run` need from the script?
2. A Java service uses 100% CPU. How do you find the responsible code?
3. "Too many open files": causes and how to investigate.
4. Walk through a TLS 1.3 handshake. What is SNI? Forward secrecy?
5. A client gets `PKIX path building failed`. What are the likely causes?
6. Does `*.example.com` match `example.com`? `a.b.example.com`?
7. Why do clients see sporadic `Connection reset` through a load balancer, and how do you fix it?
8. What is TIME_WAIT, and when does it become a problem?
9. L4 vs L7 load balancers. How does your app learn the client's real IP?
