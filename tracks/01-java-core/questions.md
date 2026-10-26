# Java core: question bank

Drill with `java quiz/Quiz.java --track java-core` (add `--level 1`, `--kind predict,bug`, ...).
Reading here works too: answer out loud first, then open the answer.

Kinds: **recall** (facts), **why** (reasons), **predict** (what does it print), **bug** (find it), **scenario** (production situations), **design**, **interview** (open questions, answer like in an interview), **dsa**.

## L1

### JAVA-L1-001 · recall
What are the JDK, the JRE and the JVM, and which one do you need to build a project?

<details><summary>Answer</summary>

- **JVM**: the virtual machine that runs bytecode (interprets it, JIT-compiles hot code).
- **JRE**: the runtime (JVM + standard library) without development tools. No separate download since Java 11.
- **JDK**: the JRE plus `javac`, `jar`, `jcmd`, `jstack`, etc.

You need a **JDK** to build. Bytecode is portable because every platform has its own JVM.

</details>

### JAVA-L1-002 · predict
What does this print?

```java
Integer a = 127, b = 127, c = 128, d = 128;
System.out.println((a == b) + " " + (c == d) + " " + c.equals(d));
```

<details><summary>Answer</summary>

`true false true`. Autoboxing uses `Integer.valueOf`, which caches -128..127, so `a` and `b` are the same object. 128 creates two objects, and `==` compares references. `equals` compares values. Never compare wrappers with `==`.

</details>

### JAVA-L1-003 · predict
What does this print?

```java
String a = "shop";
String b = new String("shop");
String c = "sh";
String d = c + "op";
System.out.println((a == b) + " " + (a == d) + " " + a.equals(d) + " " + (a == d.intern()));
```

<details><summary>Answer</summary>

`false false true true`. `new String` always creates a new object; `c + "op"` is computed at runtime (c is not a constant) so it is a new object too. `equals` compares text. `intern()` returns the pooled instance, which is the literal `a`.

</details>

### JAVA-L1-004 · why
Why is Java called "pass-by-value" even though methods can change the objects you pass?

<details><summary>Answer</summary>

