# Build tools: question bank

Drill with `java quiz/Quiz.java --track build-tools`.

## L1

### BUILD-L1-001 · recall
What does a build tool do that `javac` alone does not?

<details><summary>Answer</summary>

Downloads dependencies and their transitive dependencies from repositories and builds the right classpaths; runs a fixed, repeatable sequence (compile, test, package, checks) the same way on every machine and in CI; produces versioned artifacts; and is the single, versioned description of the project's structure, Java version and quality gates.

</details>

### BUILD-L1-002 · recall
What are Maven coordinates? Give the coordinates of a real dependency.

<details><summary>Answer</summary>

`groupId:artifactId:version` (GAV), plus optional packaging and classifier. Example: `org.postgresql:postgresql:42.7.14`. The groupId is usually a reverse domain of the publisher, the artifactId the module name. Coordinates also decide the path in a repository: `org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar`.

</details>

### BUILD-L1-003 · recall
Where does Maven download dependencies from, and where does it store them?

<details><summary>Answer</summary>

From remote repositories: Maven Central by default, or a company proxy (Nexus, Artifactory) configured in `settings.xml` or the POM. It caches them in the **local repository** `~/.m2/repository`, shared by all projects on the machine, so each artifact version is downloaded once.

</details>

### BUILD-L1-004 · predict
You run `mvn package`. Which phases run?

<details><summary>Answer</summary>

Every phase of the default lifecycle up to and including package: validate, (initialize, generate/process sources and resources,) compile, (process classes, generate test sources/resources,) test-compile, test, (prepare-package,) package. Not verify, install or deploy. `clean` does not run unless you ask (`mvn clean package`).

</details>

### BUILD-L1-005 · recall
Phase vs goal vs plugin?

<details><summary>Answer</summary>

A **plugin** (maven-surefire-plugin) provides **goals** (`surefire:test`), each a unit of work. A **phase** (`test`) is a step of the lifecycle; goals are **bound** to phases by default (via packaging) or in the POM's `<executions>`. Running a phase runs all earlier phases and their bound goals; you can also run a goal directly (`mvn dependency:tree`).

</details>

### BUILD-L1-006 · recall
What is the difference between `mvn verify` and `mvn install`? Which should you run before pushing?

<details><summary>Answer</summary>

`verify` builds, runs unit and integration tests and checks the package. `install` does all of that and also copies the artifact into your local `~/.m2`, which only matters if another local project must use it. Before pushing: `verify` (what CI runs). Habitual `install` can hide problems: other projects may pick up your unreleased local copy.

</details>

### BUILD-L1-007 · predict
Order these Maven versions from oldest to newest: `2.0`, `1.9`, `2.0-SNAPSHOT`, `1.10`, `2.0-RC1`.

<details><summary>Answer</summary>

`1.9` < `1.10` < `2.0-RC1` < `2.0-SNAPSHOT` < `2.0`. Numbers compare numerically (10 > 9), and qualifiers sort before the release, with SNAPSHOT after rc in Maven's ordering. A version without a qualifier is the release.

</details>

### BUILD-L1-008 · why
What is a SNAPSHOT version, and why must a release never depend on one?

<details><summary>Answer</summary>

`1.0.0-SNAPSHOT` means "the current development state towards 1.0.0"; it can be republished many times and Maven checks for newer copies. A release depending on a SNAPSHOT is not reproducible: building the same tag next week may pull different code. (maven-release-plugin and the enforcer can refuse SNAPSHOT dependencies in releases.)

</details>

### BUILD-L1-009 · recall
What does semantic versioning promise for MAJOR.MINOR.PATCH?

<details><summary>Answer</summary>

PATCH: backwards-compatible bug fixes. MINOR: new backwards-compatible functionality. MAJOR: breaking changes. `0.x` versions make no stability promise. It is a convention the publisher follows (or not); verify with tests and, for libraries, binary-compatibility checks.

</details>

### BUILD-L1-010 · recall
List the four common Maven scopes with an example of each.

