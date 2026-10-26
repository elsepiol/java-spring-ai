# Docker and deployment, L3: the JVM in containers, graceful shutdown, Kubernetes, CI/CD

**Goal:** run Java correctly under container limits, shut down without dropping requests, understand the Kubernetes objects and rollout strategies you deploy with, and design a CI/CD pipeline with safe releases and rollbacks.

Prerequisite: L1, L2, Spring Boot web L3, Concurrency L1. Katas: `cd katas && ./mvnw test -Dgroups="docker & L3"`. Drill: `java quiz/Quiz.java --track docker-deploy --level 3`.

---

## 1. The JVM inside a container

- Since Java 10 (and 8u191) the JVM is **container-aware**: it reads the cgroup memory and CPU limits. The default max heap is only **25%** of the limit: set `-XX:MaxRAMPercentage=75` (or an explicit `-Xmx`).
- Memory is more than the heap: **heap + metaspace + code cache + thread stacks (~1 MB each) + direct buffers (Netty) + GC structures + native allocations**. A container at its memory limit is **OOMKilled** by the kernel (exit code 137) with no Java stack trace. Leave headroom (25%) and watch native memory (`-XX:NativeMemoryTracking=summary`, `jcmd VM.native_memory`).
- Add `-XX:+ExitOnOutOfMemoryError` (or `CrashOnOutOfMemoryError` plus a heap dump to a volume) so a broken JVM is restarted instead of limping.
- **CPU**: the JVM sizes GC and ForkJoin pools from the CPU limit (`availableProcessors`). Low CPU limits cause **throttling** (CFS quota: the container is paused for the rest of each 100 ms period), which looks like random latency spikes. Many teams set CPU *requests* but no CPU *limits*.
- Start-up: CDS archives, Spring AOT, or GraalVM native images when start time matters (scale-to-zero, serverless); and **virtual threads** reduce thread-stack memory for blocking workloads.

---

## 2. Graceful shutdown

What happens when Kubernetes deletes a pod:

```
1. pod marked Terminating; removed from Service endpoints (asynchronously, takes a moment to propagate)
2. preStop hook runs (e.g. sleep 10, to let load balancers stop routing to it)
3. SIGTERM to PID 1
4. ... terminationGracePeriodSeconds (default 30) ...
5. SIGKILL
```

The application must, on SIGTERM: **stop accepting new requests** (readiness down, refuse with 503 or close the listener), **finish in-flight requests** with a deadline, stop consumers and schedulers, flush and close resources, then exit. Spring Boot: `server.shutdown=graceful` (the default since Boot 3.4) and `spring.lifecycle.timeout-per-shutdown-phase=20s`; the JVM runs shutdown hooks on SIGTERM, which only arrives if Java is PID 1 (exec-form entrypoint) or a small init (`tini`) forwards it.

Budget: preStop sleep + graceful timeout < terminationGracePeriodSeconds. Message consumers: stop polling, finish or nack the current message (at-least-once processing makes a crash safe, idempotency makes redelivery harmless).

> **Kata `ShutdownCoordinator`**: refuse new requests, drain in-flight ones, respect a deadline, thread-safe.

---

## 3. Kubernetes in one page

| Object | Purpose |
|---|---|
| **Pod** | one or more containers scheduled together (shared network namespace, volumes); ephemeral |
| **Deployment** | desired state for stateless pods: replicas, the pod template, the rollout strategy; manages ReplicaSets |
| **Service** | a stable virtual IP and DNS name (`shop.default.svc`) load-balancing to ready pods matching a label selector |
| **Ingress / Gateway** | HTTP routing from outside (host, path, TLS) to Services |
| **ConfigMap / Secret** | configuration and secrets as env vars or files |
| **StatefulSet** | stable identity and storage per pod (databases, Kafka) |
| **Job / CronJob** | run-to-completion and scheduled work |
| **HorizontalPodAutoscaler** | scales replicas on CPU, memory or custom metrics |
| **PodDisruptionBudget** | limits voluntary evictions (node drains) so enough replicas stay up |

