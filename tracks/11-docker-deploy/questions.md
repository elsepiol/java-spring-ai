# Docker and deployment: question bank

Drill with `java quiz/Quiz.java --track docker-deploy`.

## L1

### DOCK-L1-001 · recall
What is a container, technically?

<details><summary>Answer</summary>

An ordinary Linux process (or group of processes) isolated by kernel namespaces (PID, network, mount, UTS, IPC, user) and limited by cgroups (memory, CPU, PIDs, I/O), running on a root file system unpacked from an image. It shares the host's kernel.

</details>

### DOCK-L1-002 · recall
Container vs virtual machine?

<details><summary>Answer</summary>

A VM virtualises hardware and runs its own guest kernel (strong isolation, seconds to boot, GBs of overhead). A container shares the host kernel and isolates only the process's view (milliseconds to start, little overhead, weaker isolation: a kernel vulnerability affects all containers).

</details>

### DOCK-L1-003 · recall
Image vs container?

<details><summary>Answer</summary>

An image is a read-only template: stacked file-system layers plus metadata (entrypoint, env, user, ports). A container is a running or stopped instance of an image, with its own thin writable layer, process and network identity. One image, many containers.

</details>

### DOCK-L1-004 · predict
An app writes uploaded files to `/app/uploads` inside its container. The container is replaced during a deploy. What happens to the files?

<details><summary>Answer</summary>

They are lost: they lived in the container's writable layer, which is deleted with the container. Store state outside: a volume (single host), object storage such as S3 (the usual answer for uploads), or a database.

</details>

### DOCK-L1-005 · recall
What does `-p 8080:8080` do, and what does `EXPOSE 8080` do?

<details><summary>Answer</summary>

`-p host:container` publishes the container port on the host, so it is reachable from outside. `EXPOSE` is only documentation and metadata in the image (used by `-P` and tools); it does not publish anything.

</details>

### DOCK-L1-006 · recall
What image does `postgres` refer to?

<details><summary>Answer</summary>

`docker.io/library/postgres:latest`: Docker Hub (default registry), the `library/` namespace for official images, and the default tag `latest`.

</details>

### DOCK-L1-007 · recall
Tag vs digest?

<details><summary>Answer</summary>

A tag (`:17`, `:1.4.2`, `:latest`) is a movable, human-readable pointer that can be re-pushed to point at different content. A digest (`@sha256:...`) is the content hash of the image manifest: it always identifies exactly the same image.

</details>

### DOCK-L1-008 · why
Why shouldn't you deploy images tagged `latest`?

<details><summary>Answer</summary>

`latest` can point to different images over time, so you cannot tell which version runs, two nodes may pull different contents, rollbacks are guesswork, and Kubernetes defaults to `imagePullPolicy: Always` for it. Deploy immutable tags (a version or the git SHA) or digests.

</details>

### DOCK-L1-009 · recall
What is a multi-stage build, and why use it?

<details><summary>Answer</summary>

A Dockerfile with several `FROM` stages: an early stage with build tools (JDK, Maven, npm) compiles the app, and the final stage copies only the artefacts (`COPY --from=build`) onto a small runtime image. The result is smaller, has fewer packages (fewer CVEs) and contains no source code or build secrets.

</details>

### DOCK-L1-010 · why
Why should a container not run as root?

<details><summary>Answer</summary>

If the app is compromised, the attacker is root inside the container: they can modify the file system, use more kernel attack surface, and come closer to escaping to the host (root in the container is root on the host without user namespaces). Create a dedicated user and switch with `USER`; Kubernetes can enforce `runAsNonRoot`.

</details>

### DOCK-L1-011 · recall
`COPY` vs `ADD`?

<details><summary>Answer</summary>

Both copy files from the build context. `ADD` also downloads remote URLs and auto-extracts local tar archives, which is implicit and surprising. Prefer `COPY`; use `ADD` only when you need the extraction.

</details>

### DOCK-L1-012 · recall
`ENTRYPOINT` vs `CMD`?

<details><summary>Answer</summary>