<details><summary>Answer</summary>

`compile` (default; everywhere and packaged): Spring, Jackson. `provided` (compile and test, not packaged; the runtime supplies it): servlet API in a war, Lombok. `runtime` (not for compiling main code, needed to run): JDBC drivers. `test` (tests only): JUnit, AssertJ, Testcontainers. (Also `system`, deprecated, and `import`, only for BOMs in dependencyManagement.)

</details>

### BUILD-L1-011 · why
Why is the PostgreSQL JDBC driver usually `runtime` scope?

<details><summary>Answer</summary>

Application code talks to the standard `java.sql`/`javax.sql` interfaces (or JPA), never to `org.postgresql` classes. With `runtime` scope the compiler stops you from accidentally coupling to driver internals, and the driver is still on the runtime and test classpaths where it is loaded by name.

</details>

### BUILD-L1-012 · scenario
The app compiles fine but fails at startup with `ClassNotFoundException: jakarta.servlet.Filter`. What do you check?

<details><summary>Answer</summary>

The class was available at compile time but not at runtime: check the scope (`provided` or `test` instead of `compile`), whether it was excluded, or omitted by conflict mediation (`dependency:tree -Dverbose`), and what is actually inside the built artifact (`jar tf app.jar`). With Spring Boot, a missing starter (e.g. `spring-boot-starter-web`) is the usual cause.

</details>

### BUILD-L1-013 · recall
What are transitive dependencies, and what happens to the `test` dependencies of your dependencies?

<details><summary>Answer</summary>

Dependencies of your dependencies, added automatically by Maven. Their scope is derived from both scopes (e.g. compile → runtime stays runtime; anything under a test dependency becomes test). The `test` and `provided` dependencies of your dependencies are **not** inherited at all: they were only for building that library.

</details>

### BUILD-L1-014 · why
Why commit the Maven wrapper (`mvnw`) into a repository?

<details><summary>Answer</summary>

Everyone and every CI job builds with exactly the Maven version in `.mvn/wrapper/maven-wrapper.properties`, without installing Maven. Different Maven versions can resolve plugins and dependencies differently. Upgrading becomes a reviewed one-line change.

</details>

### BUILD-L1-015 · recall
What is Maven's standard directory layout?

<details><summary>Answer</summary>

`pom.xml`; `src/main/java` (production code) and `src/main/resources` (config, SQL), compiled/copied to `target/classes`; `src/test/java` and `src/test/resources` to `target/test-classes`; everything generated in `target/`. Following it means almost no configuration (convention over configuration).

</details>

### BUILD-L1-016 · why
Why are version ranges like `[1.0,2.0)` discouraged?

<details><summary>Answer</summary>

The resolved version changes whenever a new matching release is published, so the same commit builds differently on different days (non-reproducible, possibly broken overnight, a supply-chain risk). Pin exact versions and update deliberately with automated update pull requests.

</details>

### BUILD-L1-017 · recall
What does `mvn dependency:tree` show, and what does `-Dverbose` add?

<details><summary>Answer</summary>

The resolved dependency tree with versions and scopes: which dependency brings in which. `-Dverbose` also shows nodes that were **omitted** (duplicates, conflicts: "omitted for conflict with 31.0") and version changes by dependencyManagement. Add `-Dincludes=group:artifact` to focus on one library.

</details>

### BUILD-L1-018 · dsa
How would you compare two version strings like "1.10.2" and "1.9.15" in an interview, and what is the complexity?

<details><summary>Answer</summary>

Split on dots, parse each segment as a number, compare segment by segment, treating missing segments as 0; the first difference decides. O(n) in the string length. Pitfalls: string comparison ("1.10" < "1.9" lexically), leading zeros, and qualifiers (rc, SNAPSHOT) that need their own ordering. Make `compareTo` consistent with `equals` (1.0 == 1.0.0).

</details>

### BUILD-L1-019 · recall
What does `mvn clean` do, and when is it needed?

<details><summary>Answer</summary>

