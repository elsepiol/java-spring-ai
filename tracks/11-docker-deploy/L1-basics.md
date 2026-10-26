# Docker and deployment, L1: images, containers, Dockerfiles

**Goal:** understand what a container really is (and is not), the difference between images and containers, how image names and tags work, and how to write a correct Dockerfile for a Java service.

Prerequisite: Build tools L1, basic shell (track 14 L1). Katas: `cd katas && ./mvnw test -Dgroups="docker & L1"`. Drill: `java quiz/Quiz.java --track docker-deploy --level 1`.

---

## 1. What a container is

A container is **an ordinary Linux process** with isolation and limits from the kernel:
- **Namespaces** give it its own view: process ids (it is PID 1 inside), network interfaces and ports, mount points (its own root file system), hostname, users.
- **cgroups** limit and account resources: memory, CPU, PIDs, I/O.
- A **root file system** unpacked from an image (layers stacked by an overlay file system).

It is **not a virtual machine**: there is no guest kernel; all containers share the host's kernel. Start-up takes milliseconds, overhead is small, and isolation is weaker than a VM's (a kernel exploit escapes it). On macOS and Windows, Docker Desktop runs a small Linux VM to host the containers.

Why containers: the same artefact runs on a laptop, in CI and in production ("works on my machine" solved at the OS level), dependencies are packaged with the app, and orchestrators (Kubernetes) can schedule, restart and scale them uniformly.

---

## 2. Images and containers

| | Image | Container |
|---|---|---|
| What | a read-only template: layers + metadata (entrypoint, env, exposed ports, user) | a running (or stopped) instance of an image, plus a thin writable layer |
| Analogy | a class | an object |
| Commands | `docker build`, `pull`, `push`, `images`, `rmi` | `docker run`, `ps`, `logs`, `exec`, `stop`, `rm` |

```
docker run -d --name shop -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ghcr.io/acme/shop:1.4.2
docker logs -f shop              # stdout/stderr of PID 1: log to stdout, not to files
docker exec -it shop sh          # a shell inside the running container (debugging)
docker stop shop                 # SIGTERM, then SIGKILL after 10 s
```

- `-p host:container` publishes a port; without it, the service is only reachable from other containers on the same network.
- The writable layer **disappears with the container**: state belongs in volumes or, better, in external services (database, object storage). Containers are **cattle, not pets**: replaceable at any moment.
- Configuration comes from the environment (`-e`, env files, mounted files), never baked into the image: one image, promoted unchanged from test to production (twelve-factor app).

---

## 3. Image names, tags and digests

```
ghcr.io/acme/shop:1.4.2@sha256:4f6c...
└─registry─┘└repository┘└tag┘└────digest────┘
```

- No registry part → Docker Hub (`docker.io`); official images live under `library/` (`postgres` = `docker.io/library/postgres:latest`).
- The first component is a registry only if it looks like a host (`ghcr.io`, `localhost:5000`); `acme/shop` is a Docker Hub user.
- A **tag** is a movable pointer: `postgres:17` moves with every patch release; `latest` is just the default tag name, not "the newest" in any reliable sense.
- A **digest** is the content hash of the image manifest: immutable. Pin base images by tag and (for reproducibility and supply-chain safety) digest; deploy your own images by an immutable tag (version or git SHA) or digest, **never `latest`**, or a rollback and "which version runs?" become guesswork.
- Multi-architecture images: one tag points to a manifest list (amd64, arm64); `docker buildx build --platform linux/amd64,linux/arm64`.

> **Kata `ImageRef`**: parse references the way Docker does: registry vs user, `library/`, ports vs tags, digests.

---

## 4. A Dockerfile for a Java service

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build          # stage 1: build tools
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline                    # cached until pom.xml changes
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre                         # stage 2: only what runs
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/app.jar /app/app.jar
USER app                                            # not root
EXPOSE 8080                                         # documentation; -p publishes
ENTRYPOINT ["java", "-jar", "/app/app.jar"]         # exec form: java is PID 1
```

Instructions: `FROM` (base image, starts a stage), `WORKDIR`, `COPY` (files from the build context), `RUN` (a command at build time, creating a layer), `ENV`/`ARG` (runtime vs build-time variables), `USER`, `EXPOSE`, `ENTRYPOINT`/`CMD` (what runs; `CMD` provides default arguments), `HEALTHCHECK`.

The common mistakes (the kata's rules):
- **Unpinned base images** (`FROM openjdk`): builds change under you; the `openjdk` images are deprecated anyway; use `eclipse-temurin` or another maintained distribution.
- **Running as root**: a compromised app is root in the container, closer to the host. Add a user and `USER` it.
- **`ADD` instead of `COPY`**: `ADD` also fetches URLs and unpacks archives, surprising behaviour; use `COPY` unless you need that.
- **Secrets in `ENV`/`ARG`**: baked into the image and visible with `docker history`/`inspect`. Use BuildKit secret mounts at build time (`RUN --mount=type=secret,...`) and runtime injection.
- **Package manager leftovers**: `apt-get install` without `rm -rf /var/lib/apt/lists/*` in the same `RUN` keeps the index in the layer forever (deleting it in a later layer does not shrink the image).
- **Shell-form `ENTRYPOINT java -jar app.jar`**: `/bin/sh -c` becomes PID 1, does not forward SIGTERM, and the JVM is killed after the timeout instead of shutting down gracefully.
- A `.dockerignore` (`target/`, `.git/`, `node_modules/`, `.env`) keeps the build context small and secrets out.

> **Kata `DockerfileLinter`**: implement those rules, including multi-stage awareness and line continuations.

See [examples/Dockerfile](examples/Dockerfile) for the full production version (layered jars, cache mounts, JVM memory flags).

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `ImageRef` | registries, repositories, tags, digests, Docker Hub defaults |
| `DockerfileLinter` | Dockerfile best practices, multi-stage builds, PID 1 and signals |

```
cd katas && ./mvnw test -Dgroups="docker & L1"
```

## 6. Seen in the shop project

- A multi-stage `Dockerfile`: an `eclipse-temurin:21-jdk` build stage, an `eclipse-temurin:21-jre` runtime stage, `USER appuser`, and an exec-form entrypoint with `-XX:MaxRAMPercentage=75`; `docker-compose.yml` with PostgreSQL for local runs.
- CI builds the image and runs a container smoke test against it (Level 6).

## 7. Interview checklist

1. Container vs virtual machine. What do namespaces and cgroups provide?
2. Image vs container.
3. What happens to files written inside a container when it is removed?
4. Tag vs digest. Why not deploy `latest`?
5. Why multi-stage builds?
6. Why not run as root in a container?
7. `COPY` vs `ADD`; `ENTRYPOINT` vs `CMD`; exec form vs shell form.
8. How do you keep secrets out of images?
