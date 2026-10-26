# Java core, L2: the everyday toolkit

**Goal:** pick the right collection without thinking, know what happens inside `HashMap`, read and write generic signatures, write stream pipelines that a reviewer approves, and model data with records and sealed types. This is the Java you write every day in a Spring service.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="javacore & L2"`. Drill: `java quiz/Quiz.java --track java-core --level 2`.

---

## 1. The collections map

```
Iterable
└── Collection
    ├── List      ordered, index access, duplicates allowed     ArrayList (default), LinkedList (rarely)
    ├── Set       no duplicates                                 HashSet (default), LinkedHashSet (insertion order), TreeSet (sorted)
    └── Queue     process in some order                         ArrayDeque (FIFO/LIFO default), PriorityQueue (smallest first)
        └── Deque  both ends                                    ArrayDeque
Map (not a Collection)  key -> value                            HashMap (default), LinkedHashMap (order), TreeMap (sorted keys)
```

How to choose, in one table:

| You need | Use | Cost |
|---|---|---|
| A list you append to and read by index | `ArrayList` | get O(1), add at end amortised O(1), insert/remove in the middle O(n) |
| Uniqueness / "seen before?" | `HashSet` | add/contains O(1) average |
| Lookup by key | `HashMap` | get/put O(1) average |
| Keys sorted, or "the next key after x" (range queries) | `TreeMap` / `TreeSet` | O(log n); `floorKey`, `ceilingKey`, `headMap`, `subMap` |
| Predictable iteration order (e.g. JSON output) | `LinkedHashMap` / `LinkedHashSet` | as Hash*, plus a linked list |
| A stack or a FIFO queue | `ArrayDeque` (`push/pop`, `offer/poll`) | O(1); faster than `Stack` and `LinkedList` |
| Always take the smallest (or largest) next | `PriorityQueue` (a binary heap) | offer/poll O(log n), peek O(1) |
| Thread-safe versions | concurrency track, L2 | |

**Why `LinkedList` is almost never right:** every element is a separate node object scattered across the heap. Iterating chases pointers and misses the CPU cache; `ArrayList` walks one contiguous array. Even "insert in the middle" needs O(n) to *find* the middle. Use `ArrayDeque` for queues.

**`ArrayList` growth:** backed by an array. When full, it allocates one 1.5x larger and copies. That is why `add` is O(1) *amortised*. If you know the size, `new ArrayList<>(expected)` avoids the copies.

---

## 2. Inside `HashMap` (the most asked Java interview question)

```
table (array of buckets, length = power of two, starts at 16)
 [0] -> null
 [1] -> ("mug", 7) -> ("pan", 2)        collision: same bucket, kept in a linked list
 [2] -> null
 ...
 [9] -> TreeNode ...                     8+ entries in one bucket and table >= 64: becomes a red-black tree
