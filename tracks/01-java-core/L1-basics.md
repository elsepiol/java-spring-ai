# Java core, L1: the basics, properly

**Goal:** know what actually happens when Java code runs, and stop being surprised by `==`, `Integer`, integer division, `null` and exceptions. Every later track (Spring, JPA, concurrency) assumes this level.

**How to use this lesson** (same for every lesson in the dojo):
1. Read a section. Run its example (`java tracks/01-java-core/examples/X.java` from the repo root, no build needed).
2. Predict the output before you run it. Being wrong is the point: that is where you learn.
3. Do the katas for the level: `cd katas && ./mvnw test -Dgroups="javacore & L1"`. They fail until you write the code.
4. Drill: `java quiz/Quiz.java --track java-core --level 1`. Answer out loud before you reveal.
5. Come back tomorrow and let the quiz ask you what you forgot.

---

## 1. From source code to a running program

```
Money.java  --javac-->  Money.class (bytecode)  --java-->  JVM: loads classes, interprets, JIT-compiles hot code to machine code
```

- **JDK** (Java Development Kit): compiler `javac`, the `java` launcher, tools (`jcmd`, `jstack`, `jar`), and the runtime. You install a JDK to build.
- **JVM** (Java Virtual Machine): the program that runs bytecode. The same `.class` file runs on Linux, macOS and Windows because each has its own JVM. "Write once, run anywhere".
- **JRE**: the runtime without the tools. Oracle stopped shipping a separate JRE with Java 11; some vendors (e.g. Eclipse Temurin) still publish JRE builds, which make smaller production images. You can also build a trimmed runtime with `jlink`.
- **Bytecode** is not machine code. The JVM starts by interpreting it, measures which methods are hot, and compiles those with the JIT (just-in-time) compiler. This is why Java services are slower for the first seconds after start ("warm-up", L3).
- `java Hello.java` (Java 11+) compiles in memory and runs a single file. The examples in this dojo use it.
- A real project has many classes and libraries (`.jar` files = zip files of classes). The **classpath** tells the JVM where to find them. Build tools (Maven, track 03) manage it for you.

```java
public class Hello {
    public static void main(String[] args) {   // the entry point the launcher looks for
        System.out.println("Hello " + args.length);
    }
}
```

`public static void main(String[] args)`: `public` so the launcher can call it, `static` because no object exists yet, `String[] args` holds the command-line arguments.

---

## 2. Types: primitives and references

Java has exactly two kinds of types.

| Primitives (8) | Size | Default | Example |
|---|---|---|---|
| `byte`, `short`, `int`, `long` | 1, 2, 4, 8 bytes | 0 | `int count = 3; long id = 9_000_000_000L;` |
| `float`, `double` | 4, 8 bytes | 0.0 | `double ratio = 0.75;` |
| `char` | 2 bytes (UTF-16 unit) | `'\u0000'` | `char c = 'A';` |
| `boolean` | (JVM-dependent) | `false` | `boolean ok = true;` |

Everything else is a **reference type**: classes, interfaces, arrays, enums, records, `String`. A variable of reference type holds a *reference* (think: an address) to an object on the heap, or `null`.

```
int n = 5;                  n: [ 5 ]                         the value itself
String s = "shop";          s: [ ref ] ---> String object "shop" on the heap
String t = s;               t: [ ref ] ---^ (same object, NOT a copy)
```

Simplified memory picture (L3 refines it): each thread has a **stack** of frames holding local variables (primitives and references); **objects live on the heap**, shared by all threads and cleaned up by the garbage collector when nothing references them any more.

**Three arithmetic traps that cause real bugs:**
- **Integer division:** `3 / 2 == 1`. `int / int` stays `int`. Write `(double) sum / count`.
- **Overflow is silent:** `Integer.MAX_VALUE + 1 == Integer.MIN_VALUE`. Money in cents adds up fast: 3 orders of 10 million euros overflow an `int`. Use `long`, or `Math.addExact` to get an exception instead of a wrong number.
- **`double` cannot represent 0.1 exactly:** `0.1 + 0.2 == 0.30000000000000004`. Never store money in `double`. Use `long` cents (as the shop project does: `priceCents`) or `BigDecimal` created from a String.

Run `examples/FloatingMoney.java`.

---

## 3. Strings

- **Immutable.** No method changes a String; `s.toUpperCase()` returns a new one. That makes Strings safe to share between threads and to use as map keys.
- **`==` compares references, `equals` compares text.** String literals are kept in the **string pool**, so `"shop" == "shop"` happens to be true, while text built at runtime (read from a request, concatenated in a loop) is a new object. Always `equals`. Put the constant first to be null-safe: `"admin".equals(name)`.
- **Building strings in a loop:** `s += x` copies the whole string each time, O(n²). Use `StringBuilder`. (A single `a + b + c` expression is fine: the compiler optimises it.)
- **Text blocks** (Java 15) for multi-line text such as SQL or JSON:

```java
String sql = """
        SELECT id, name
        FROM product
        WHERE price_cents < ?
        """;
```

Run `examples/StringPool.java`.

---

## 4. Variables, methods, boxing

**Java is always pass-by-value.** A method gets a copy of each argument. For a reference, the copy points to the same object: the method can change the object, but reassigning the parameter does not affect the caller. ("Java passes objects by reference" is the interview trap; the precise answer is "it passes references by value".) Run `examples/PassByValue.java`.

**`static` vs instance:** an instance field belongs to each object; a `static` field belongs to the class, one copy for the whole JVM. Static mutable state is shared by every request and every thread: in a server, that is almost always a bug (and a leak, L3).

**`final`:** a final variable cannot be reassigned. A final reference does not make the object immutable: `final List<String> l` can still be `l.add(...)`ed.

**Boxing:** each primitive has a wrapper class (`int` ↔ `Integer`, `long` ↔ `Long`, ...). Collections hold objects, so `List<Integer>` boxes automatically. Three consequences:
- `Integer a = 1000, b = 1000; a == b` is **false**. `Integer.valueOf` caches only -128..127, so small numbers "work" in tests and big ones fail in production.
- Unboxing `null` throws `NullPointerException`: `int x = map.get("missing");`.
- Boxing in hot loops allocates an object per step and is several times slower (`examples/IntegerCache.java`).

---

## 5. Object-oriented programming, practically

A **class** is a blueprint; an **object** is an instance with its own field values.

```java
public class Account {
    private long balanceCents;                 // encapsulation: state is private

    public void deposit(long cents) {          // behaviour guards the state
        if (cents <= 0) throw new IllegalArgumentException("deposit must be positive");
        balanceCents += cents;
    }

    public long balanceCents() { return balanceCents; }
}
```

The four ideas, with what they mean in real code:

| Idea | Meaning | Real use |
|---|---|---|
| **Encapsulation** | Hide fields; expose operations that keep the object valid. | Nobody can set a negative balance from outside. JPA entities, value objects. |
| **Abstraction** | Depend on *what* something does, not *how*. | `interface PaymentGateway { Receipt charge(Money m); }`; Spring injects an implementation. |
| **Inheritance** | `class B extends A`: B is-an A and reuses its code. | Exceptions (`class ConfigException extends Exception`). Use sparingly. |
| **Polymorphism** | One call, the behaviour of the actual object's class. | `gateway.charge(m)` calls Stripe's or the fake's code depending on what was injected. |

- **Overriding** (runtime): a subclass redefines a method with the same signature; the JVM picks the version from the *object's* class. Always write `@Override`: it makes the compiler check you actually override.
- **Overloading** (compile time): same name, different parameter types, chosen by the *declared* types of the arguments.
- **Interface vs abstract class:** an interface is a contract (a class can implement many; methods can have `default` bodies since Java 8); an abstract class can hold state and constructors (a class can extend only one). Default to interfaces.
- **Composition over inheritance:** "has-a" (a field) instead of "is-a" (extends). Inheritance couples the subclass to the parent's internals; composition lets you swap parts. Spring is built on composition: services *have* repositories injected.
- **Access modifiers:** `private` (the class), package-private (no keyword: the package), `protected` (package + subclasses), `public` (everyone). Start with the most restrictive.

---

## 6. Value objects: `equals`, `hashCode`, immutability

Some objects are defined by their **identity** (a customer, an order: two customers named "Ann" are different people). Others are defined by their **value** (money, an email address, a date: two "12.50 EUR" are the same thing). Value objects should:
- be **immutable** (all fields `final`, no setters, operations return new objects);
- override **`equals` and `hashCode` together**.

The `equals`/`hashCode` contract:
1. `equals` is reflexive, symmetric, transitive, consistent, and `x.equals(null)` is false.
2. **If `a.equals(b)` then `a.hashCode() == b.hashCode()`.** (The reverse need not hold: different objects may share a hash code.)

Override `equals` without `hashCode` and `HashSet`/`HashMap` silently break: the set looks in the bucket of the new object's hash and never finds the equal one. Run `examples/HashCodeContract.java`.

**Records** (Java 16) are made for this: `record Money(long cents, String currency) {}` generates the constructor, accessors, `equals`, `hashCode` and `toString`. Validation goes in a compact constructor:

```java
public record Email(String value) {
    public Email {                                     // compact constructor
        if (value == null || !value.contains("@")) throw new IllegalArgumentException("not an email: " + value);
        value = value.strip().toLowerCase();           // normalise before the field is set
    }
}
```

> **Kata `Money`** (`katas/.../javacore/l1/Money.java`): write it as a plain class first (equals, hashCode, toString by hand), then notice how much a record would have saved you.

---

## 7. Hashing and Big-O: the first DSA tool you need

