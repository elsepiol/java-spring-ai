# Track 11: Docker and deployment

From "what is a container" to running Java services on Kubernetes: images, tags and Dockerfiles, then layers, compose and health probes, then the JVM under container limits, graceful shutdown, rollouts and CI/CD.

| Level | Lesson | Katas (`katas/src/main/java/dojo/docker/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): containers vs VMs, images vs containers, names/tags/digests, a correct Dockerfile | `l1/ImageRef`, `l1/DockerfileLinter` |
| L2 Practical | [L2-practical.md](L2-practical.md): layers and the build cache, layered jars, Docker Compose, startup/liveness/readiness, networking and volumes, twelve-factor config | `l2/LayerCache`, `l2/AppHealth` |
| L3 Production | [L3-production.md](L3-production.md): JVM memory and CPU in containers, graceful shutdown, Kubernetes objects, rolling/blue-green/canary, CI/CD and GitOps | `l3/ShutdownCoordinator`, `l3/RollingUpdate` |

Reference files: [examples/Dockerfile](examples/Dockerfile) (production multi-stage build with layered jars), [examples/compose.yaml](examples/compose.yaml) (app + PostgreSQL with health checks), [examples/deployment.yaml](examples/deployment.yaml) (a Kubernetes Deployment with probes, resources, graceful shutdown and security context).

```
cd katas && ./mvnw test -Dgroups="docker & L1"
java quiz/Quiz.java --track docker-deploy
```

The katas need no Docker: they model what Docker and Kubernetes do, so the rules stick. Try the example files on a machine with Docker to see them for real.

Question bank: [questions.md](questions.md) (60 questions).