```

`put(key, value)`:
1. `h = key.hashCode()`, then spread: `h ^ (h >>> 16)` (mixes the high bits into the low ones, because only low bits pick the bucket).
2. Bucket index: `h & (table.length - 1)` (a fast modulo, possible because the length is a power of two).
3. Walk that bucket: if an entry's key `equals` this key, replace the value; otherwise append.
4. If size > capacity × **load factor (0.75)**, **resize**: double the table and redistribute every entry. O(n) for that one put, so give a capacity if you know the size.
5. If one bucket reaches 8 entries (and the table has at least 64 buckets), it is converted into a balanced tree, so the worst case becomes O(log n) instead of O(n). This defends against bad hash codes and hash-flooding attacks.

Facts interviewers probe:
- **One `null` key** is allowed (bucket 0) and `null` values. `Hashtable` and `ConcurrentHashMap` allow neither.
- **Not thread-safe.** Concurrent puts can lose entries or corrupt the structure. Use `ConcurrentHashMap` (concurrency track).
- **Iteration order is not defined** and can change after a resize. If you need order, say so with `LinkedHashMap`.
- **Mutable keys are a bug**: change a key's fields after `put` and its hash code changes; the entry is now in the "wrong" bucket and can never be found or removed. (L3 kata `ActiveCarts` is exactly this, as a production memory leak.)
- `HashSet` is a `HashMap` with dummy values.

---

## 3. Ordered maps and heaps: LRU caches and top-K

**`LinkedHashMap`** keeps a doubly linked list through its entries. Two modes:
- insertion order (default): iteration returns keys in the order they were first put;
- **access order** (`new LinkedHashMap<>(16, 0.75f, true)`): every `get` moves the entry to the end. The *first* entry is then the least recently used. Override `removeEldestEntry` and you have an **LRU cache** in ten lines.

The interview version asks you to build it yourself: a `HashMap<K, Node>` for O(1) lookup plus your own doubly linked list for O(1) "move to front" and "drop the tail". Sentinel head and tail nodes remove all the null checks.

```
head <-> [c] <-> [a] <-> [b] <-> tail          most recent ... least recent
get(b):  head <-> [b] <-> [c] <-> [a] <-> tail  (unlink b, link after head: O(1))
put(d) at capacity 3: evict tail.prev = a
```

> **Kata `LruCache`**: from scratch, then with `LinkedHashMap`. In production you would use Caffeine (the shop project's product cache and its rate-limiter buckets do).

**`PriorityQueue`** is a **binary heap**: an array where each parent is smaller than its children, so the smallest element is always at index 0. `offer` and `poll` are O(log n) (sift up / sift down), `peek` is O(1). Iteration order is NOT sorted.

**Top-K pattern:** to find the k largest of m items, keep a *min*-heap of size k. For each item, add it; if the heap has more than k, remove the smallest. The heap always holds the best k seen so far, with the weakest on top, ready to be replaced. O(m log k) time, O(k) memory, and it works on a stream that does not fit in memory.

> **Kata `TopK`**: the k most frequent words, ties alphabetical. Get the comparator right: the heap's head must be the *worst* of the kept words.

---

## 4. Generics

Generics let the compiler check types in containers and algorithms: `List<String>` cannot receive an `Integer`, and `get` needs no cast.

**Type erasure:** generics exist only at compile time. At runtime a `List<String>` and a `List<Integer>` are both just `List`. Consequences:
- you cannot write `new T()`, `T.class`, or `instanceof List<String>`;
- you cannot overload `void f(List<String>)` and `void f(List<Integer>)` (same erased signature);
- when you need the type at runtime, pass a **type token**: `Class<T> type`, then `type.cast(obj)`. Spring and Jackson do this everywhere (`objectMapper.readValue(json, Order.class)`).

**Bounded types:** `<T extends Comparable<T>> T max(List<T> xs)` means "any T that can compare itself".

**Wildcards and PECS** ("Producer Extends, Consumer Super"):
- `List<? extends Number>`: a list of *some* subtype of Number. You can **read** Numbers from it, you cannot add (the compiler does not know which subtype). It *produces* values: use `extends`.
- `List<? super Integer>`: a list of Integer or a supertype (Number, Object). You can **add** Integers, reads give you `Object`. It *consumes* values: use `super`.
- Why it matters: **`List<Integer>` is NOT a `List<Number>`** (otherwise you could add a Double into a list of Integers). Generics are *invariant*; wildcards add the flexibility back safely. Arrays, by contrast, are covariant, and that is why `Object[] o = new String[1]; o[0] = 1;` compiles and then throws `ArrayStoreException`.

```java
// From java.util.Collections. Read the signature with PECS and it explains itself:
public static <T> void copy(List<? super T> dest, List<? extends T> src)
public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp)
```

> **Kata `Generics`**: the signatures are given; implement the bodies, then try removing a wildcard and see which test stops compiling. Includes a type-safe heterogeneous container (type tokens).

---

## 5. Lambdas and streams

A **lambda** is an implementation of a **functional interface** (an interface with one abstract method): `Runnable`, `Supplier<T>`, `Consumer<T>`, `Function<T,R>`, `Predicate<T>`, `Comparator<T>`, `BiFunction<T,U,R>`. A **method reference** is shorthand: `String::length` is `s -> s.length()`. Lambdas can use local variables only if they are *effectively final* (never reassigned).

A **stream** is a pipeline over data: a source, zero or more lazy intermediate operations, and one terminal operation that runs it.

```java
Map<String, Long> revenue = orders.stream()                        // source
        .filter(o -> o.status() == PAID)                           // intermediate (lazy)
        .collect(Collectors.groupingBy(Order::customer,            // terminal: runs the pipeline
                 Collectors.summingLong(Order::cents)));
