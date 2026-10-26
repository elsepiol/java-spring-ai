# Track 01: Java core

The language and the JVM underneath everything else in the dojo.

| Level | Lesson | Katas (`katas/src/main/java/dojo/javacore/`) | Examples |
|---|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): JDK/JVM, types, Strings, pass-by-value, boxing, OOP, equals/hashCode, Big-O and hashing, exceptions | `Money`, `BugHunt`, `Hashing` (DSA: two-sum, first unique), `ConfigReader` | FloatingMoney, IntegerCache, PassByValue, StringPool, HashCodeContract |
| L2 Practical | [L2-practical.md](L2-practical.md): collections and HashMap internals, LinkedHashMap/TreeMap/heaps, generics and PECS, streams, records/sealed/pattern matching | `LruCache` (DSA), `TopK` (DSA: heap), `OrderReport`, `Generics`, `Expr` (tree recursion) | FailFastIteration, StreamLaziness |
| L3 Production | [L3-production.md](L3-production.md): JIT and class loading, memory areas, GC, memory leaks, diagnostics (jcmd, JFR, heap dumps), caching, streaming, performance, Java 8-21 | `ActiveCarts` (find the leak), `ExpiringCache`, `KWayMerge` (DSA: k-way merge) | HeapAndGc |

```
java tracks/01-java-core/examples/IntegerCache.java     # run an example (from the repo root)
cd katas && ./mvnw test -Dgroups="javacore & L1"         # the katas of a level
java quiz/Quiz.java --track java-core --level 1          # drill the questions
```

Question bank: [questions.md](questions.md) (62 questions).