Deletes `target/` (the separate clean lifecycle). Normally incremental builds are fine; use it when output looks stale (deleted or renamed classes still in `target/classes`, resources not refreshed), before release builds, and in CI (which usually starts clean anyway).

</details>

### BUILD-L1-020 · predict
`mvn test -Dtest=OrderServiceTest`: what runs?

<details><summary>Answer</summary>

All phases up to `test` (compile, test-compile...), but Surefire runs only the test class `OrderServiceTest` (patterns like `*Service*Test` and `OrderServiceTest#placesOrder` also work). In a multi-module build add `-Dsurefire.failIfNoSpecifiedTests=false`, or modules without that class fail.

</details>

## L2

### BUILD-L2-001 · recall
Parent POM vs aggregator POM?

<details><summary>Answer</summary>

**Parent** (`<parent>` in the child): the child inherits properties, dependencyManagement, pluginManagement, plugins and dependencies. **Aggregator** (`<modules>`): a list of projects to build together in one reactor. Usually the same multi-module root does both, but they are independent: Spring Boot's starter-parent is a parent you do not aggregate with.

</details>

### BUILD-L2-002 · interview
Your app depends on A and B. A needs guava 33, B needs guava 31. How does Maven decide, and how would Gradle?

<details><summary>Answer</summary>

Maven: **nearest wins** in the tree; at equal depth, the **first declared**; so the order of A and B in your POM decides here, and an older version may win. Gradle: by default the **highest** version wins (conflict resolution), with options to fail on conflicts or lock versions. In both you should decide explicitly: Maven `dependencyManagement`/BOM, Gradle constraints/platforms.

</details>

### BUILD-L2-003 · predict
`app -> a -> b -> guava:33` and `app -> c -> guava:31`. Which guava does Maven use?

<details><summary>Answer</summary>

31: it is at depth 2, while 33 is at depth 3. Library `b` was compiled against 33 and may hit `NoSuchMethodError` at runtime. Fix with `dependencyManagement` (guava 33) and add `requireUpperBoundDeps` to catch it.

</details>

### BUILD-L2-004 · recall
`dependencyManagement` vs `dependencies`?

<details><summary>Answer</summary>

`dependencies` adds artifacts to the project. `dependencyManagement` only **decides versions (and scopes/exclusions)** for artifacts if and wherever they appear, directly or transitively, and lets children declare them without a version. Declaring something only in dependencyManagement adds nothing to the classpath.

</details>

### BUILD-L2-005 · recall
What is a BOM and how do you use one in Maven?

<details><summary>Answer</summary>

