# Build tools, L3: builds in production

**Goal:** make builds reproducible and fast, debug the classpath problems that only appear at runtime, keep a monorepo's CI quick, release artifacts properly, and defend the build against supply-chain attacks. The build is part of production: everything that runs was produced by it.

Prerequisite: L1, L2. Katas: `cd katas && ./mvnw test -Dgroups="build & L3"`. Drill: `java quiz/Quiz.java --track build-tools --level 3`.

---

## 1. Reproducible and hermetic builds

**Reproducible:** the same sources produce the same artifact, byte for byte, anywhere. **Hermetic:** the build depends only on declared inputs, not on what happens to be installed or published today.

What breaks it, and the fix:

| Cause | Fix |
|---|---|
| a different Maven or JDK on each machine | the wrapper; `maven-enforcer` `requireJavaVersion`; CI uses a pinned JDK image |
| unpinned plugin versions | `pluginManagement` with explicit versions |
| version ranges, SNAPSHOT or `LATEST` dependencies | exact versions; no SNAPSHOT dependencies in a release |
| timestamps inside jars | `project.build.outputTimestamp` (the playground sets it: two clean builds give identical jars) |
| artifacts changing in a repository | checksums / lock files (section 5); an internal proxy repository |
| tests depending on the time zone, locale, network, order | inject clocks, set `-Duser.timezone`, isolate tests |

Why it matters: you can prove what is running in production came from that commit, rebuild an old version to patch it, and trust that "works in CI" means "works".

---

## 2. Dependency hell at runtime

The classic failures, all from **two versions of a library being compiled against, but only one being present at runtime**:

| Error | Usual meaning |
|---|---|
| `NoSuchMethodError` / `NoSuchFieldError` | library X was compiled against a newer version of Y that has the method; an older Y won mediation |
| `ClassNotFoundException` / `NoClassDefFoundError` | the class's jar is not on the runtime classpath (wrong scope, excluded, omitted), or its static initialiser failed |
| `AbstractMethodError`, `IncompatibleClassChangeError` | binary-incompatible versions mixed (an interface changed) |
| `ClassCastException: X cannot be cast to X` | the same class loaded by two class loaders |
| two logging backends, `SLF4J: multiple bindings` | two implementations on the classpath |

How to investigate:
1. `./mvnw dependency:tree -Dverbose -Dincludes=com.google.guava` (which versions were requested, which won, why).
2. Check the **resolved** jar list of the actual artifact (`jar tf app.jar | grep BOOT-INF/lib`), not your IDE.
3. Fix by declaring the right version in `dependencyManagement` (or the BOM), excluding the wrong artifact, or upgrading the library that drags in the old one.
4. Prevent recurrence with enforcer rules: **`requireUpperBoundDeps`** (fail when a resolved version is lower than one requested deeper in the tree), **`dependencyConvergence`** (fail when any artifact appears in several versions), `banDuplicateClasses` (extra-enforcer-rules).

**Shading** (maven-shade-plugin) copies a dependency into your jar under a renamed package (relocation): a library uses it to ship its own private Guava without conflicting with yours. Applications rarely need it.

> **Kata `UpperBounds`**: the enforcer rule, built on your `MavenVersion` and `DependencyMediation`.

---

## 3. Fast CI for big repositories