`ENTRYPOINT` is the executable that always runs; `CMD` provides default arguments (or the default command if there is no ENTRYPOINT), and `docker run image args` replaces CMD. The usual pattern is `ENTRYPOINT ["java", "-jar", "app.jar"]` and optional `CMD` defaults.

</details>

### DOCK-L1-013 · bug
`ENTRYPOINT java -jar /app/app.jar`. On `docker stop`, the app is killed after 10 seconds without running its shutdown hooks. Why?

<details><summary>Answer</summary>

Shell form runs `/bin/sh -c "java -jar ..."`: the shell is PID 1 and does not forward SIGTERM to java, so after the timeout Docker sends SIGKILL. Use exec form `ENTRYPOINT ["java", "-jar", "/app/app.jar"]` (or `exec java ...` in a script, or an init like tini).

</details>

### DOCK-L1-014 · bug
`ENV DB_PASSWORD=s3cret` in a Dockerfile. What is wrong?

<details><summary>Answer</summary>

The secret becomes part of the image metadata: anyone who can pull the image sees it with `docker inspect` or `docker history`, and it is the same in every environment. Inject secrets at runtime (orchestrator secrets, mounted files) and use BuildKit secret mounts for build-time secrets.

</details>

### DOCK-L1-015 · bug
```dockerfile
RUN apt-get update && apt-get install -y curl
RUN rm -rf /var/lib/apt/lists/*
```
Why doesn't the second line make the image smaller?

<details><summary>Answer</summary>

Each RUN creates a layer, and layers are additive: the package lists are stored in the first layer, and the second layer only marks them deleted. Clean up in the same RUN: `apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*`.

</details>

### DOCK-L1-016 · recall
What is `.dockerignore` for?

<details><summary>Answer</summary>

It excludes files from the build context sent to the builder: `target/`, `.git/`, `node_modules/`, IDE files, `.env`. That makes builds faster, keeps caches stable (irrelevant files do not invalidate `COPY . .`) and prevents secrets from ending up in images.

</details>

### DOCK-L1-017 · recall
Name five everyday Docker commands for debugging a running container.

<details><summary>Answer</summary>

`docker ps` (what runs), `docker logs -f <c>` (stdout/stderr), `docker exec -it <c> sh` (a shell inside), `docker inspect <c>` (config, env, mounts, IP, exit code), `docker stats` (CPU and memory), plus `docker top` and `docker events`.

</details>

### DOCK-L1-018 · why
Why should a containerised app log to stdout rather than to files?

<details><summary>Answer</summary>

The platform (Docker, Kubernetes) captures stdout/stderr and ships it to the log system; files inside the container fill its writable layer, disappear with the container, and need extra agents to collect. One event per line, ideally JSON.

</details>

### DOCK-L1-019 · interview
"Why containers? What problems do they solve, and which don't they solve?"

<details><summary>Answer</summary>

They package the app with its runtime and dependencies into one immutable artefact that runs the same everywhere, start fast, use resources densely, and give orchestrators a uniform unit to schedule, scale and restart. They do not solve configuration management, state and data, security (shared kernel, image CVEs), observability, or bad architecture, and they add a layer to learn and operate.

</details>

### DOCK-L1-020 · dsa
A layer cache works like a chain of hashes: each layer's id depends on its parent's id and its own content. Which data structure is this, and why does changing an early layer invalidate all later ones?

<details><summary>Answer</summary>

A hash chain (like a Merkle chain or a blockchain): id(n) = hash(id(n-1), instruction, inputs). Changing layer k changes id(k), and therefore the parent id of k+1 and so on, so no later cached entry matches anymore and everything after k is rebuilt.

</details>

## L2

### DOCK-L2-001 · recall
When does Docker reuse a cached layer during a build?

<details><summary>Answer</summary>

When the instruction text is identical, the files it copies have the same contents (checksums), and all previous steps were cache hits (same parent layer). For RUN, only the command string is compared, not what it would download, which is why `apt-get update` alone can serve stale caches.

</details>

### DOCK-L2-002 · design
Order a Dockerfile for a Maven project so that a source code change does not re-download dependencies.

<details><summary>Answer</summary>

