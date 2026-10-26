# Track 03: Build tools (Maven, with Gradle for comparison)

How a Java project becomes a tested, reproducible artifact, and what to do when the classpath lies to you.

| Level | Lesson | Katas (`katas/src/main/java/dojo/build/`) | Playground |
|---|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): what a build tool does, coordinates and repositories, lifecycle phases and goals, versions and SNAPSHOTs, scopes, the wrapper | `MavenVersion`, `Scopes` | exercises 1-3 |
| L2 Practical | [L2-practical.md](L2-practical.md): parent vs aggregator, nearest-wins mediation, dependencyManagement and BOMs, exclusions, plugins, multi-module reactor, profiles, Gradle compared | `DependencyMediation` (DSA: BFS), `BuildOrder` (DSA: topological sort) | exercises 4-8 |
| L3 Production | [L3-production.md](L3-production.md): reproducible builds, NoSuchMethodError and friends, enforcer rules, fast monorepo CI, releases, supply-chain security, Spring Boot packaging | `AffectedModules` (DSA: reverse graph), `UpperBounds`, `LockFile` | exercises 9-11 |

```
cd katas && ./mvnw test -Dgroups="build & L2"
katas/mvnw -f tracks/03-build-tools/playground verify      # from the repo root
java quiz/Quiz.java --track build-tools
```

`UpperBounds` (L3) uses your `MavenVersion` (L1) and `DependencyMediation` (L2): do them first.

Question bank: [questions.md](questions.md).
