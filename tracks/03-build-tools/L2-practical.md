# Build tools, L2: dependency management, plugins, multi-module builds, Gradle

**Goal:** control which versions end up on the classpath and why, structure a multi-module project, configure plugins the way real projects do, and read a Gradle build when you meet one.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="build & L2"`. Drill: `java quiz/Quiz.java --track build-tools --level 2`. Hands-on: [playground/README.md](playground/README.md), exercises 4-8.

---

## 1. Parent POM vs aggregator

Two different ideas that usually live in the same file:
- **Inheritance (`<parent>`):** a child inherits properties, `dependencyManagement`, `pluginManagement`, plugins and dependencies from its parent. `spring-boot-starter-parent` is a parent: it gives you Java settings, plugin versions and the versions of hundreds of libraries.
- **Aggregation (`<modules>`):** "build these projects together". The parent POM of a multi-module project lists its modules; running Maven there builds all of them in one **reactor**.

A child refers to its parent by coordinates (and by `relativePath`, default `../pom.xml`). A parent has `<packaging>pom</packaging>` and no code.

---

## 2. Which version wins: mediation, management, BOMs

When the tree contains the same `groupId:artifactId` with several versions, Maven keeps **one**, using **nearest wins**:
- the version closest to your project in the tree wins (fewest hops);
- at equal distance, the one declared first wins;
- the losers are **omitted together with their own subtrees**.

This is not "newest wins" (Gradle's default). So an older version can win just because it is closer, while a library deeper in the tree was compiled against a newer one: a `NoSuchMethodError` at runtime (L3).

`mvn dependency:tree -Dverbose` shows the losers: `(com.google.guava:guava:33.4 - omitted for conflict with 31.0)`.

**Taking control:**

```xml
<!-- 1. dependencyManagement: decides the VERSION wherever the artifact appears (direct or transitive). Adds nothing by itself. -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>33.4.0-jre</version>
    </dependency>
    <!-- 2. A BOM (bill of materials): import a whole curated set of versions -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>4.1.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.example</groupId>
    <artifactId>old-client</artifactId>
    <version>1.2</version>
    <!-- 3. exclusions: cut a transitive dependency you do not want (e.g. it brings a second logging framework) -->
    <exclusions>
      <exclusion>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
      </exclusion>
    </exclusions>
  </dependency>
</dependencies>
```

- Declaring a dependency directly also wins (depth 1), but `dependencyManagement` states the intent without pretending you use the library.
- BOMs are why a Spring Boot project declares `spring-boot-starter-web` **without a version**: Boot's BOM chooses versions of Spring, Jackson, Hibernate, Tomcat, Micrometer... that were tested together.
- `<optional>true</optional>` on a dependency means "my users do not get this transitively" (used by libraries with optional integrations).

> **Kata `DependencyMediation`**: nearest-wins with BFS, losers' subtrees omitted, managed versions override.

---

## 3. Plugins

All work in Maven is done by plugins. Configure them in `<build><plugins>`, and pin their versions in `<pluginManagement>` (an unpinned plugin version depends on the Maven version: a different build on a different machine).

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <excludedGroups>slow</excludedGroups>        <!-- skip tests tagged @Tag("slow") -->
  </configuration>
</plugin>
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>                                  <!-- bind a goal to a phase -->
      <id>enforce</id>
      <goals><goal>enforce</goal></goals>        <!-- default phase: validate -->
      <configuration><rules><requireJavaVersion><version>[21,)</version></requireJavaVersion></rules></configuration>
    </execution>
  </executions>
</plugin>
```

Plugins you will meet:

| Plugin | Job |
|---|---|
| compiler | `javac`; set `<maven.compiler.release>21</maven.compiler.release>` (not source/target: `release` also checks the JDK API) |
| surefire / failsafe | unit tests in `test` (`*Test`) / integration tests in `integration-test` + `verify` (`*IT`), so a failing IT still lets cleanup run |
| jar / war | package |
| spring-boot-maven-plugin | repackage into an executable "fat jar" with all dependencies; build images (`spring-boot:build-image`); run (`spring-boot:run`) |
| enforcer | fail early: Java/Maven versions, banned dependencies, upper bounds, convergence |
| jacoco | test coverage |
| dependency | `tree`, `analyze` (declared but unused / used but undeclared) |
| versions | `display-dependency-updates` |
| flyway | run migrations from the build |