`COPY pom.xml .` (and `.mvn/`, `mvnw`), then `RUN mvn dependency:go-offline` (cached until the POM changes), then `COPY src ./src` and `RUN mvn package`. Add a BuildKit cache mount for `~/.m2` so even a POM change reuses already downloaded artefacts.

</details>

### DOCK-L2-003 · recall
What are Spring Boot layered jars?

<details><summary>Answer</summary>

Spring Boot can extract a jar into layers ordered by change frequency: `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application` (`java -Djarmode=tools -jar app.jar extract --layers --launcher`). Copying each into its own image layer means a code change rebuilds and pushes only the small application layer.

</details>

### DOCK-L2-004 · recall
Name three ways to build a Spring Boot image besides a hand-written Dockerfile.

<details><summary>Answer</summary>

Cloud Native Buildpacks (`mvn spring-boot:build-image`), Google Jib (`jib-maven-plugin`, no Docker daemon needed), and Paketo/other buildpack builders in CI. They produce layered, non-root images with sensible JVM settings.

</details>

### DOCK-L2-005 · recall
How do services in a Docker Compose stack reach each other?

<details><summary>Answer</summary>

Compose creates a network for the project; each service is reachable by its service name through Docker's embedded DNS (`jdbc:postgresql://db:5432/shop`), on the container port (published ports are only for the host).

</details>

### DOCK-L2-006 · bug
In compose, the app's datasource URL is `jdbc:postgresql://localhost:5432/shop`, and the database runs as service `db`. The app cannot connect. Why?

<details><summary>Answer</summary>

Inside a container, `localhost` is the container itself, not the host or another container. Use the service name: `jdbc:postgresql://db:5432/shop`.

</details>

### DOCK-L2-007 · bug
`depends_on: [db]`, yet the app crashes at startup with "connection refused" half the time. Why, and the fix?

<details><summary>Answer</summary>

Plain `depends_on` only waits for the db container to start, not for PostgreSQL to accept connections. Add a healthcheck to `db` (`pg_isready`) and use `depends_on: db: condition: service_healthy`, and make the app retry connections anyway (in production nobody orders start-ups for you).

</details>

### DOCK-L2-008 · recall
Named volume vs bind mount?

<details><summary>Answer</summary>

A named volume is storage managed by Docker (`pgdata:/var/lib/postgresql/data`): survives container removal, portable, good for data. A bind mount maps a host path into the container (`./src:/app/src`): good for development (live code), depends on the host's file layout and permissions.

</details>

### DOCK-L2-009 · recall
Startup vs liveness vs readiness probes.

<details><summary>Answer</summary>

Startup: has the app finished starting? (the other probes wait for it). Liveness: is it broken beyond repair, so restart it? Readiness: should it receive traffic right now? Failing readiness only removes the pod from load balancing; failing liveness restarts the container.

</details>

### DOCK-L2-010 · bug
The liveness probe includes a database check. The database is unavailable for two minutes. What happens?

<details><summary>Answer</summary>

Every pod fails liveness and Kubernetes restarts all of them, repeatedly, so there is no capacity even after the database recovers, and the restarting pods stampede it with connections (and cold JVMs). Dependency checks belong in readiness; liveness should only detect a broken process.

</details>

### DOCK-L2-011 · recall
Which Spring Boot endpoints and properties back Kubernetes probes?

<details><summary>Answer</summary>

`/actuator/health/liveness` and `/actuator/health/readiness`, enabled automatically on Kubernetes or with `management.endpoint.health.probes.enabled=true`, backed by `ApplicationAvailability` (`LivenessState`, `ReadinessState`). Groups are customised with `management.endpoint.health.group.readiness.include=readinessState,db`.

</details>

### DOCK-L2-012 · recall
What are the twelve-factor rules most relevant to containers?

<details><summary>Answer</summary>

One codebase, many deploys; explicit dependencies; config in the environment; backing services as attached resources; separate build, release and run stages; stateless processes; port binding; scale out via processes; disposability (fast start-up, graceful shutdown); dev/prod parity; logs as event streams; admin tasks as one-off processes.

</details>

