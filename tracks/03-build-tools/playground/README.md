# Build-tools playground

A real two-module Maven project. Run everything from the **repository root** with the katas' wrapper:

```
katas/mvnw -f tracks/03-build-tools/playground verify
```

```
playground/pom.xml        parent (properties, dependencyManagement with the JUnit BOM, pluginManagement,
                          enforcer rules, a "quick" profile) AND aggregator (<modules>)
  shop-core/              a record Price, no dependencies
  shop-app/               depends on shop-core; a Main class
```

## Exercises

Write down your prediction before each command. (L1 = exercises 1-3, L2 = 4-8, L3 = 9-11.)

1. **Lifecycle.** Run `katas/mvnw -f tracks/03-build-tools/playground package` and read which plugin goals run in which order (`--- compiler:...`, `--- surefire:...`, `--- jar:...`). Then run `validate`. Which goals run now, and why?
2. **Where things go.** After `package`, find the compiled classes, the test reports and the jar under `target/` of each module. Run the app: `java -cp tracks/03-build-tools/playground/shop-app/target/classes:tracks/03-build-tools/playground/shop-core/target/classes dev.dojo.app.Main`. What happens if you leave out shop-core's classes?
3. **Scopes.** Run `katas/mvnw -f tracks/03-build-tools/playground dependency:tree`. Which scope does junit-jupiter have, and what does it bring along? Is any of it inside `shop-app-1.0.0-SNAPSHOT.jar` (`jar tf`)?
4. **The reactor reorders.** `<modules>` lists shop-app BEFORE shop-core. Look at "Reactor Build Order" in the output. Why is shop-core first?
5. **Partial builds.** Run `... -pl shop-app -am verify`, then `... -pl shop-core -amd verify`. Which modules are built each time?
6. **Make a cycle.** Add shop-app as a dependency of shop-core. What does Maven say? Remove it again.
7. **dependencyManagement vs dependencies.** Remove the `<version>` handling of shop-core from the parent's dependencyManagement. What fails, and why did shop-app's dependency need no version before?
8. **A profile.** Run `... verify -Pquick`. Did the tests run? Find the property the profile changes.
9. **Reproducible jars.** Run `clean verify` twice and compare `sha256sum shop-core/target/*.jar`. Now remove `project.build.outputTimestamp` from the parent and repeat. Why do the hashes differ now?
10. **Enforcer.** Change `requireJavaVersion` to `[99,)`. In which phase does the build stop, and how quickly? Put it back.
11. **Dependency hell for real.** In shop-app, add `org.junit.jupiter:junit-jupiter-api` version `5.10.0`, scope `test`, as a direct dependency, and run `verify`. The tests crash with a `NoClassDefFoundError` and a hint about "JUnit jars not properly aligned": your direct 5.10.0 won by nearest-wins, while the BOM gives every other JUnit jar 6.0.3. Questions: (a) `dependency:tree -Dverbose` shows which jar lost to which. (b) Why did the enforcer's `requireUpperBoundDeps` rule stay green? (Read the rule's documentation for `excludedScopes`.) (c) What is the fix? (Never put a version on a dependency that a BOM manages.) Remove the dependency again.