---

## 4. Multi-module builds and the reactor

```
shop-parent/          pom (parent + aggregator)
  shop-core/          domain, no framework
  shop-persistence/   depends on core
  shop-web/           depends on core
  shop-app/           depends on web + persistence; the runnable Spring Boot app
```

Why split: enforce dependency direction (core cannot import web: it is not on its classpath), reuse modules, build and test parts separately. Cost: more POMs and versions to manage. (A modular monolith with package-level checks, like the shop project's Spring Modulith test, is often enough.)

The **reactor** sorts modules so each is built after its dependencies, whatever order `<modules>` lists them in (a topological sort), and refuses cycles ("The projects in the reactor contain a cyclic reference").

```
./mvnw verify                       # everything, in dependency order
./mvnw -pl shop-web -am verify      # shop-web and what it needs ("also make")
./mvnw -pl shop-core -amd verify    # shop-core and everything that depends on it ("also make dependents")
./mvnw verify -rf :shop-web         # resume from shop-web after a failure
./mvnw -T 1C verify                 # parallel: one thread per CPU core, where the graph allows
```

Inside one reactor, modules see each other's freshly built classes; no `install` needed.

> **Kata `BuildOrder`**: Kahn's algorithm, alphabetical among ready modules, and a cycle reported as a path.

---

## 5. Properties, profiles, resources

- **Properties** `<properties><guava.version>33.4</guava.version></properties>` and `${guava.version}`; overridable from the command line: `-Dguava.version=...`. Built-ins: `${project.version}`, `${project.basedir}`.
- **Profiles** switch configuration: `<profile><id>ci</id>...</profile>`, activated with `-Pci` or automatically (by JDK, OS, a property, a file). Good for build *behaviour* (run slow tests in CI, sign artifacts on release). Bad for producing *different artifacts per environment* (a "prod" jar and a "test" jar): build **one** artifact and configure it at runtime (environment variables, Spring profiles; Docker track).
- **Resource filtering** replaces `${...}` in resource files at build time. Useful for the build version; never for secrets.

---

## 6. Gradle in one page

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "4.1.2"
}
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }   // Gradle can download the JDK
repositories { mavenCentral() }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))   // a BOM
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

| Maven | Gradle |
|---|---|
| `compile` scope | `implementation` (not exposed to consumers' compile classpath) or `api` (exposed; `java-library` plugin) |
| `provided` | `compileOnly` |
| `runtime` | `runtimeOnly` |
| `test` | `testImplementation` |
| lifecycle phases | a graph of **tasks** (`./gradlew build`, `test`, `bootJar`); tasks declare inputs and outputs |
| nearest wins | **highest version wins** by default (conflict resolution), configurable; `dependencyLocking` and `failOnVersionConflict()` available |
| `mvnw` | `gradlew` (the Gradle wrapper) |
| re-runs everything each time | **incremental**: up-to-date tasks are skipped; local and remote **build cache** |

`implementation` vs `api` is a real advantage: changing an `implementation` dependency of a library does not force recompiling its consumers, and consumers cannot accidentally use it.

---

## 7. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `DependencyMediation` | nearest wins, omitted subtrees, dependencyManagement | BFS over a tree |
| `BuildOrder` | reactor order, cycle detection | topological sort (Kahn), finding a cycle |

```
cd katas && ./mvnw test -Dgroups="build & L2"
```

## 8. Seen in the shop project

- `spring-boot-starter-parent` as the parent, starters without versions (Boot's BOM).
- `spring-boot-maven-plugin` builds the executable, layered jar that the Dockerfile unpacks.
- A Maven profile for exercises (`-Pexercises`) that changes which tests run, but not the artifact.

## 9. Interview checklist

1. Parent POM vs aggregator POM.
2. Two libraries need different versions of the same dependency. How does Maven choose, and how does Gradle choose?
3. `dependencyManagement` vs `dependencies`. What is a BOM and how do you import one?
4. How do you find out why a certain version of a library is on your classpath?
5. When do you use an exclusion? What is an optional dependency?
6. Surefire vs Failsafe, and why integration tests run in `verify`.
7. What does `-pl x -am` do? `-amd`? How does the reactor order modules?
8. Why pin plugin versions?
9. Why should one artifact be built for all environments instead of a profile per environment?
10. Gradle `implementation` vs `api`; `compileOnly` vs `runtimeOnly`.
