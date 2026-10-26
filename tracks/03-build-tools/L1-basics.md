# Build tools, L1: Maven from zero

**Goal:** know what happens when you type `./mvnw verify`: where the dependencies come from, what each phase does, why `2.0-SNAPSHOT` is older than `2.0`, and why something can compile and still fail at runtime. Every Java project you join has a build; this level makes it readable.

Katas: `cd katas && ./mvnw test -Dgroups="build & L1"`. Drill: `java quiz/Quiz.java --track build-tools --level 1`. Hands-on: [playground/](playground/).

---

## 1. Why a build tool

Compiling one file is `javac Hello.java`. A real service has hundreds of classes and about a hundred libraries (Spring alone brings dozens), tests, resources, and must produce the same artifact on every laptop and in CI. A build tool:
- **downloads dependencies** (and *their* dependencies) from a repository and puts the right ones on the right classpath;
- runs a fixed sequence of steps (compile, test, package) the same way everywhere;
- produces an **artifact**: a `.jar` (or `.war`), or a Docker image;
- is the one place where the project's structure, Java version and quality checks are written down.

The two in the Java world: **Maven** (XML, convention over configuration, most enterprise and Spring projects) and **Gradle** (Kotlin/Groovy scripts, faster incremental builds, Android). L2 compares them.

---

## 2. The Maven model

Every artifact has **coordinates** ("GAV"):

```xml
<groupId>org.springframework.boot</groupId>   <!-- who: reverse domain -->
<artifactId>spring-boot-starter-web</artifactId>  <!-- what -->
<version>4.1.2</version>                       <!-- which -->
```

A `pom.xml` (Project Object Model) describes your project with the same coordinates plus its dependencies and plugins.

**Convention over configuration:** if you follow the standard layout, you configure almost nothing:

```
pom.xml
src/main/java        production code           -> target/classes
src/main/resources   config files, SQL         -> target/classes
src/test/java        tests                     -> target/test-classes
src/test/resources
target/              everything generated (delete it freely: mvn clean)
```