**Big-O** describes how the work grows with the input size n. You need four of them daily:
- O(1): constant, e.g. `HashMap.get`, array index;
- O(log n): halve the problem each step, e.g. binary search, `TreeMap.get`, a database B-tree index;
- O(n): look at everything once, e.g. a loop, a full table scan;
- O(n²): a loop inside a loop. 1,000 elements = 1 million steps; 100,000 = 10 billion, minutes instead of milliseconds.

**How a `HashMap` finds things in O(1):** it keeps an array of "buckets". To store a key, it computes `key.hashCode()`, maps it to a bucket index, and stores the entry there. To find a key, it goes straight to that bucket and checks the few entries in it with `equals`. No scanning. (L2 goes inside: collisions, resizing, trees.)

The pattern "have I seen this before?" turns many O(n²) problems into O(n):

```java
// Does any pair add up to target?  O(n^2): check every pair.  O(n): remember what you've seen.
Set<Integer> seen = new HashSet<>();
for (int x : nums) {
    if (seen.contains(target - x)) return true;
    seen.add(x);
}
return false;
```

> **Kata `Hashing`**: two-sum (with indices) and "first unique character". The tests use 200,000 elements, so the double loop times out.

---

## 8. Exceptions

```
Throwable
├── Error                      JVM trouble: OutOfMemoryError, StackOverflowError. Do not catch.
└── Exception                  CHECKED: the compiler forces you to catch or declare (IOException, SQLException)
    └── RuntimeException       UNCHECKED: bugs and business rule violations (NullPointerException,
                               IllegalArgumentException, IllegalStateException, your domain exceptions)
```

- **Checked** exceptions are for failures the caller can reasonably handle (a file is missing). **Unchecked** are for programming errors and for errors that should travel up to a single handler. Modern frameworks (Spring, JPA) use unchecked exceptions almost everywhere; the shop project's `ProductNotFoundException` is unchecked and turned into a 404 by one `@RestControllerAdvice`.
- **try-with-resources** closes anything `AutoCloseable` (files, sockets, JDBC connections), however the block exits:

```java
try (BufferedReader in = Files.newBufferedReader(path)) {
    return in.readLine();
}   // in.close() has run here, even after an exception
```

- **Wrap, do not lose:** `throw new ConfigException("cannot read " + path, e);` The second argument is the **cause**; the stack trace then shows both. Losing the cause turns a 5-minute fix into a day of guessing.
- **Never swallow:** `catch (Exception e) {}` hides the bug and corrupts the program's state. At minimum log it; usually rethrow.
- `finally` runs after try/catch whatever happens (even `return`). Do not `return` from `finally`: it discards the exception.
- Messages are for the person on call at 3 a.m.: say what failed, with which value, in which file or for which id.

> **Kata `ConfigReader`**: read a port from a config file, and turn every failure into one clear, checked exception that keeps its cause.

> **Kata `BugHunt`**: five small methods that compile, look right, and are wrong (sections 2-4).

---

## 9. Katas for this level

```
cd katas
./mvnw test -Dgroups="javacore & L1"          # all L1 katas
./mvnw test -Dtest=MoneyTest                    # one kata
```

| Kata | Practises | Done when |
|---|---|---|
| `Money` | value objects, equals/hashCode, immutability | `MoneyTest` passes |
| `BugHunt` | `==` vs equals, Integer cache, integer division, references, overflow | `BugHuntTest` passes, and you can explain each bug in one sentence |
| `Hashing` | HashMap, Big-O, two-sum, first unique | `HashingTest` passes (the timing tests too) |
| `ConfigReader` | checked exceptions, causes, try-with-resources | `ConfigReaderTest` passes |

Reference solutions are in `solutions/katas/`. Look only after your tests pass, or after a real attempt.

---

## 10. Seen in the shop project

- Money as `long` cents: `Product.priceCents`, `OrderEvent.totalCents` in [spring-backend-ai](https://github.com/elsepiol/spring-backend-ai) (`src/main/java/dev/elsepiol/shop/...`).
- Records as immutable value types: `CreateOrderRequest`, `OrderResponse`, `Limit`, `Decision`.
- Unchecked domain exceptions mapped to HTTP errors in one place: `error/ApiExceptionHandler.java`.

## 11. Interview checklist

You should be able to answer, without notes:
1. JDK vs JRE vs JVM. What is bytecode and why is Java portable?
2. Primitive vs reference types. Where do objects live?
3. Why is comparing two `Integer`s with `==` wrong, and why does it "work" for small numbers?
4. Is Java pass-by-value or pass-by-reference? Prove it with an example.
5. Why are Strings immutable, and what is the string pool?
6. The `equals`/`hashCode` contract, and what breaks if you violate it.
7. Checked vs unchecked exceptions, and when you would create each.
8. What does try-with-resources do, and what does it require of the resource?
9. Abstract class vs interface. Composition vs inheritance.
10. Why must money not be a `double`?

Then: `java quiz/Quiz.java --track java-core --level 1`.