A "bill of materials": a POM with only a dependencyManagement section listing versions known to work together (Spring Boot's `spring-boot-dependencies`, Jackson, JUnit, cloud SDKs). Import it in your dependencyManagement with `<type>pom</type><scope>import</scope>`; then declare those dependencies without versions. With the Spring Boot parent, its BOM is already inherited.

</details>

### BUILD-L2-006 · scenario
Two logging frameworks end up on your classpath through an old library (commons-logging and SLF4J bridges fight). How do you fix it in Maven?

<details><summary>Answer</summary>

Find who brings it: `dependency:tree -Dincludes=commons-logging`. Add an `<exclusion>` on that dependency, and, if the old API is still used, add the bridge (`jcl-over-slf4j` / Spring's `spring-jcl`) so calls go to one backend. Optionally enforce with `bannedDependencies` so it does not come back.

</details>

### BUILD-L2-007 · recall
What is an optional dependency?

<details><summary>Answer</summary>

`<optional>true</optional>` in a library's POM: used by the library itself, but **not** passed on transitively to its users. For optional integrations (a library that can use Jackson or Gson if present). Users who want that feature declare it themselves.

</details>

### BUILD-L2-008 · recall
Surefire vs Failsafe: what does each run, in which phase, and why does the split matter?

<details><summary>Answer</summary>

Surefire runs unit tests (`*Test`) in `test`. Failsafe runs integration tests (`*IT`) in `integration-test` and checks results in `verify`. The split lets `post-integration-test` steps (stop a server or container) run even when ITs fail, and lets you run fast unit tests alone. Run `mvn verify`, not `mvn integration-test`, so cleanup and the failure check happen.

</details>

### BUILD-L2-009 · why
Why pin plugin versions in `pluginManagement`?

<details><summary>Answer</summary>

If a plugin version is not set, Maven uses a default that depends on the Maven version (and warns). Builds on machines with different Maven versions then differ, and a Maven upgrade silently changes your build. Pinning makes the build reproducible and upgrades explicit. (Parents like spring-boot-starter-parent pin most for you.)

</details>

### BUILD-L2-010 · recall
What do `-pl`, `-am`, `-amd`, `-rf` and `-T` do in a multi-module build?

<details><summary>Answer</summary>

`-pl web` build only the listed project(s); `-am` also make the modules they depend on; `-amd` also make the modules that depend on them; `-rf :web` resume the reactor from that module (after a failure); `-T 1C` build in parallel with one thread per core where the dependency graph allows.

</details>

### BUILD-L2-011 · predict
`<modules>` lists `app` before `core`, and `app` depends on `core`. In which order are they built?

<details><summary>Answer</summary>

`core` first. The reactor sorts modules topologically by their inter-module dependencies (and plugin/parent relationships); the declaration order only breaks ties. A cycle between modules is an error.

</details>

### BUILD-L2-012 · dsa
Explain how to order modules for building given their dependencies, and how to detect a cycle.

<details><summary>Answer</summary>

Topological sort. Kahn: compute each module's number of unbuilt dependencies; queue those with 0; repeatedly take one, emit it, decrement its dependents, enqueue any that reach 0. If fewer modules were emitted than exist, the rest are on or behind a cycle; follow unbuilt dependencies from one of them until a module repeats to print the cycle. O(V + E). Alternative: DFS with "visiting/visited" colours, where meeting a "visiting" node means a cycle.

</details>

### BUILD-L2-013 · why
Why build one artifact for all environments instead of a Maven profile per environment?

<details><summary>Answer</summary>

What you tested must be what you deploy: a "prod" artifact built separately has never been tested. Environment differences (URLs, credentials, feature flags) belong in runtime configuration: environment variables, Spring profiles, config maps, secret managers. Profiles are for build behaviour (extra checks in CI, signing on release).

</details>

### BUILD-L2-014 · recall
What does `maven.compiler.release` do, and why is it better than `source`/`target`?

<details><summary>Answer</summary>

It compiles for a Java version (`21`) **and** checks your code only uses the API of that version (`--release` of javac). With source/target on a newer JDK you could compile code that calls APIs missing on the older runtime, which then fails with `NoSuchMethodError`.

</details>

### BUILD-L2-015 · recall
Gradle `implementation` vs `api` vs `compileOnly` vs `runtimeOnly` vs `testImplementation`?

<details><summary>Answer</summary>

`implementation`: compile and runtime for this module, **not** exposed to consumers' compile classpath. `api` (java-library plugin): like implementation but exposed (part of your public API). `compileOnly`: compile only (≈ provided). `runtimeOnly`: runtime only (≈ runtime). `testImplementation`: tests only. implementation-by-default speeds up builds (less recompilation) and stops consumers from using your internals.

</details>

### BUILD-L2-016 · interview
Maven or Gradle for a new Spring Boot service? Argue both sides.

<details><summary>Answer</summary>

Maven: declarative, standard layout, very predictable, easy for any Java developer to read, tooling everywhere, slower for big builds. Gradle: faster (incremental tasks, build cache, configuration cache), flexible scripting, better dependency modelling (api/implementation, rich version constraints), but builds can become programs that are hard to read and maintain. For a typical service either is fine; follow the organisation's standard. For large monorepos Gradle's caching often wins.

</details>

### BUILD-L2-017 · recall
What are Maven profiles, and how can they be activated?

<details><summary>Answer</summary>

Named blocks of POM configuration (properties, dependencies, plugins, modules) applied only when active. Activation: explicitly `-Pname`, by default (`activeByDefault`, deactivated when another is chosen), by JDK version, OS, a system property (`-Dci`), or the presence of a file. `mvn help:active-profiles` shows what is active.

</details>

### BUILD-L2-018 · scenario
You must use a library that drags in a vulnerable version of jackson-databind transitively. You cannot upgrade the library. What do you do?

<details><summary>Answer</summary>

Override the version with `dependencyManagement` (or the Jackson BOM) to a patched version, then verify with `dependency:tree` and the tests that the library still works with it (same major version is usually fine). If it is incompatible, exclude it and see if the feature using it is needed, or shade/isolate. Record why, so the override is removed when the library catches up.

</details>

### BUILD-L2-019 · recall
What does the Spring Boot Maven plugin add to a plain Maven build?

<details><summary>Answer</summary>

`repackage` (bound to package): turns the jar into an executable fat jar with dependencies in `BOOT-INF/lib` and a launcher; layer metadata for Docker; `spring-boot:run` (run from the build); `build-image` (Buildpacks image without a Dockerfile); AOT processing for native images; build info generation.

</details>

### BUILD-L2-020 · why
Why can a multi-module split (core, persistence, web) be worth its cost, and what is the cost?

<details><summary>Answer</summary>

Benefits: dependency direction is enforced by the compiler (core cannot import web classes because they are not on its classpath), independent reuse, partial builds and tests. Costs: more POMs and versions, more boilerplate, cross-module refactoring friction, sometimes slower builds. A modular monolith with architecture tests (Spring Modulith, ArchUnit) gets much of the benefit inside one module.

</details>

## L3

### BUILD-L3-001 · recall
What does "reproducible build" mean? Name five things that break reproducibility.

<details><summary>Answer</summary>

The same source produces bit-for-bit the same artifact anywhere, any time. Breakers: different JDK or Maven versions; unpinned plugin versions; version ranges, SNAPSHOT or LATEST dependencies; timestamps and file ordering inside archives (set `project.build.outputTimestamp`); repository contents changing; builds depending on environment (time zone, locale, network, local `~/.m2` leftovers).

</details>

### BUILD-L3-002 · scenario
Tests pass, but production throws `NoSuchMethodError: com.google.common.collect.ImmutableList.toImmutableList()`. Walk through the investigation.

<details><summary>Answer</summary>

Some code was compiled against a newer Guava than the one at runtime. (1) Check which Guava is in the real artifact (`jar tf app.jar | grep guava` in BOOT-INF/lib, or the image). (2) `dependency:tree -Dverbose -Dincludes=com.google.guava` to see which versions were requested by whom and which won by nearest-wins. (3) Ask why tests passed: a different classpath in tests (test-scope dependency bringing the newer version), or the code path is not covered. (4) Fix: manage the higher version in dependencyManagement (check compatibility), or upgrade/exclude the library pulling the old one. (5) Prevent: `requireUpperBoundDeps` / `dependencyConvergence` in the enforcer.

</details>

### BUILD-L3-003 · recall
What do the enforcer rules `requireUpperBoundDeps` and `dependencyConvergence` check?

<details><summary>Answer</summary>

`requireUpperBoundDeps`: fails if the resolved version of a dependency is **lower** than a version requested somewhere in the tree (someone may need newer API). `dependencyConvergence`: stricter, fails if any dependency is requested in **more than one** version anywhere in the tree, forcing you to manage every conflict explicitly.

</details>

### BUILD-L3-004 · recall
What is shading (relocation), and when is it justified?

<details><summary>Answer</summary>

Copying a dependency's classes into your own jar under a renamed package (`com.google.common` → `com.mylib.shaded.guava`), so your library uses its private copy and never conflicts with the version your users choose. Justified for widely used libraries with problematic dependencies (Guava, Netty). Costs: bigger jars, duplicated classes, security patches must be re-shaded. Applications rarely need it.

</details>

### BUILD-L3-005 · scenario
A monorepo build takes 40 minutes on every pull request. How do you bring it down?

<details><summary>Answer</summary>

Measure first (which modules and tests take the time). Then: cache `~/.m2` (or Gradle caches) in CI; build only affected modules on PRs (`-pl changed -amd`, or a tool like Gradle/Bazel/Nx with a dependency graph), full build on main; parallelise modules (`-T 1C`) and tests (forks, CI matrix sharding); a remote build cache; split slow integration tests into a separate parallel job; delete or speed up the slowest tests (Testcontainers reuse, fewer Spring contexts).

</details>

### BUILD-L3-006 · dsa
Given module dependencies and a set of changed modules, which modules must be rebuilt? Algorithm and complexity.

<details><summary>Answer</summary>

Build the **reverse** graph (module → modules that depend on it). BFS or DFS from all changed modules over the reverse edges; everything reached is affected. Order the affected set topologically for building. O(V + E). Modules not reachable cannot be affected by the change (assuming the graph is complete: watch out for undeclared dependencies like shared config files or generated code).

</details>

### BUILD-L3-007 · why
What does "build once, deploy many" mean, and why does it matter?

<details><summary>Answer</summary>

Build the artifact (jar or image) once in CI, tag it immutably (version or commit SHA, image digest), and promote that same artifact through test, staging and production with environment-specific runtime configuration. Rebuilding per environment would deploy something that was never tested, and could differ because dependencies or tools changed in between.

</details>

### BUILD-L3-008 · recall
What is dependency confusion, and how do you prevent it?

<details><summary>Answer</summary>

An attacker publishes a package on a public repository with the same name as your **internal** package and a higher version; a build that searches both repositories picks the attacker's. Prevent: resolve everything through one internal repository proxy that serves internal group ids only from the internal repo (no fallthrough to public for them), reserve your group id / namespace publicly, pin versions and verify checksums.

</details>

### BUILD-L3-009 · recall
What is an SBOM, and what question does it answer?

<details><summary>Answer</summary>

A software bill of materials: a machine-readable inventory (CycloneDX, SPDX) of every component and version inside an artifact or image, generated by the build (cyclonedx-maven-plugin) or image scanners. It answers "which of our deployed services contain library X version Y?" in minutes when a new CVE appears (e.g. Log4Shell).

</details>

### BUILD-L3-010 · design
List the controls that protect a Java build pipeline against supply-chain attacks.

<details><summary>Answer</summary>

A single internal repository proxy; exact versions and checksum (and where possible signature) verification of dependencies and plugins; the build tool via a pinned wrapper with a verified distribution checksum; automated vulnerability scanning of dependencies and images that fails on critical issues; automated small dependency updates; SBOMs per release; CI least privilege (no secrets for untrusted PRs, short-lived credentials, protected release jobs); signed artifacts and build provenance (SLSA); review of new dependencies.

</details>

### BUILD-L3-011 · recall
What is inside a Spring Boot executable jar, and how does it start?

<details><summary>Answer</summary>

`BOOT-INF/classes` (your code and resources), `BOOT-INF/lib` (all dependency jars, unmodified), `META-INF/MANIFEST.MF` with `Main-Class` = Spring Boot's launcher (`JarLauncher`) and `Start-Class` = your `@SpringBootApplication`, and the loader classes. `java -jar` runs the launcher, which builds a class loader that can read nested jars, then calls your main method.

</details>

### BUILD-L3-012 · why
Why do Docker images of Spring Boot apps use layered jars?

<details><summary>Answer</summary>

Docker caches image layers. If the whole fat jar is one layer, every code change ships all dependencies again (often 50-100 MB) to the registry and every node. Extracting into layers (dependencies, spring-boot-loader, snapshot dependencies, application) and copying them in that order means a typical commit changes only the small application layer: faster pushes, pulls and deploys.

</details>

### BUILD-L3-013 · scenario
Two jars on the classpath contain the same class with different code. What symptoms appear and how do you detect it in the build?

<details><summary>Answer</summary>

Which version is loaded depends on classpath order, which can differ between IDE, tests and the packaged app: bugs that only appear in one environment, `NoSuchMethodError`, `LinkageError`. Typical causes: a library and its renamed fork (e.g. `javax.*` vs `jakarta.*`, old `bcprov` artifacts), or shaded uber-jars. Detect with the extra-enforcer-rules `banDuplicateClasses`, or tools like the duplicate-finder plugin; fix with exclusions.

</details>

### BUILD-L3-014 · recall
What does a lock file (or dependency verification metadata) protect against that pinned versions alone do not?

<details><summary>Answer</summary>

A pinned version names the artifact; it does not prove the **bytes** are the reviewed ones. A compromised or misconfigured repository/proxy could serve a different file under the same coordinates. Checksums (SHA-256) of every artifact in a lock file make the build fail when bytes differ; signatures additionally prove who published them. Lock files also pin transitive versions exactly.

</details>

### BUILD-L3-015 · recall
How do you cache Maven dependencies in GitHub Actions, and what is the cache key?

<details><summary>Answer</summary>

`actions/setup-java` with `cache: maven` (or `actions/cache` on `~/.m2/repository`). The key is a hash of all `pom.xml` files (plus OS), so the cache is reused until dependencies change, then rebuilt. Do not cache `target/` across branches blindly; and do not cache your own SNAPSHOT artifacts installed into `~/.m2`.

</details>

### BUILD-L3-016 · interview
How would you release a library to Maven Central or an internal repository?

<details><summary>Answer</summary>

Decide the version (semantic versioning; check binary compatibility for minor/patch), set it (no SNAPSHOT), run the full build and tests once in CI, attach sources and javadoc jars, sign the artifacts (required for Central), deploy to the repository (staging then release for Central), tag the commit, publish release notes, bump to the next SNAPSHOT. Never overwrite a released version; publish a new one.

</details>

### BUILD-L3-017 · why
Why should a CI build start from a clean state, and why can a developer's local build be misleading?

<details><summary>Answer</summary>

A local `~/.m2` may contain artifacts installed from unreleased branches, stale SNAPSHOTs, or files that are no longer in any repository, and `target/` may contain stale classes. The build succeeds locally but not for anyone else. CI with a clean workspace (only a cache of downloaded release artifacts) proves the build works from the repository alone.

</details>

### BUILD-L3-018 · recall
What is a binary-incompatible change in a Java library? Give two examples that are source-compatible but break binaries.

<details><summary>Answer</summary>

A change after which already-compiled callers fail at link time without recompiling. Examples: changing a method's return type from `List` to `ArrayList` (callers compiled against the old descriptor get `NoSuchMethodError`), changing a parameter from `int` to `long`, changing a class to an interface (`IncompatibleClassChangeError`), moving a method to a superclass in some cases. Tools that check this in CI: japicmp, revapi.

</details>

### BUILD-L3-019 · scenario
A critical CVE is announced in a popular logging library on a Friday. How does a well-run build setup help you respond?

<details><summary>Answer</summary>

SBOMs or dependency scans tell you within minutes which services and versions are affected. Because versions are managed centrally (BOM, dependencyManagement) and updates are routine (Renovate/Dependabot), the fix is a one-line version bump that CI validates. Build once, deploy many plus automated deploys ship it quickly and safely. Without these, teams grep jars by hand across dozens of repositories.

</details>

### BUILD-L3-020 · why
Why can parallel builds (`-T`) or parallel tests expose bugs that sequential builds hide?

<details><summary>Answer</summary>

Hidden shared state: modules or tests writing the same files or ports, plugins that are not thread-safe, tests depending on execution order or on static state left by others, undeclared module dependencies that happened to be satisfied by sequential order. Fix the isolation (unique temp dirs, random ports, no static mutable state, declared dependencies), rather than turning parallelism off.

</details>