A slow build is a productivity and reliability problem: people stop running it locally and batch changes. Levers, roughly in order of payoff:
1. **Cache dependencies** between CI runs (`actions/setup-java` with `cache: maven` caches `~/.m2/repository`).
2. **Build only what is affected:** in a monorepo, from the files changed by a pull request, find the changed modules and everything that depends on them; build and test only those (`mvn -pl <changed> -amd`, Gradle's configuration/build cache, Bazel, Nx, Turborepo). Run the full build on the main branch.
3. **Parallelise:** `-T 1C` between independent modules; tests in parallel forks (surefire `forkCount`); split test suites across CI machines.
4. **Remote build cache** (Gradle, Bazel, Maven build cache extension): never rebuild what anyone already built from the same inputs.
5. **Separate fast and slow checks:** unit tests on every push; long integration and end-to-end suites in parallel jobs or before merge.
6. Measure: Gradle build scans, the per-module times in Maven's reactor summary, CI step timings.

> **Kata `AffectedModules`**: reverse the dependency graph, BFS from the changed modules, return them in build order.

---

## 4. Releasing artifacts

- A release turns `1.4.0-SNAPSHOT` into an immutable `1.4.0`: set the version, build and test once, tag the commit, deploy the artifact to the repository (Nexus, Artifactory, GitHub Packages, Maven Central), and move to `1.5.0-SNAPSHOT`. (maven-release-plugin automates the dance; many teams simply derive the version from the git tag in CI.)
- **Build once, deploy many:** the artifact (or Docker image) tested in staging is the very same bytes deployed to production, never rebuilt.
- **Libraries** must take semantic versioning seriously: removing a public method is a major version. Tools like japicmp or revapi check binary compatibility in CI.
- **Services** usually do not need semantic versions; a version per commit (`git describe`, commit SHA in the image tag) is enough, plus a changelog.

---

## 5. Supply-chain security

Your build downloads and **executes** code from strangers: dependencies, plugins, the build tool itself. Real attacks: typosquatted package names, hijacked maintainer accounts publishing malicious versions, **dependency confusion** (a public package with the same name as your internal one but a higher version), compromised build plugins, and vulnerable versions (Log4Shell, 2021).

Defences:
- **Pin and verify:** exact versions, and checksums of every artifact (Gradle dependency verification; for Maven, checksum policies, a lock file plugin, or a trusted proxy repository). Signature verification (PGP) where available.
- **One curated entry point:** all builds resolve through an internal repository proxy; internal group ids are reserved there, so a public package cannot shadow them.
- **Scan continuously:** OWASP Dependency-Check, Snyk, GitHub Dependabot alerts, Trivy on images; fail builds on critical known vulnerabilities, and have a process to update quickly.
- **SBOM** (software bill of materials, CycloneDX or SPDX): a machine-readable list of everything inside an artifact, so "are we affected by CVE-X?" takes minutes, not days.
- **Least privilege in CI:** pull requests from forks run without secrets; release credentials only in the release job; signed provenance (SLSA) for published artifacts.
- **Update regularly** in small steps (Renovate/Dependabot PRs with CI), so a security fix is a small bump, not a migration.

> **Kata `LockFile`**: verify artifacts against SHA-256 checksums; report swapped, missing and unreviewed files.

---

## 6. Packaging Spring Boot applications

- `spring-boot-maven-plugin` **repackages** the jar into an executable "fat jar": your classes in `BOOT-INF/classes`, every dependency jar in `BOOT-INF/lib`, and a launcher. `java -jar app.jar` just works.
- **Layered jars** for Docker: dependencies change rarely, your code changes every commit. Extracting the jar into layers (`java -Djarmode=tools -jar app.jar extract --layers`) and copying them in that order lets Docker cache the big dependency layer; a new commit only ships a few hundred KB.
- Alternatives: **Buildpacks** (`spring-boot:build-image`, no Dockerfile), **Jib** (images straight from Maven/Gradle, no Docker daemon), **GraalVM native images** (fast startup, small memory, long builds, reflection configuration).

The Docker track goes further.

---

## 7. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `AffectedModules` | monorepo CI, "also make dependents" | reverse graph + BFS + topological order |
| `UpperBounds` | runtime classpath conflicts, the enforcer rule | BFS over the full tree |
| `LockFile` | supply chain, SHA-256 checksums | hashing |

```
cd katas && ./mvnw test -Dgroups="build & L3"
```

## 8. Seen in the shop project

- CI caches Maven dependencies (`setup-java` with `cache: maven`) and builds the image once, then smoke-tests that exact image.
- The Dockerfile extracts the Spring Boot jar into layers (`-Djarmode=tools ... extract --layers`).
- Pinned versions everywhere: Spring Boot parent, wrapper, plugins via the parent, base images by tag.

## 9. Interview talk track

"I treat the build as production code. Maven runs through the committed wrapper on a pinned JDK, plugin and dependency versions are exact, and I set a fixed output timestamp so builds are reproducible. Versions come from a BOM and dependencyManagement; I know Maven resolves conflicts by nearest-wins, so I use dependency:tree when I see NoSuchMethodError and add enforcer rules like requireUpperBoundDeps so conflicts fail the build instead of production. In CI I cache the local repository, build only affected modules on pull requests with -pl and -amd, run independent modules in parallel and keep slow integration tests in a separate stage. Releases are built once and the same artifact or image is promoted through environments. For supply-chain security, everything resolves through a proxy repository, artifacts are checksum-verified, dependencies are scanned continuously with automated update PRs, and we produce an SBOM for each release."

**Questions you should be able to answer:**
1. What makes a build reproducible? Name five things that break reproducibility.
2. Your service throws `NoSuchMethodError` in production but tests passed. Walk through the investigation and the fix.
3. What do `requireUpperBoundDeps` and `dependencyConvergence` check?
4. What is shading, and when is it justified?
5. How would you make a 40-minute monorepo build take 5 minutes on pull requests?
6. Why "build once, deploy many"?
7. What is dependency confusion, and how do you prevent it?
8. What is an SBOM, and what question does it answer quickly?
9. What is inside a Spring Boot fat jar? Why use layered jars in Docker images?
