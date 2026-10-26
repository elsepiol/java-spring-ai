# JavaScript and TypeScript, L1: the language for a Java developer

**Goal:** read and write modern JavaScript and TypeScript confidently, coming from Java: values and types, equality, scope and closures, `this`, objects and arrays, modules, and the TypeScript type system's basics.

Katas: `cd web-katas && npm install && npx vitest run src/js/l1`. Drill: `java quiz/Quiz.java --track js-ts --level 1`.

---

## 1. JavaScript is not "Java in the browser"

| Java | JavaScript / TypeScript |
|---|---|
| compiled, statically typed, classes everywhere | interpreted/JIT, **dynamically** typed; TypeScript adds static types that are **erased** at runtime |
| threads | **one thread** per page or Node process, an **event loop**, async I/O (L2) |
| `int`, `long`, `double` | one `number` (64-bit float: `0.1 + 0.2 !== 0.3`, integers safe up to 2^53), plus `bigint` |
| `null` | `null` **and** `undefined` (missing property, missing argument, no return value) |
| classes and interfaces (nominal) | objects are bags of properties; TypeScript types are **structural** (same shape = compatible) |
| packages, Maven | ES modules (`import`/`export`), npm (`package.json`, `package-lock.json` ≈ a lock file) |

Primitive types: `string`, `number`, `bigint`, `boolean`, `undefined`, `null`, `symbol`; everything else is an object (arrays, functions, dates, maps). `typeof null === 'object'` is a historical bug.

Declare with **`const`** (cannot be reassigned; the object can still change) or **`let`**; never `var` (function-scoped and hoisted, section 3).

---

## 2. Equality and truthiness

- `===` (strict): same type and value, no conversion. Use it always. `==` converts types (`'1' == 1`, `null == undefined`, `[] == false` are all true).
- Objects and arrays compare **by reference**: `{a: 1} === {a: 1}` is false, like Java's `==` on objects. There is no built-in `equals`; tests use deep equality (`toEqual`), React compares by reference (track 13).
- `NaN !== NaN`; use `Number.isNaN`. `Object.is` treats NaN as equal to itself and +0/-0 as different.
- **Falsy** values: `false`, `0`, `-0`, `0n`, `''`, `null`, `undefined`, `NaN`. Everything else is truthy, including `'0'`, `[]` and `{}`. `if (count)` is wrong when 0 is a valid count.
- `??` (nullish coalescing) only replaces `null`/`undefined`: `count ?? 10` keeps 0, while `count || 10` does not. `?.` (optional chaining): `user?.address?.city`.

> **Kata `deepEqual`**: structural equality with NaN, arrays vs objects, dates and cycles.

---

## 3. Scope, closures and `this`

- `let`/`const` are **block-scoped**. `var` is function-scoped and **hoisted** (usable before its line, as `undefined`).
- A **closure** is a function together with the variables it captured where it was created. They stay alive as long as the function does, the basis of callbacks, event handlers, React hooks and module privacy:

```ts
function counter() {
  let count = 0;                         // private: only the returned functions can reach it
  return { inc: () => ++count, get: () => count };
}
```

- The classic bug: `for (var i = 0; i < 3; i++) handlers.push(() => i)` gives three functions returning 3, because there is **one** `i` shared by all closures; `let` creates a new binding per iteration.
- **`this`** depends on **how a function is called**, not where it is defined: `obj.method()` sets `this` to `obj`; a detached `const f = obj.method; f()` loses it (`undefined` in strict mode). **Arrow functions** have no own `this`: they use the surrounding one, so use them for callbacks. `fn.bind(obj)` fixes `this`.

> **Kata `closures`**: `once`, `memoize`, and the `var`-in-a-loop bug.

---

## 4. Objects, arrays and functions

```ts
const user = { name: 'Ada', email: 'ada@example.com' };
const { name, ...rest } = user;                         // destructuring + rest
const updated = { ...user, email: 'new@example.com' };  // spread: a shallow COPY with one change (immutability)
const totals = orders
  .filter((o) => o.status === 'PAID')
  .map((o) => o.totalCents)
  .reduce((sum, c) => sum + c, 0);                       // like Java streams, but eager arrays
const byId = new Map(products.map((p) => [p.id, p]));    // Map/Set: any key type, insertion order
```

- Functions are values: pass them, return them, store them. Default parameters, rest parameters (`...args`).
- Copies with spread are **shallow**: nested objects are still shared. `structuredClone` makes deep copies.
- `JSON.stringify`/`JSON.parse` for (de)serialisation; dates become strings, `undefined` disappears, `Map` becomes `{}`.
- Classes exist (`class Cart { #items = []; add(i) {...} }`, `#` = truly private) and are syntax over prototypes; much modern frontend code prefers plain objects and functions.

---

## 5. Modules and npm

```ts
// money.ts
export function formatCents(cents: number): string { return (cents / 100).toFixed(2); }
export default class Money {}
// cart.ts
import Money, { formatCents } from './money';
```

- ES modules: each file has its own scope; `export`/`import` are static (bundlers can tree-shake unused code).
- npm: `package.json` lists dependencies with ranges (`^19.3.0` = compatible minor and patch updates), `package-lock.json` pins exact versions (commit it; `npm ci` installs exactly it, like a reproducible Maven build), `node_modules` is never committed. `devDependencies` are build/test only.
- Scripts: `npm run build`, `npm test`. `npx tool` runs a locally installed binary.

---

## 6. TypeScript basics

```ts
interface Product { id: number; name: string; priceCents: number; tags?: string[] }   // ? = optional
type Status = 'NEW' | 'PAID' | 'SHIPPED';                    // a union of literal types: an enum without the enum
function total(lines: Array<{ quantity: number; unitCents: number }>): number { ... }
const cache = new Map<string, Product>();                    // generics, like Java
function first<T>(items: T[]): T | undefined { return items[0]; }
```

- **Structural typing**: anything with the right shape fits an interface; no `implements` needed.
- **Type inference**: `const n = 5` is a `number`; annotate function parameters and public APIs, let the rest be inferred.
- **Narrowing**: `if (typeof x === 'string')`, `if ('email' in obj)`, `if (result.ok)`: TypeScript tracks what a value can be in each branch. **Discriminated unions** (`{ kind: 'circle', r } | { kind: 'square', side }`) with a `switch` give exhaustive handling.
- `unknown` (must be narrowed before use) vs `any` (turns checking off: avoid). `strict: true` always (null checks included).
- Types are **erased**: they cost nothing at runtime and protect nothing at runtime. Data from the network must be validated (L3).

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `deepEqual` | equality, typeof, arrays vs objects, NaN, cycles with a Map |
| `closures` | closures, `once`/`memoize`, `var` vs `let` |

```
cd web-katas && npx vitest run src/js/l1
```

## 8. Seen in the shop project

- The React frontend (`frontend/`) is strict TypeScript: API types in `src/api`, discriminated states, `??` and `?.` around optional data; `package-lock.json` committed and `npm ci` in CI.

## 9. Interview checklist

1. `==` vs `===`. Which values are falsy? `??` vs `||`.
2. Why is `{} === {}` false? How do you compare objects?
3. `var` vs `let` vs `const`. What is hoisting?
4. What is a closure? Give a practical use.
5. Why does the `var` loop with callbacks print the same number?
6. How is `this` determined? Arrow functions vs regular functions.
7. Spread copies: shallow or deep?
8. What does `package-lock.json` do? `npm install` vs `npm ci`?
9. Structural vs nominal typing. `unknown` vs `any`.
10. Do TypeScript types exist at runtime?