### DOCK-L2-013 · why
Why build an image once and promote it through environments rather than building per environment?

<details><summary>Answer</summary>

What you tested is exactly what runs in production (same bytes, same digest). Rebuilding can pull different base images or dependencies. Environment differences go into configuration only.

</details>

### DOCK-L2-014 · recall
How does Spring Boot map the environment variable `SPRING_DATASOURCE_URL`?

<details><summary>Answer</summary>

Relaxed binding: upper case and underscores map to the lower-case dotted property `spring.datasource.url` (and `MY_APP_ITEMS_0_NAME` to `my-app.items[0].name`). Environment variables override application.properties in the property source order.

</details>

### DOCK-L2-015 · recall
How can you make an image smaller?

<details><summary>Answer</summary>

Multi-stage builds; a JRE or distroless base instead of a JDK and a full OS; `--no-install-recommends` and cleanup in the same RUN; `.dockerignore`; layered jars; a `jlink` custom runtime; and not copying build caches. Smaller images pull faster and have fewer vulnerabilities.

</details>

### DOCK-L2-016 · scenario
CI builds take 9 minutes because every run downloads all Maven dependencies again. How do you speed them up?

<details><summary>Answer</summary>

Order the Dockerfile so dependencies are a separate cached layer, use BuildKit cache mounts for `~/.m2`, and persist the build cache between CI runners (`docker buildx build --cache-from/--cache-to type=registry` or `type=gha`). Also cache the Maven repository for non-Docker steps.

</details>

### DOCK-L2-017 · recall
What does Docker's `HEALTHCHECK` instruction do?

<details><summary>Answer</summary>

Docker runs the command periodically inside the container and marks it `healthy`/`unhealthy`; compose uses that for `depends_on: condition: service_healthy`. Docker itself does not restart unhealthy containers (Swarm does); Kubernetes ignores it and uses its own probes.

</details>

### DOCK-L2-018 · scenario
A developer's local database container is reachable from the whole office network. Why, and how do you prevent it?

<details><summary>Answer</summary>

`-p 5432:5432` binds to all host interfaces (0.0.0.0). Bind to loopback only: `-p 127.0.0.1:5432:5432`, or do not publish the port at all and connect from containers on the same network. Docker's published ports also bypass some host firewall rules (ufw).

</details>

### DOCK-L2-019 · interview
"How would you set up a local development environment for a Spring Boot service with PostgreSQL and Redis?"

<details><summary>Answer</summary>