**Requests vs limits**: requests are what the scheduler reserves (placement and CPU share); limits are caps (memory: OOMKill; CPU: throttling). Set memory request = limit for predictable JVMs.

**Rolling updates**: `maxSurge` (extra pods allowed above the desired count) and `maxUnavailable` (pods allowed below it); percentages round **up** for surge and **down** for unavailable. `maxSurge: 1, maxUnavailable: 0` keeps full capacity at the cost of one extra pod. Readiness gates every step: a new pod that never becomes ready stops the rollout (`kubectl rollout status`, `kubectl rollout undo`). See [examples/deployment.yaml](examples/deployment.yaml).

> **Kata `RollingUpdate`**: simulate the rollout steps and the percentage rounding.

---

## 4. Release strategies

| Strategy | How | Trade-off |
|---|---|---|
| Recreate | stop all old, start all new | downtime; simplest (needed when versions cannot coexist) |
| Rolling | replace pods gradually | no downtime; old and new run side by side, so both must work with the same database and APIs |
| Blue-green | full new environment, switch traffic at once | instant rollback; double capacity during the switch |
| Canary | send a small share of traffic (1%, 10%) to the new version, watch metrics, then continue | catches problems with real traffic; needs good metrics and traffic splitting |
| Feature flags | deploy dark, enable per user or percentage | decouples deploy from release; flags need cleanup |

Because old and new versions run at the same time, **database migrations must be backward compatible** (expand/contract, SQL track L3) and APIs must tolerate both versions (Web track L3).

---

## 5. CI/CD

A typical pipeline: build and unit tests → integration tests (Testcontainers) → build the image **once**, tag it with the git SHA, scan it (Trivy, Grype), sign it (cosign) and generate an SBOM → push to the registry → deploy the **same image** to staging → smoke tests → production (manual approval or automatic, canary) → post-deploy verification and automatic rollback on alerts.

- **GitOps** (Argo CD, Flux): the desired state of each environment lives in a Git repository; a controller in the cluster applies it. Deploys and rollbacks are commits.
- Keep configuration per environment outside the image (Helm values, Kustomize overlays).
- Measure delivery with the DORA metrics: deployment frequency, lead time for changes, change failure rate, time to restore.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `ShutdownCoordinator` | graceful shutdown, in-flight tracking, deadlines, monitors |
| `RollingUpdate` | maxSurge/maxUnavailable, availability invariants, percentage rounding |

```
cd katas && ./mvnw test -Dgroups="docker & L3"
```

## 7. Seen in the shop project

- Level 6: `deploy/k8s/` manifests (Deployment with probes and resources, ConfigMap, Ingress, HorizontalPodAutoscaler, PodDisruptionBudget), graceful shutdown configured in `application.properties`, `-XX:MaxRAMPercentage=75` in the image, and a CI job that starts the image next to a PostgreSQL container and smoke-tests it.

## 8. Interview talk track

"A container is a process with namespaces and cgroups, so I treat the JVM's view of memory and CPU carefully: MaxRAMPercentage around 75%, headroom for metaspace, threads and direct memory, exit on OOM, and usually no CPU limit to avoid throttling. The image is multi-stage, non-root, pinned, with dependencies in their own layers, built once per commit, scanned and promoted unchanged. On Kubernetes I separate startup, liveness and readiness probes, never putting dependencies in liveness, and I make shutdown graceful: readiness off, a short preStop delay, drain in-flight requests within the grace period. Rollouts use maxSurge/maxUnavailable gated by readiness, or canaries for risky changes, with backward-compatible migrations because old and new versions overlap. Deploys are automated and reversible, ideally through GitOps."

**Questions you should be able to answer:**
1. A pod is OOMKilled although the heap is only 60% used. Why?
2. Why do CPU limits cause latency spikes in Java services?
3. Walk through what happens when Kubernetes terminates a pod. How do you avoid dropped requests?
4. Requests vs limits.
5. What do maxSurge and maxUnavailable control? What happens to a rollout when new pods never become ready?
6. Rolling vs blue-green vs canary.
7. Why must migrations be backward compatible with rolling deployments?
8. What does a good CI/CD pipeline for a containerised service look like?