**Repositories:** Maven downloads artifacts from **Maven Central** (or your company's mirror: Nexus, Artifactory) into your **local repository** `~/.m2/repository`, a cache shared by all your projects, laid out by coordinates: `~/.m2/repository/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar`.

---

## 3. The lifecycle: phases and goals

Maven's default lifecycle is an ordered list of **phases**. Asking for one runs **every phase before it**:

| Phase | What happens (by default) |
|---|---|
| `validate` | the project is correct (enforcer rules often run here) |
| `compile` | compile `src/main/java` |
| `test-compile` | compile `src/test/java` |
| `test` | run unit tests (Surefire: classes named `*Test`) |
| `package` | build the jar/war |
| `verify` | run checks on the package: integration tests (Failsafe: `*IT`), quality gates |
| `install` | copy the artifact into your local `~/.m2` (so other local projects can use it) |
| `deploy` | upload it to a remote repository (release pipelines only) |

Plus `clean` (a separate lifecycle: delete `target/`).

The actual work is done by **plugin goals** bound to phases: `compiler:compile` to `compile`, `surefire:test` to `test`, `jar:jar` to `package`. You can also run a goal directly: `mvn dependency:tree`.

Commands you will use daily:

```
./mvnw verify                      # the full check before you push (what CI runs)
./mvnw test -Dtest=MoneyTest       # one test class
./mvnw package -DskipTests         # just build the jar (careful: you skipped the safety net)
./mvnw clean verify                # from scratch, when something looks stale
./mvnw dependency:tree             # where does this library come from?
```

`mvn install` is rarely needed for a single project; `verify` is the habit.

---

## 4. Versions

- **Release versions** (`4.1.2`) are **immutable**: once published, `4.1.2` must never change. Repositories refuse overwrites.
- **SNAPSHOT versions** (`1.0.0-SNAPSHOT`) mean "the latest development build towards 1.0.0". Maven re-checks them for newer copies. Never depend on someone else's SNAPSHOT in something you release: the build is no longer reproducible.
- **Semantic versioning** (`MAJOR.MINOR.PATCH`): patch = bug fixes, minor = new backwards-compatible features, major = breaking changes. A promise by the library authors, not a guarantee.
- **Maven's ordering is not string ordering:** `1.10 > 1.9` (numbers), `1 == 1.0 == 1.0.0`, and qualifiers sort `alpha < beta < milestone < rc < SNAPSHOT < release < sp`. So `2.0-SNAPSHOT < 2.0` and `2.0-RC1 < 2.0`.
- **Version ranges** (`[1.0,2.0)`) exist but make builds non-reproducible (a new release changes your build overnight). Pin exact versions; update deliberately (Dependabot/Renovate PRs).

> **Kata `MavenVersion`**: parse and order versions the way Maven does.

---

## 5. Dependencies and scopes

```xml
<dependencies>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.14</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

The **scope** decides on which classpaths a dependency appears:

| Scope | compile main | run the app (packaged) | compile tests | run tests | Example |
|---|---|---|---|---|---|
| `compile` (default) | yes | yes | yes | yes | Spring, Jackson |
| `provided` | yes | **no** (the runtime has it) | yes | yes | `jakarta.servlet-api` in a war deployed to Tomcat, Lombok |
| `runtime` | **no** | yes | no | yes | a JDBC driver: your code uses `java.sql`, never `org.postgresql` |
| `test` | no | no | yes | yes | JUnit, AssertJ, Testcontainers |

**Transitive dependencies:** you declare Spring Web; it depends on Spring Core, Jackson, and so on; Maven adds those too. The scope a transitive dependency gets depends on both scopes (the "transitive scope" table): a `compile` dependency's `runtime` dependency is `runtime` for you; anything your `test` dependency brings is `test`; and the `test` and `provided` dependencies of your dependencies are **not** inherited at all.

Two symptoms that come from scopes:
- **Compiles, fails at runtime with `ClassNotFoundException`**: the class was `provided` (or `test`) and the runtime does not actually supply it.
- **Your jar is huge / contains a servlet API**: something that should be `provided` or `test` is `compile`.

> **Kata `Scopes`**: both tables, as code.

---

## 6. The Maven wrapper

`./mvnw` (and `mvnw.cmd` on Windows) is a small script committed to the repository. It downloads the exact Maven version written in `.mvn/wrapper/maven-wrapper.properties` and runs it. Everyone (and CI) uses the same Maven without installing it. Always use the wrapper of a project instead of a globally installed `mvn`. Upgrading Maven for the project = changing one line, reviewed like any other change.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `MavenVersion` | version ordering, SNAPSHOT vs release, qualifiers (parsing + a consistent comparator) |
| `Scopes` | which scope ends up on which classpath; transitive scopes |

```
cd katas && ./mvnw test -Dgroups="build & L1"
```

Then do exercises 1-3 in [playground/README.md](playground/README.md).

## 8. Seen in the shop project

- `pom.xml` of spring-backend-ai: the Spring Boot parent, starters, `runtime` scope for the PostgreSQL driver, `test` scope for Testcontainers.
- `mvnw` + `.mvn/wrapper/maven-wrapper.properties`: the pinned Maven.
- CI runs `./mvnw verify` (`.github/workflows/ci.yml`).

## 9. Interview checklist

1. What does a build tool do for you? Maven vs Gradle in one sentence each.
2. What are Maven coordinates? Where does Maven get artifacts from, and where does it keep them?
3. Name the main lifecycle phases in order. What happens when you run `mvn package`?
4. Phase vs goal vs plugin.
5. SNAPSHOT vs release. Why must releases be immutable? Is `1.10` newer than `1.9` for Maven?
6. The four common scopes, with an example each.
7. Something compiles but fails at runtime with `ClassNotFoundException`. What do you check?
8. Why commit a Maven wrapper?
