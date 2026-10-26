# Docker and deployment, L2: layers and caching, compose, networking, health checks

**Goal:** build images that are small and fast to rebuild, run a multi-service stack locally with Docker Compose, understand container networking and volumes, and design health checks and configuration the way orchestrators expect.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="docker & L2"`. Drill: `java quiz/Quiz.java --track docker-deploy --level 2`.

---

## 1. Layers and the build cache

Every `RUN`, `COPY` and `ADD` creates a **layer**: a tar of the file-system changes, identified by its content. Layers are shared between images and pulled once per host.

The build cache reuses a step's layer when the instruction is unchanged, its inputs (the copied files' contents) are unchanged, and **every previous step was reused**. One miss rebuilds everything after it. So order a Dockerfile from **least to most frequently changing**:

```dockerfile
COPY pom.xml .                         # changes rarely
RUN mvn -B dependency:go-offline       # the slow download stays cached
COPY src ./src                         # changes on every commit
RUN mvn -B package
```

With `COPY . .` first, every source change re-downloads all dependencies.

More:
- **BuildKit cache mounts** (`RUN --mount=type=cache,target=/root/.m2 mvn ...`) keep the Maven repository between builds without storing it in a layer.
- **Spring Boot layered jars**: `java -Djarmode=tools -jar app.jar extract --layers --launcher` splits the jar into `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`; copy them as separate layers so a code change ships a few hundred KB instead of 80 MB. Buildpacks (`mvn spring-boot:build-image`) and Jib do this for you without a Dockerfile.
- Image size: a JRE (not a JDK) base, distroless or `-alpine`/`-noble` variants, `jlink` custom runtimes; fewer packages also mean fewer CVEs.
- In CI, reuse the cache across runners (`--cache-from`/`--cache-to type=registry` or the GitHub Actions cache).

> **Kata `LayerCache`**: model cache invalidation and see why ordering matters.

---

## 2. Docker Compose for local stacks

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/shop     # service name = host name
    depends_on:
      db: { condition: service_healthy }                        # wait for HEALTHY, not just started
  db:
    image: postgres:17
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck: { test: ["CMD-SHELL", "pg_isready -U shop"], interval: 5s }
volumes:
  pgdata:
```

`docker compose up -d`, `logs -f app`, `ps`, `exec db psql -U shop`, `down` (keeps named volumes), `down -v` (deletes them). Compose is for development, CI and small single-host deployments; production clusters use an orchestrator. See [examples/compose.yaml](examples/compose.yaml). Spring Boot's `spring-boot-docker-compose` module can even start the compose file when you run the app.

---

## 3. Health checks and probes

An orchestrator asks three different questions:

| Probe | Question | On failure | Typical check |
|---|---|---|---|
| **startup** | has it finished starting? | wait (other probes are off until it passes) | the app reports started |
| **liveness** | is it broken beyond self-repair? | **restart** the container | the process responds; no deadlock |
| **readiness** | should it get traffic now? | remove from the load balancer, **no restart** | dependencies reachable, warmed up, not shutting down |

The classic outage: liveness checks the database. The database has a hiccup, every pod fails liveness, Kubernetes restarts them all, the restarts storm the recovering database. **Dependencies belong in readiness, never in liveness.**

Spring Boot Actuator exposes `/actuator/health/liveness` and `/actuator/health/readiness` (probes are enabled automatically on Kubernetes; `management.endpoint.health.probes.enabled=true` elsewhere), backed by `ApplicationAvailability` (`LivenessState`, `ReadinessState`), and readiness flips to `REFUSING_TRAFFIC` at shutdown. Add indicators to the readiness group with `management.endpoint.health.group.readiness.include=readinessState,db`.

Docker's own `HEALTHCHECK` (and compose's `healthcheck`) only marks a container unhealthy; compose uses it for `depends_on: service_healthy`.

> **Kata `AppHealth`**: implement the three probes with the right semantics.

---

## 4. Networking and volumes

- **Bridge networks**: containers on the same user-defined network (compose creates one) reach each other **by service name** through Docker's DNS; `localhost` inside a container is the container itself, not the host or another container. `host.docker.internal` reaches the host (Docker Desktop).
- **Published ports** (`-p 8080:8080`) map host ports to container ports; bind to `127.0.0.1:8080:8080` to avoid exposing a dev database to the network.
- **Volumes**: named volumes (managed by Docker, survive container removal) for data; **bind mounts** (`./src:/app/src`) for development; `tmpfs` for scratch data. Never store state you care about in the container layer.
- Run databases in containers for development and tests; in production, prefer a managed database unless you have a reason and the operational skills.

---

## 5. Configuration (twelve-factor)

- One image for every environment; differences come from **environment variables** or mounted config files. Spring Boot's relaxed binding maps `SPRING_DATASOURCE_URL` to `spring.datasource.url` (Spring Boot L3).
- Secrets via the orchestrator's secret mechanism (mounted files or env vars from a secret store), never in the image or the compose file in Git (`${DB_PASSWORD:?}` reads it from the environment or `.env`, which is git-ignored).
- **Logs to stdout/stderr**, one event per line (JSON in production); the platform collects them.
- Disposable processes: fast start-up, graceful shutdown (L3), no local state.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `LayerCache` | cache keys, invalidation cascade, ordering COPY and RUN |
| `AppHealth` | startup vs liveness vs readiness, what must not fail liveness |

```
cd katas && ./mvnw test -Dgroups="docker & L2"
```

## 7. Seen in the shop project

- `docker-compose.yml` with PostgreSQL for local runs.
- `application.properties`: `management.endpoint.health.probes.enabled=true`, liveness = `livenessState` only, readiness = `readinessState,db`: exactly the rule of section 3. The manifests in `deploy/k8s/` (Level 6) point their probes at these groups.
- Configuration through environment variables only; no secrets in the repository.

## 8. Interview checklist

1. How does Docker's build cache decide to reuse a layer? How do you order a Java Dockerfile?
2. Why doesn't deleting files in a later `RUN` make the image smaller?
3. What are Spring Boot layered jars for?
4. Liveness vs readiness vs startup probes. What must never be in a liveness check?
5. How do containers in a compose stack find each other? What does `localhost` mean inside a container?
6. Named volume vs bind mount.
7. How should a containerised app get its configuration and secrets? Where should it log?