A compose file with postgres and redis (pinned versions, named volumes, healthchecks, ports bound to 127.0.0.1) and the app run from the IDE against them (or Spring Boot's docker-compose support starting them automatically). Configuration via a git-ignored `.env`/profile, Flyway creating the schema, seed data scripts, and the same images used by Testcontainers in tests.

</details>

### DOCK-L2-020 · dsa
A rebuild has steps of 2 s, 120 s (dependency download), 1 s, 40 s (compile). Expected time with the naive `COPY . .` first vs dependencies first, if 95% of commits change only source files?

<details><summary>Answer</summary>

Naive: every change invalidates everything: 2 + 120 + 1 + 40 = 163 s. Ordered: 95% of builds rerun only the last two steps (1 + 40 = 41 s), 5% everything (163 s): 0.95 × 41 + 0.05 × 163 ≈ 47 s. Ordering by change frequency cuts the expected build time by about 70%.

</details>

## L3

### DOCK-L3-001 · recall
How does a modern JVM size its heap inside a container by default?

<details><summary>Answer</summary>

It reads the cgroup memory limit (container support since Java 10/8u191) and sets the max heap to 25% of it by default (`MaxRAMPercentage=25`). For a dedicated container that is too little; set `-XX:MaxRAMPercentage=75` or an explicit `-Xmx`.

</details>

### DOCK-L3-002 · scenario
A pod with a 1 GiB memory limit and `-Xmx900m` is OOMKilled under load, while GC logs show the heap at 600 MB. Why?

<details><summary>Answer</summary>

The JVM uses memory beyond the heap: metaspace, code cache, thread stacks (hundreds of threads × ~1 MB), direct buffers, GC structures and native libraries. Heap + non-heap exceeded the cgroup limit and the kernel killed the process (exit code 137, no Java OOM error). Leave about 25% headroom (MaxRAMPercentage=75), limit threads, and measure with Native Memory Tracking.

</details>

### DOCK-L3-003 · recall
What does exit code 137 mean?

<details><summary>Answer</summary>

128 + 9: the process was killed with SIGKILL, typically by the kernel's OOM killer (`OOMKilled` in `kubectl describe pod`) or after the termination grace period expired. Exit code 143 (128 + 15) means it exited on SIGTERM.

</details>

### DOCK-L3-004 · why
Why can CPU limits cause latency spikes for Java services?

<details><summary>Answer</summary>

CPU limits are enforced by CFS quotas per 100 ms period: a multi-threaded JVM (GC threads, JIT, request threads) can use its quota in the first 20 ms and is then throttled for the remaining 80 ms, so requests stall. The JVM also sizes thread pools from the limit. Many teams set CPU requests (for scheduling and fair share) and omit CPU limits, or set them generously, and monitor throttling metrics.

</details>

### DOCK-L3-005 · recall
What happens, in order, when Kubernetes terminates a pod?

<details><summary>Answer</summary>

The pod becomes Terminating and is removed from Service endpoints (propagating asynchronously to kube-proxy, ingress controllers and load balancers). The preStop hook runs, then SIGTERM is sent to PID 1 in each container. After `terminationGracePeriodSeconds` (default 30), SIGKILL. Removal from endpoints and SIGTERM happen concurrently, which is why a short preStop sleep helps.

</details>

### DOCK-L3-006 · recall
What should an application do on SIGTERM?

<details><summary>Answer</summary>

Stop taking new work (readiness down, refuse or close listeners), finish in-flight requests within a deadline, stop consumers and schedulers (commit or release in-progress messages), flush buffers and close pools, then exit 0, all before the grace period ends. In Spring Boot: `server.shutdown=graceful` (the default since 3.4) and `spring.lifecycle.timeout-per-shutdown-phase`.

</details>

### DOCK-L3-007 · bug
`terminationGracePeriodSeconds: 30`, `preStop: sleep 20`, `spring.lifecycle.timeout-per-shutdown-phase=30s`. Problem?

<details><summary>Answer</summary>

The grace period counts from the start of termination, including preStop: 20 s of sleep leaves 10 s for Spring's 30 s graceful shutdown, so long requests are cut off by SIGKILL. Make preStop + shutdown timeout + margin < terminationGracePeriodSeconds (e.g. 10 s + 20 s with a 45 s grace period).

</details>

### DOCK-L3-008 · recall
Pod, Deployment, Service: what is each for?

<details><summary>Answer</summary>

Pod: the smallest unit, one or more containers sharing network and volumes, ephemeral. Deployment: declares the desired replicas and pod template for stateless apps, and manages rolling updates through ReplicaSets. Service: a stable virtual IP and DNS name that load-balances to the ready pods selected by labels.

</details>

### DOCK-L3-009 · recall
Resource requests vs limits in Kubernetes?

<details><summary>Answer</summary>

Requests: what the scheduler reserves on a node and the guaranteed share (CPU weight). Limits: hard caps. Exceeding the memory limit gets the container OOMKilled; reaching the CPU limit throttles it. For JVMs, set memory request = limit for predictability.

</details>

### DOCK-L3-010 · recall
What do `maxSurge` and `maxUnavailable` control?

<details><summary>Answer</summary>

During a rolling update: `maxSurge` is how many pods may exist above the desired replica count (extra capacity for new pods); `maxUnavailable` is how many may be below it (old pods removed before new ones are ready). Percentages of replicas round up for surge and down for unavailable; both cannot be 0.

</details>

### DOCK-L3-011 · predict
A Deployment with 4 replicas, `maxSurge: 1`, `maxUnavailable: 0`, rolls out a version whose pods never become ready. What happens?

<details><summary>Answer</summary>

Kubernetes creates one new pod (5 in total), which never becomes ready, so no old pod is removed (unavailable must stay 0). The rollout stalls with full old capacity and eventually reports `ProgressDeadlineExceeded`. Nothing breaks for users; `kubectl rollout undo` cleans up.

</details>

### DOCK-L3-012 · recall
Rolling vs blue-green vs canary deployments.

<details><summary>Answer</summary>

Rolling: replace pods gradually; no extra environment, but versions overlap. Blue-green: run the full new version alongside, switch all traffic at once; instant rollback, double capacity. Canary: route a small percentage of traffic to the new version, compare error rates and latency, then increase; limits the blast radius but needs traffic splitting and good metrics.

</details>

### DOCK-L3-013 · why
Why must schema migrations be backward compatible when using rolling deployments?

<details><summary>Answer</summary>

During the rollout, old and new pods run at the same time against the same database. If the new version's migration renames or drops a column that old pods still use, the old pods fail. Use expand/contract: add the new structure, deploy code that works with both, migrate data, and remove the old structure in a later release.

</details>

### DOCK-L3-014 · recall
What is a PodDisruptionBudget?

<details><summary>Answer</summary>

A rule limiting voluntary disruptions (node drains, cluster upgrades, autoscaler scale-downs) for a set of pods: `minAvailable: 2` or `maxUnavailable: 1`. Evictions that would violate it wait, so maintenance does not take down too many replicas at once. It does not protect against crashes.

</details>

### DOCK-L3-015 · recall
ConfigMap vs Secret?

<details><summary>Answer</summary>

Both provide configuration to pods as environment variables or mounted files. Secrets are meant for sensitive data: base64-encoded (not encrypted by default), with separate RBAC, optional encryption at rest, and integration with external secret managers (External Secrets Operator, CSI driver). Mounted files can be updated without a restart; env vars cannot.

</details>

### DOCK-L3-016 · recall
What is GitOps?

<details><summary>Answer</summary>

The desired state of every environment (manifests, Helm values, Kustomize overlays) lives in Git; a controller in the cluster (Argo CD, Flux) continuously reconciles the cluster with it. Deploys are pull requests, history and audit come from Git, rollback is a revert, and drift is detected automatically.

</details>

### DOCK-L3-017 · design
Design a CI/CD pipeline for a containerised Spring Boot service.

<details><summary>Answer</summary>

On every push: build, unit tests, integration tests with Testcontainers. On main: build the image once, tag it with the git SHA, scan it (Trivy/Grype), generate an SBOM and sign it (cosign), push to the registry. Update the staging environment (GitOps commit), run smoke and contract tests, then promote the same digest to production via canary or rolling with automatic rollback on SLO alerts. Migrations run backward compatibly before or with the deploy.

</details>

### DOCK-L3-018 · recall
What are the four DORA metrics?

<details><summary>Answer</summary>

Deployment frequency, lead time for changes (commit to production), change failure rate (share of deployments causing failures), and time to restore service (recently "failed deployment recovery time"). High performers deploy often with low failure rates: speed and stability go together.

</details>

### DOCK-L3-019 · interview
"Your service works locally but the pods keep restarting in Kubernetes. How do you investigate?"

<details><summary>Answer</summary>

`kubectl get pods` (restart count, status), `kubectl describe pod` (events: OOMKilled, failed probes, image pull errors, exit codes), `kubectl logs --previous` (logs of the crashed container). Common causes: liveness probe failing (too short initial delay or startup time, probe hitting a dependency), OOMKilled (memory limit vs JVM settings), missing config or secrets, wrong ports, or a crash on startup (database unreachable). Fix the probe timing with a startup probe, size memory, and check the configuration.

</details>

### DOCK-L3-020 · dsa
How many steps does a rolling update of n replicas take with `maxSurge: 1, maxUnavailable: 0`, and with `maxSurge: s, maxUnavailable: 0` in general?

<details><summary>Answer</summary>

Each step can add at most s new pods and then remove s old ones, so ceil(n / s) steps: n steps for s = 1 (4 replicas: 4 steps). The trade-off is extra capacity (s pods) against rollout time; with s = n it becomes a blue-green-like single step.

</details>