The method receives a **copy of the reference**. Both references point to the same object, so the method can change the object's state. But reassigning the parameter (`list = new ArrayList<>()`) changes only the local copy; the caller's variable still points to the original. Java passes references by value; it has no pass-by-reference (unlike C++ `&` or C# `ref`).

</details>

### JAVA-L1-005 · bug
What is wrong, and what is the fix?

```java
double average(int[] xs) {
    int sum = 0;
    for (int x : xs) sum += x;
    return sum / xs.length;
}
```

<details><summary>Answer</summary>

Three problems:
1. `sum / xs.length` is **integer division**: `[1, 2]` gives 1.0, not 1.5. Fix: `(double) sum / xs.length`.
2. `sum` is an `int` and can **overflow** silently for large values. Use `long`.
3. An empty array throws `ArithmeticException: / by zero` (integer division). Decide what to return and check first.

</details>

### JAVA-L1-006 · why
Why must you never store money in a `double`? What do you use instead?

<details><summary>Answer</summary>

`double` is binary floating point: most decimal fractions (0.1, 0.2) cannot be represented exactly, so sums drift (`0.1 + 0.2 = 0.30000000000000004`, a thousand 0.10s is 99.9999...). Use **`long` cents** (minor units) for amounts, or **`BigDecimal` created from a String** (`new BigDecimal("19.99")`), with an explicit rounding mode for division and tax. Note `BigDecimal.equals` compares scale too (2.0 vs 2.00); use `compareTo` for value equality.

</details>

### JAVA-L1-007 · recall
State the `equals`/`hashCode` contract. Which direction is required, which is not?

<details><summary>Answer</summary>

`equals` must be reflexive, symmetric, transitive, consistent, and `x.equals(null)` is false. **If `a.equals(b)` then `a.hashCode() == b.hashCode()`** (required). The reverse is not required: unequal objects may share a hash code (a collision), which only costs performance.

</details>

### JAVA-L1-008 · scenario
A class overrides `equals` but not `hashCode`. Objects of it are put in a `HashSet`. What do users of the set observe?

<details><summary>Answer</summary>

`contains(equalObject)` usually returns **false** and duplicates are **not** removed: the set looks in the bucket of the new object's (identity-based) hash code, which is almost always a different bucket from the stored equal object. It "works" only by accident when two hashes happen to land in the same bucket, which makes the bug intermittent. Fix: override both, or use a record.

</details>

### JAVA-L1-009 · recall
Checked vs unchecked exceptions: what is the difference, and give two examples of each?

<details><summary>Answer</summary>

**Checked** (subclasses of `Exception` but not `RuntimeException`): the compiler forces you to catch or declare them. Examples: `IOException`, `SQLException`. **Unchecked** (`RuntimeException` and subclasses, plus `Error`): no compiler check. Examples: `NullPointerException`, `IllegalArgumentException`, `IllegalStateException`. Use checked for recoverable conditions the caller must think about; unchecked for programming errors and for errors handled centrally (Spring's style).

</details>

### JAVA-L1-010 · bug
What is wrong with this code?

```java
try {
    process(order);
} catch (Exception e) {
}
```

<details><summary>Answer</summary>

It **swallows** every exception, including bugs (`NullPointerException`) and failures that leave `order` half-processed. Nobody will ever know it failed; the system silently corrupts data. Catch only what you can handle, log with context and the exception (`log.error("processing order {} failed", id, e)`), and usually rethrow or wrap with the cause.

</details>

### JAVA-L1-011 · recall
What does try-with-resources do, what must the resource implement, and in which order are multiple resources closed?

<details><summary>Answer</summary>

It calls `close()` on each resource automatically when the block ends, normally or by exception. Resources must implement `AutoCloseable` (or `Closeable`). Multiple resources are closed in **reverse order** of declaration. If `close()` also throws while an exception is propagating, the close exception is attached as a **suppressed** exception (`e.getSuppressed()`), not lost.

</details>

### JAVA-L1-012 · predict
What does this print?

```java
static int f() {
    try { return 1; }
    finally { System.out.print("finally "); }
}
System.out.println(f());
```

<details><summary>Answer</summary>

`finally 1`. The return value 1 is computed, then `finally` runs, then the method returns. (If `finally` itself contained `return 2`, the method would return 2 and swallow any exception from `try`, which is why you never return from `finally`.)

</details>

### JAVA-L1-013 · recall
Overloading vs overriding: which is decided at compile time and which at runtime?

<details><summary>Answer</summary>

**Overloading** (same name, different parameter lists) is resolved at **compile time** from the declared types of the arguments. **Overriding** (a subclass redefines an inherited method with the same signature) is resolved at **runtime** from the actual object's class (dynamic dispatch). `@Override` makes the compiler verify that you really override.

</details>

### JAVA-L1-014 · interview
Interface or abstract class: how do you choose? Mention what each can hold.

<details><summary>Answer</summary>

An **interface** is a contract: abstract methods, `default` and `static` methods (Java 8), private methods (9), constants, no instance state or constructors. A class can implement many. An **abstract class** can hold state, constructors and protected helpers, but a class extends only one. Default to interfaces (they keep implementations swappable, which is what dependency injection relies on); use an abstract class when several implementations genuinely share state or a template method.

</details>

### JAVA-L1-015 · why
Why prefer composition over inheritance? Give a concrete risk of inheritance.

<details><summary>Answer</summary>

Inheritance couples the subclass to the superclass's implementation details. Classic example: extending `HashSet` and overriding `add` and `addAll` to count insertions double-counts, because `HashSet.addAll` internally calls `add`. A parent change can break all children ("fragile base class"). Composition (holding the other object in a field and delegating) depends only on its public contract, and lets you swap the part at runtime, which is exactly how Spring wires services together.

</details>

### JAVA-L1-016 · recall
What does `final` mean on a variable, a method, a class? Does a `final` field make an object immutable?

<details><summary>Answer</summary>

Variable: cannot be reassigned. Method: cannot be overridden. Class: cannot be extended (`String` is final). A final field that holds a mutable object (a `List`, an array) can still have that object changed: `final` fixes the reference, not the contents. Immutability needs final fields AND no way to change what they point to (copy collections in, do not expose them).

</details>

### JAVA-L1-017 · dsa
Given an array of n integers, return whether any value appears twice. Give an O(n) approach and its space cost. What is the O(1)-space alternative?

<details><summary>Answer</summary>

Walk once, adding to a `HashSet`; if `add` returns false, you found a duplicate. O(n) time, O(n) space. Alternative with O(1) extra space: sort in place (O(n log n)) and compare neighbours. The trade-off between extra memory and time comes up in almost every hashing problem.

</details>

### JAVA-L1-018 · predict
What happens?

```java
Map<String, Integer> stock = new HashMap<>();
int mugs = stock.get("mug");
```

<details><summary>Answer</summary>

`NullPointerException`. `get` returns `null` for a missing key, and assigning it to an `int` auto-unboxes it (`null.intValue()`). Use `getOrDefault("mug", 0)` or keep it as `Integer` and check. Java 14+ "helpful NullPointerException" messages name the exact expression that was null.

</details>

### JAVA-L1-019 · recall
What are the four access levels in Java, from most to least restrictive?

<details><summary>Answer</summary>

`private` (same class), package-private / no modifier (same package), `protected` (same package plus subclasses), `public` (everyone). Start with the most restrictive; package-private is underused and great for keeping implementation classes inside a feature package.

</details>

### JAVA-L1-020 · bug
Why can this method change the caller's data unexpectedly?

```java
List<String> withTag(List<String> tags, String tag) {
    List<String> result = tags;
    result.add(tag);
    return result;
}
```

<details><summary>Answer</summary>

`result = tags` copies the **reference**, not the list. Both names point to the same list, so the caller's list gets the tag too. (And if the caller passed `List.of(...)`, `add` throws `UnsupportedOperationException`.) Fix: `new ArrayList<>(tags)` before adding.

</details>

### JAVA-L1-021 · scenario
Three orders of 10,000,000.00 EUR are summed in an `int` of cents. What is the result and why is it dangerous?

<details><summary>Answer</summary>

10 million euros = 1,000,000,000 cents; three of them = 3,000,000,000, which exceeds `Integer.MAX_VALUE` (2,147,483,647). The `int` **silently wraps** to a negative number (-1,294,967,296). No exception, just a wrong total, possibly written to a database or an invoice. Use `long`, and `Math.addExact` where an overflow must be an error.

</details>

## L2

### JAVA-L2-001 · recall
Walk through `HashMap.put(key, value)` step by step.

<details><summary>Answer</summary>

1. Compute `key.hashCode()` and spread it (`h ^ (h >>> 16)`).
2. Bucket index = `hash & (table.length - 1)` (table length is a power of two).
3. In that bucket, look for an entry whose key `equals` the new key: replace its value and return the old one.
4. Otherwise add a new node (list, or tree if the bucket was treeified).
5. If size exceeds capacity × load factor (0.75), resize: double the table and redistribute.
6. A bucket with 8+ entries becomes a red-black tree if the table has at least 64 buckets (otherwise the table is resized instead).

</details>

### JAVA-L2-002 · why
Why does `HashMap` use a power-of-two table size and spread the hash with `h ^ (h >>> 16)`?

<details><summary>Answer</summary>

With a power-of-two length, `hash & (length - 1)` is a cheap modulo. But it only uses the **low bits** of the hash, so hash codes that differ only in their high bits would all collide. XOR-ing the high 16 bits into the low 16 makes the high bits influence the bucket.

</details>

### JAVA-L2-003 · scenario
You put a key object into a `HashMap`, then change a field that `hashCode` uses. What happens on `get`, `remove` and `size`?

<details><summary>Answer</summary>

The entry stays in the bucket computed from the **old** hash. `get(sameObject)` computes the new hash, looks in a different bucket, and returns `null`. `remove` fails the same way. `size` still counts the entry: it is unreachable through the map's API but still referenced, so it can never be freed: a memory leak. Keys must be immutable (records, Strings, boxed numbers).

</details>

### JAVA-L2-004 · interview
`ArrayList` vs `LinkedList`: which is faster for (a) get by index, (b) add at the end, (c) insert in the middle, (d) iterate? What do you use in practice?

<details><summary>Answer</summary>

(a) ArrayList O(1), LinkedList O(n). (b) both O(1) (ArrayList amortised). (c) both O(n) in practice: ArrayList shifts elements (a fast `System.arraycopy`), LinkedList must first walk to the position. (d) both O(n), but ArrayList is much faster because its array is contiguous and cache-friendly, while LinkedList chases pointers to scattered nodes. In practice: `ArrayList` for lists, `ArrayDeque` for queues and stacks, `LinkedList` almost never.

</details>

### JAVA-L2-005 · design
Design an LRU cache with O(1) `get` and `put`. What are the data structures and what does each operation do?

<details><summary>Answer</summary>

A `HashMap<K, Node>` for O(1) lookup plus a **doubly linked list** of nodes in recency order (sentinel head and tail).
- `get`: look up the node; if present, unlink it and link it right after head (most recent); return its value.
- `put`: if the key exists, update the value and move it to the front. Otherwise, if at capacity, remove `tail.prev` (least recent) from the list and the map; then create a node, link it at the front, put it in the map.

Each step touches a constant number of pointers. In Java you can also use `LinkedHashMap` with `accessOrder = true` and `removeEldestEntry`; in production, Caffeine.

</details>

### JAVA-L2-006 · dsa
You need the 100 most frequent search terms out of 50 million queries. Describe an approach with its time and memory cost.

<details><summary>Answer</summary>

Count with a `HashMap<String, Long>` (O(n) time, O(m) memory for m distinct terms). Then keep a **min-heap of size 100** ordered by count: for each term, offer it, and poll the smallest when the heap exceeds 100. O(m log 100). Result: pop the heap and reverse. If even the counts do not fit in memory: partition terms by hash into files, count each file separately, merge the per-file top-100s; or use an approximate sketch (Count-Min Sketch + heap) as streaming systems do.

</details>

### JAVA-L2-007 · predict
Does this compile? If not, why, and how do you fix the signature?

```java
static double sum(List<Number> xs) { ... }
List<Integer> ints = List.of(1, 2, 3);
sum(ints);
```

<details><summary>Answer</summary>

It does **not** compile: `List<Integer>` is not a `List<Number>`. Generics are invariant; otherwise `sum` could add a `Double` into the caller's list of Integers. Fix: `static double sum(List<? extends Number> xs)`, a producer of Numbers (PECS: producer extends).

</details>

### JAVA-L2-008 · recall
What is type erasure? Name three things you cannot do because of it.

<details><summary>Answer</summary>

Generic type arguments exist only at compile time; the compiler checks them, inserts casts, and removes them from the bytecode. At runtime `List<String>` is just `List`. You cannot: `new T()`; `T.class` or `instanceof List<String>`; create a generic array `new T[10]`; overload methods whose parameters differ only in type arguments (`f(List<String>)` and `f(List<Integer>)`). Workaround: pass a `Class<T>` type token.

</details>

### JAVA-L2-009 · recall
What does PECS mean? Read the signature `static <T> void copy(List<? super T> dest, List<? extends T> src)` with it.

<details><summary>Answer</summary>

**Producer Extends, Consumer Super.** `src` *produces* Ts (we read from it), so it is `? extends T`: a `List<Integer>` can be a source of Numbers. `dest` *consumes* Ts (we write into it), so it is `? super T`: a `List<Object>` can receive Integers. Together they let you copy a `List<Integer>` into a `List<Number>`.

</details>

### JAVA-L2-010 · predict
What is printed, and in which order?

```java
Stream.of("a", "b", "c")
      .filter(s -> { System.out.print("f" + s + " "); return true; })
      .map(s -> { System.out.print("m" + s + " "); return s; })
      .findFirst();
```

<details><summary>Answer</summary>

`fa ma `. Streams are lazy and process **one element at a time through the whole pipeline**; `findFirst` short-circuits after the first element, so "b" and "c" are never filtered.

</details>

### JAVA-L2-011 · bug
What can go wrong with this collector, and how do you fix it?

```java
Map<String, Product> bySku = products.stream()
        .collect(Collectors.toMap(Product::sku, p -> p));
```

<details><summary>Answer</summary>

If two products share a SKU, `toMap` throws `IllegalStateException: Duplicate key`. (It also throws `NullPointerException` for null values.) Decide the rule and pass a merge function: `toMap(Product::sku, p -> p, (a, b) -> a)` keeps the first, or use `groupingBy` if duplicates are legitimate.

</details>

### JAVA-L2-012 · why
Why are parallel streams usually a bad idea inside a web service?

<details><summary>Answer</summary>

They run on the JVM-wide **ForkJoin common pool**, shared by every request and by other libraries; one heavy parallel stream steals CPU from all other requests and can block them if it does I/O. Parallelism also has overhead (splitting, merging) that only pays off for large CPU-bound work on many cores. Web requests are already parallel across threads. Use sequential streams; for genuinely parallel CPU work, a dedicated executor.

</details>

### JAVA-L2-013 · recall
Where should you use `Optional`, and where not?

<details><summary>Answer</summary>

Use it as a **return type** for "maybe no result" (`findById`, `findFirst`), and consume it with `map`, `filter`, `orElse`, `orElseGet`, `orElseThrow`. Do not use it for fields (not serializable, extra object), method parameters (callers then pass `null` Optionals), or collections (return an empty collection). Never call `get()` without checking; `orElseThrow()` says what you mean.

</details>

### JAVA-L2-014 · recall
What does a record generate, and what can you still add to it?

<details><summary>Answer</summary>

A canonical constructor, private final fields, accessor methods named like the components (`cents()`, not `getCents()`), `equals`, `hashCode` and `toString`. You can add a compact constructor (validation, normalisation), static factory methods, instance methods, static fields, and implement interfaces. You cannot add instance fields or extend another class (records are implicitly final and extend `Record`).

</details>

### JAVA-L2-015 · bug
A record holds a list. Can its state still change? Fix it.

```java
public record Order(long id, List<String> items) {}
```

<details><summary>Answer</summary>

Yes. The field is final, but the list is the caller's mutable list: `items.add(...)` from outside (before or after construction) changes the "immutable" record, and `order.items().add(...)` works too. Fix with a defensive copy in the compact constructor: `public Order { items = List.copyOf(items); }` (unmodifiable, and rejects nulls).

</details>

### JAVA-L2-016 · why
What do sealed interfaces add to pattern-matching `switch`?

<details><summary>Answer</summary>

The compiler knows the complete set of subtypes, so a `switch` that covers them all is **exhaustive** without a `default`. When someone adds a new permitted subtype, every switch that does not handle it **fails to compile**, instead of silently falling into a default branch at runtime. That is how you model "one of these cases" (results, events, commands) safely.

</details>

### JAVA-L2-017 · predict
What happens?

```java
List<Integer> xs = new ArrayList<>(List.of(1, 2, 3, 4));
for (Integer x : xs) {
    if (x % 2 == 0) xs.remove(x);
}
```

<details><summary>Answer</summary>

`ConcurrentModificationException` (the fail-fast iterator notices the list changed structurally since it was created). Fix: `xs.removeIf(x -> x % 2 == 0)` or use `Iterator.remove()`. Note: no threads are involved; the name is misleading.

</details>

### JAVA-L2-018 · recall
`List.of(...)` vs `Collections.unmodifiableList(list)` vs `Arrays.asList(array)`: what can you change through each, and does each reflect later changes of its source?

<details><summary>Answer</summary>

- `List.of`: fully immutable, no nulls, independent copy (there is no source).
- `Collections.unmodifiableList(list)`: read-only **view**; changes to the underlying `list` show through.
- `Arrays.asList(array)`: fixed-size, write-through: `set` works and changes the array; `add`/`remove` throw `UnsupportedOperationException`; allows nulls.

Use `List.copyOf(list)` to snapshot something you were given.

</details>

### JAVA-L2-019 · interview
When would you choose a `TreeMap` over a `HashMap`? Give a backend example.

<details><summary>Answer</summary>

When you need **sorted keys** or **range/neighbour queries**: `floorKey`, `ceilingKey`, `headMap`, `subMap`, `firstKey`. O(log n) instead of O(1) per operation. Examples: price tiers ("which discount band applies to 73 EUR?" with `floorEntry`), time-series buckets, consistent-hashing rings (find the next node clockwise with `ceilingEntry`), or report output that must be in key order.

</details>

### JAVA-L2-020 · dsa
Implement "group anagrams" (`["eat","tea","tan","ate","nat","bat"]` → `[[eat,tea,ate],[tan,nat],[bat]]`). What is the key and the complexity?

<details><summary>Answer</summary>

Key = the word's letters sorted (`"aet"`), or a 26-count signature. `Map<String, List<String>>` with `computeIfAbsent(key, k -> new ArrayList<>()).add(word)`. O(n · k log k) with sorting (k = word length), O(n · k) with counting. It is the "canonical key" pattern: map each item to a normal form, group by it.

</details>

### JAVA-L2-021 · recall
What is the difference between `Comparable` and `Comparator`? How do you sort orders by amount descending, then by customer name?

<details><summary>Answer</summary>

`Comparable<T>` is the type's own *natural* order (`compareTo`, e.g. String, Integer). `Comparator<T>` is an external order you can define many of. `orders.sort(Comparator.comparingLong(Order::cents).reversed().thenComparing(Order::customer));`. Keep `compareTo` consistent with `equals`, or sorted sets and maps (TreeSet, TreeMap) will treat "different" objects as duplicates.

</details>

## L3

### JAVA-L3-001 · recall
Name the JVM memory areas. Which ones count against a container's memory limit?

<details><summary>Answer</summary>

Heap (young: Eden + survivors; old), Metaspace (class metadata), thread stacks (one per platform thread), code cache (JIT output), direct/native memory (NIO buffers, GC structures, JNI). **All of them** count against the container limit, which is why the heap must be smaller than the limit (e.g. `-XX:MaxRAMPercentage=75`, not 100).

</details>

### JAVA-L3-002 · recall
What is a GC root? Why can Java collect two objects that reference each other?

<details><summary>Answer</summary>

GC roots are references the collector starts from: local variables and operands of live threads' stack frames, static fields of loaded classes, JNI references, and a few JVM-internal ones. An object is live if it is **reachable** from a root. Two objects referencing only each other are unreachable from any root, so both are collected. Java uses tracing, not reference counting.

</details>

### JAVA-L3-003 · why
What is the generational hypothesis, and why does it make allocating many short-lived objects relatively cheap?

<details><summary>Answer</summary>

Most objects die young. So the heap is split: new objects go to the young generation, which is collected often by **copying only the survivors** out; everything else in Eden is freed at once. The cost is proportional to the number of live objects, not the amount of garbage. Allocation itself is a pointer bump in a thread-local buffer (TLAB). Long-lived survivors are promoted to the old generation, collected less often.

</details>

### JAVA-L3-004 · interview
G1, ZGC, Parallel, Serial: when would you use each?

<details><summary>Answer</summary>

- **G1** (default): general purpose services, balanced throughput and pause target (~200 ms default).
- **ZGC** (generational since 21): latency-sensitive services and large heaps; sub-millisecond pauses, at the cost of more CPU and memory.
- **Parallel**: batch/throughput jobs where pauses do not matter.
- **Serial**: tiny heaps and single-CPU containers (the JVM picks it automatically below 2 CPUs or ~1.8 GB). If your pods get Serial unexpectedly, give them more CPU or set the GC explicitly.

</details>

### JAVA-L3-005 · scenario
A service's latency is fine for a day, then p99 slowly rises, and after 3 days it dies with `OutOfMemoryError: Java heap space`. Walk through your investigation.

<details><summary>Answer</summary>

It is the shape of a **leak**. (1) Check GC logs or heap metrics: is heap-after-GC (the baseline) rising steadily? (2) Make sure `-XX:+HeapDumpOnOutOfMemoryError` is on, so the next crash leaves a dump; meanwhile compare two `jcmd <pid> GC.class_histogram` outputs taken hours apart to see which classes grow. (3) Take a heap dump (from a pod drained of traffic if possible) and open it in Eclipse MAT: the dominator tree shows what retains the most, "path to GC roots" shows why it is still reachable. (4) Typical findings: an unbounded map/cache, mutable map keys, listeners never removed, ThreadLocals in pools. (5) Fix, add a test or a metric (cache size), deploy, watch the baseline flatten. Short term, restarting buys time but is not the fix.

</details>

### JAVA-L3-006 · recall
`OutOfMemoryError: Java heap space`, `Metaspace`, `unable to create native thread`, and a container `OOMKilled`: what does each point to?

<details><summary>Answer</summary>

- **Java heap space**: live objects fill the heap (leak or undersized heap).
- **Metaspace**: too many loaded classes (class loader leak on redeploys, runaway proxy or bytecode generation).
- **unable to create native thread**: the OS refused a thread (thread leak, too many platform threads, ulimit or memory for stacks).
- **OOMKilled** (exit 137): the kernel killed the process because total memory (heap + metaspace + stacks + direct + ...) exceeded the container limit. No Java stack trace; reduce `MaxRAMPercentage` or find native memory use (Native Memory Tracking).

</details>

### JAVA-L3-007 · bug
This "cache" is in a Spring singleton bean. What will happen in production, and how do you fix it?

```java
private final Map<String, Report> cache = new HashMap<>();

public Report report(String key) {
    return cache.computeIfAbsent(key, this::build);
}
```

<details><summary>Answer</summary>

Two bugs. (1) **Unbounded**: every distinct key stays forever; if keys come from users (dates, filters, ids) the map grows until `OutOfMemoryError`. (2) **Not thread-safe**: a singleton bean is called by many request threads; concurrent `computeIfAbsent` on a `HashMap` can corrupt it or throw `ConcurrentModificationException`. Fix: a real cache with a maximum size, an expiry and stats, e.g. Caffeine `Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(10)).recordStats().build()`, which is also thread-safe.

</details>

### JAVA-L3-008 · recall
What does JIT warm-up mean, and what are two production consequences?

<details><summary>Answer</summary>

Bytecode starts interpreted; hot methods are compiled by C1 then C2 after enough calls, with optimisations based on observed behaviour. Until then the code is slower. Consequences: (1) a freshly started instance has higher latency, so rolling deploys and autoscaling should ramp traffic or warm up before readiness; (2) naive benchmarks measure interpreted or dead-code-eliminated code, so use JMH. Mitigations: CDS/AppCDS, Spring AOT, CRaC, GraalVM native images.

</details>

### JAVA-L3-009 · scenario
CPU on one pod is at 100%. How do you find which code is responsible, with tools in the JDK?

<details><summary>Answer</summary>

`top -H -p <pid>` (or `ps -L`) to find the busiest thread ids; convert to hex; `jcmd <pid> Thread.print` and find `nid=0x<hex>`; read its stack (take 2-3 dumps to confirm it stays there). If the busy threads are GC threads, look at the heap/GC log instead. For a fuller picture, record 60 s with Java Flight Recorder (`jcmd <pid> JFR.start duration=60s filename=cpu.jfr`) and look at the hot methods in JDK Mission Control. Usual suspects: an infinite loop, catastrophic regex backtracking, a huge sort or serialisation, or GC churn.

</details>

### JAVA-L3-010 · recall
What is Java Flight Recorder, and why is it acceptable in production when a profiler often is not?

<details><summary>Answer</summary>

JFR is a low-overhead event recorder built into the JVM (CPU samples, allocations, locks, GC, I/O, exceptions, custom events). It typically costs around 1% overhead, can be started and stopped at runtime with `jcmd`, and writes a `.jfr` file you analyse in JDK Mission Control. Traditional instrumenting profilers slow the application significantly and change its behaviour.

</details>

### JAVA-L3-011 · design
List what a production-grade in-memory cache needs, and why each item matters.

<details><summary>Answer</summary>

- **Bound** (max entries or weight) + eviction policy: otherwise it is a memory leak.
- **Expiry** (after write or after access): data changes; bounded staleness.
- **Thread safety**: it is shared by request threads.
- **Metrics** (hit ratio, evictions, load time): to know if it helps and how to size it.
- **Invalidation strategy** on writes, and awareness that each instance has its own copy (multiple pods may disagree; use a shared cache like Redis or short TTLs if that matters).
- **Stampede protection**: one load per key when it expires under load (single-flight).
- Testable time (an injected `Clock`).

</details>

### JAVA-L3-012 · dsa
How do you sort a 50 GB file of log lines on a machine with 2 GB of RAM?

<details><summary>Answer</summary>

**External merge sort.** (1) Read chunks that fit in memory (say 1 GB), sort each in memory, write each as a sorted run file (~50 runs). (2) **k-way merge**: open all runs, put the first line of each into a min-heap keyed by the sort key; repeatedly poll the smallest, write it, and push the next line from the same run. O(N log k) comparisons, memory O(k) plus buffers. If k is too large for open files or buffers, merge in multiple passes. Same idea as LSM-tree compaction and merging shard results.

</details>

### JAVA-L3-013 · why
Why are immutable objects automatically thread-safe, and what makes a class truly immutable?

<details><summary>Answer</summary>

Data races need at least one thread *writing* shared state; immutable objects are never written after construction, so any number of threads can read them without locks. With all fields `final`, the Java Memory Model also guarantees other threads see the fully constructed values (safe publication of final fields). Truly immutable: all fields `final` and private, no setters, the class cannot be subclassed (final or a record), mutable inputs copied in (`List.copyOf`), and no mutable internals handed out.

</details>

### JAVA-L3-014 · recall
How do you get a heap dump and a thread dump from a JVM running in a Kubernetes pod?

<details><summary>Answer</summary>

`kubectl exec -it <pod> -- jcmd 1 GC.heap_dump /tmp/heap.hprof` (the JVM is usually PID 1; the dump pauses the JVM and can be large, so ensure disk space and preferably take the pod out of traffic first), then `kubectl cp <pod>:/tmp/heap.hprof .`. Thread dump: `kubectl exec <pod> -- jcmd 1 Thread.print`. Requires a JDK (or at least `jcmd`) in the image; otherwise use an ephemeral debug container sharing the process namespace, or have `-XX:+HeapDumpOnOutOfMemoryError` write to a mounted volume.

</details>

### JAVA-L3-015 · interview
What changed between Java 8 and Java 21 that matters for writing backend code? Name at least five features.

<details><summary>Answer</summary>

`var` (10), `List.of`/`Map.of` (9), new `HttpClient` (11), text blocks (15), switch expressions (14), records (16), `instanceof` pattern matching (16), sealed classes (17), pattern matching for switch and record patterns (21), **virtual threads** (21), sequenced collections (21), better container awareness, G1 by default (9), generational ZGC (21), helpful NullPointerExceptions (14).

</details>

### JAVA-L3-016 · scenario
A Spring Boot pod with a 1 GiB memory limit keeps getting `OOMKilled`, but heap metrics show only ~600 MB used out of a 900 MB max heap. What is happening and what do you change?

<details><summary>Answer</summary>

The heap is allowed to grow to 900 MB, leaving only ~120 MB for everything else: metaspace (Spring apps load many classes, often 100+ MB), thread stacks (200 Tomcat threads × up to 1 MB), code cache, direct buffers, GC structures. The total process memory exceeds 1 GiB and the kernel kills it. Fix: lower the heap share (`-XX:MaxRAMPercentage=60-75` instead of an explicit 900m), cap other areas if needed (`-XX:MaxMetaspaceSize`, fewer threads or virtual threads), measure with Native Memory Tracking, or raise the limit.

</details>

### JAVA-L3-017 · why
Why should you not measure performance with `System.nanoTime()` around a loop, and what do you use instead?

<details><summary>Answer</summary>

The first iterations run interpreted, the JIT later compiles and may optimise away work whose result is unused (dead-code elimination), constant-fold inputs, or apply on-stack replacement; GC pauses land randomly; and one run says nothing about variance. **JMH** (Java Microbenchmark Harness) handles warm-up iterations, forks fresh JVMs, prevents dead-code elimination with `Blackhole`, and reports statistics. And for real services, measure end-to-end under load, not methods in isolation.

</details>

### JAVA-L3-018 · bug
This code is "fine in tests" but slow in production with millions of calls. Why?

```java
boolean valid(String email) {
    return email.matches("^[^@\\s]+@[^@\\s]+\\.[a-z]{2,}$");
}
```

<details><summary>Answer</summary>

`String.matches` **compiles the regex every call**. Keep a `private static final Pattern EMAIL = Pattern.compile(...)` and call `EMAIL.matcher(email).matches()`. Also beware patterns with nested quantifiers, which can backtrack catastrophically on crafted input (a ReDoS risk for user input).

</details>

### JAVA-L3-019 · recall
What is the difference between a `ClassNotFoundException` and a `NoClassDefFoundError`?

<details><summary>Answer</summary>

`ClassNotFoundException` (checked) is thrown when code asks to load a class by name at runtime (`Class.forName`, a class loader) and it is not on the classpath. `NoClassDefFoundError` (an Error) means a class that **was present at compile time** cannot be found or initialised at runtime: typically a missing or conflicting jar in the deployed artifact, or a static initialiser that failed earlier (`ExceptionInInitializerError` first, then NoClassDefFoundError on later uses). Both often come from dependency conflicts (build track).

</details>

### JAVA-L3-020 · interview
Explain escape analysis and why it means "allocation is cheap" is even more true than it looks.

<details><summary>Answer</summary>

The C2 JIT analyses whether an object can be seen outside the method (stored in a field, returned, passed to code it cannot see). If it does not escape, the JIT can replace the object with its fields in registers or on the stack (**scalar replacement**), so no heap allocation happens at all, and locks on it can be removed (lock elision). It only applies after JIT compilation and when inlining makes the object's whole life visible. It does not mean "never think about allocation" in hot paths, but small temporary objects (iterators, small records) often cost nothing.

</details>