```

Rules that interviewers and reviewers check:
- **Lazy:** nothing runs until a terminal operation. Elements flow through the pipeline one at a time; `findFirst` may touch only the first element. `sorted` and `distinct` are barriers that must see everything first. Run `examples/StreamLaziness.java`.
- **One use:** a consumed stream throws `IllegalStateException` if reused.
- **No side effects** in `map`/`filter` (no adding to outside lists, no changing objects). Collect results instead.
- **Collectors you must know:** `toList()` (Java 16, unmodifiable), `toMap(k, v, merge)` (throws on duplicate keys without the merge function), `groupingBy(classifier, [mapFactory], downstream)`, `partitioningBy`, `counting`, `summingLong`, `averagingDouble`, `mapping`, `joining`.
- **Primitive streams** (`IntStream`, `LongStream`, `mapToLong`) avoid boxing and give `sum`, `average`, `max`.
- **`flatMap`**: one element to many: `orders.stream().flatMap(o -> o.items().stream())`.
- **Comparators compose:** `Comparator.comparing(Order::cents).reversed().thenComparing(Order::customer)`.
- **Parallel streams** use the shared ForkJoin common pool. In a server they compete with every other request and rarely help; do not use them for I/O. Default: sequential.
- When a loop is clearer, write a loop. Streams are a tool, not a rule.

**`Optional`** is a return type for "maybe no result" (`findById`). Use `map`, `orElse`, `orElseThrow`. Do not use it for fields, parameters or collections (return an empty list instead), and never call `get()` without checking.

> **Kata `OrderReport`**: six real reports, each as one pipeline.

---

## 6. Modern data modelling: records, enums, sealed types, pattern matching

- **Records** (16): immutable data carriers with generated `equals`/`hashCode`/`toString`. Perfect for DTOs (request and response bodies), events, value objects, map keys. A record's fields are final, but a `List` field is still mutable unless you copy it in the constructor (`items = List.copyOf(items)`).
- **Enums:** a fixed set of instances, can have fields and methods, `switch` over them, and `EnumMap`/`EnumSet` are compact and fast. Order statuses, roles.
- **Sealed interfaces** (17): `sealed interface Payment permits Card, Transfer, Voucher` limits who can implement it. The compiler then knows every case.
- **Pattern matching for `switch`** (21) with **record patterns**:

```java
sealed interface Shape permits Circle, Rect {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}

static double area(Shape s) {
    return switch (s) {                          // exhaustive: no default needed
        case Circle c -> Math.PI * c.r() * c.r();
        case Rect(double w, double h) -> w * h;  // record pattern destructures
    };
}
```

Add a `Triangle` to the sealed interface and every switch that does not handle it stops compiling. That is the strongest reason to use them: the compiler finds every place you forgot. This is "algebraic data types", the way functional languages model results (`sealed interface Result permits Ok, Failed`).

> **Kata `Expr`**: an arithmetic expression tree with `eval`, `show` and `simplify`, all as exhaustive switches. Also good tree-recursion practice for DSA rounds.

---

## 7. Iteration and mutability pitfalls

- **Fail-fast iterators:** changing a list (add/remove) while a for-each loop runs over it throws `ConcurrentModificationException`, even in one thread. Use `iterator.remove()` or `removeIf`. (It is a best-effort check, not a thread-safety guarantee.)
- **Unmodifiable vs immutable:** `Collections.unmodifiableList(list)` is a read-only **view**: changes to the original show through it. `List.copyOf(list)` and `List.of(...)` are truly immutable copies (and reject `null`).
- **`Arrays.asList`** is fixed-size and writes through to the array: `set` works, `add` throws.
- **Returning internal collections** lets callers change your object's state. Return a copy or an unmodifiable view (defensive copying, L3).

Run `examples/FailFastIteration.java`.

---

## 8. Katas for this level

| Kata | Practises | DSA |
|---|---|---|
| `LruCache` | HashMap + doubly linked list, LinkedHashMap access order | LRU in O(1) |
| `TopK` | HashMap counting, PriorityQueue, comparators | heap top-K, O(m log k) |
| `OrderReport` | streams, collectors, comparators, EnumMap | |
| `Generics` | wildcards, PECS, type tokens | |
| `Expr` | records, sealed interfaces, pattern matching | tree recursion |

```
cd katas && ./mvnw test -Dgroups="javacore & L2"
```

## 9. Seen in the shop project

- Bounded caches instead of maps: `ratelimit/RateLimiter.java` keeps token buckets in a Caffeine cache; `cache/CacheConfig.java` caches products.
- Records everywhere at the API edge: `CreateOrderRequest` (with nested `Item` record), `CursorPage`, `PageResponse`.
- Plain enums for fixed sets: `OrderStatus` (PLACED, CANCELLED), `Role` (USER, ADMIN), stored as text columns.

## 10. Interview checklist

1. How does `HashMap.put` work? What are load factor, resize and treeification?
2. ArrayList vs LinkedList: when is each faster, really?
3. HashMap vs TreeMap vs LinkedHashMap: give a use case for each.
4. How would you build an LRU cache with O(1) get and put?
5. How do you find the top 10 of a billion items with little memory?
6. What is type erasure, and what can you not do because of it?
7. Explain `? extends` vs `? super` with `Collections.copy`.
8. Are streams lazy? What is a terminal operation? Why can a stream not be reused?
9. When would you not use a stream? When are parallel streams a bad idea?
10. What problems do sealed interfaces plus pattern matching solve?
11. Why does a for-each with `remove` throw `ConcurrentModificationException`?
12. `List.of` vs `Collections.unmodifiableList` vs `Arrays.asList`.
