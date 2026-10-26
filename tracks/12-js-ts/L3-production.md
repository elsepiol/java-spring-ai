# JavaScript and TypeScript, L3: types in depth, runtime validation, leaks, performance, tooling

**Goal:** use TypeScript's type system to make wrong states unrepresentable, validate data at runtime where types cannot help, avoid memory leaks and performance traps in long-running pages, and know the build tooling that turns your code into what the browser runs.

Prerequisite: L1, L2. Katas: `cd web-katas && npx vitest run src/js/l3`. Drill: `java quiz/Quiz.java --track js-ts --level 3`.

---

## 1. TypeScript beyond the basics

```ts
type Result<T, E = string> = { ok: true; value: T } | { ok: false; error: E };   // discriminated union
type LoadState<T> = { status: 'idle' } | { status: 'loading' } | { status: 'error'; error: Error } | { status: 'ready'; data: T };

function assertNever(x: never): never { throw new Error(`unexpected ${JSON.stringify(x)}`); }
switch (state.status) { case 'idle': ...; case 'loading': ...; case 'error': ...; case 'ready': ...; default: assertNever(state); }   // exhaustive

type Events = { login: User; logout: undefined };
function on<K extends keyof Events>(event: K, fn: (payload: Events[K]) => void) {}   // keyof + indexed access
type ReadonlyProduct = Readonly<Product>;  type Draft = Partial<Product>;  type Summary = Pick<Product, 'id' | 'name'>;
type ApiPaths = `/api/${'orders' | 'products'}`;                                      // template literal types
type Brand<T, B> = T & { readonly __brand: B };  type UserId = Brand<number, 'UserId'>; // nominal-ish ids
```

- **Make illegal states unrepresentable**: one union of states instead of `isLoading`, `error`, `data` booleans that can contradict each other.
- Utility types (`Partial`, `Required`, `Pick`, `Omit`, `Record`, `ReturnType`, `Awaited`), generics with constraints (`<T extends { id: number }>`), mapped and conditional types (`T extends string ? ... : ...`).
- `satisfies` checks a value against a type without widening it; `as const` keeps literal types.
- `as` casts are promises nobody checks: every `as` is a place a bug can hide. Prefer narrowing and validation.

---

## 2. Validating data at runtime

Types are erased, so `const user = await res.json() as User` trusts whatever the server (or an attacker, or an old cached version) sent. At every **boundary** (HTTP responses, `localStorage`, URL parameters, `postMessage`, form input) validate `unknown` into typed values:
- hand-written narrowing with type guards (`function isUser(x: unknown): x is User`);
- or a schema library (Zod, Valibot) that gives you the type and the validator from one definition: `const User = z.object({...}); type User = z.infer<typeof User>`;
- report all errors at once for forms; fail loudly (and log) for API contract violations;
- copy only the fields you expect: blindly spreading parsed JSON into objects can carry `__proto__` and other surprises (**prototype pollution**).

Ideally the backend publishes an OpenAPI description and the frontend generates its types and clients from it, so contract drift is a build error.

> **Kata `validate`**: parse `unknown` into a `User` or a list of problems, without `any`.

---

## 3. Memory leaks in single-page apps

A SPA can run for days in one tab, so leaks accumulate:
- **Listeners never removed**: `window.addEventListener`, emitters, WebSocket subscriptions, `setInterval`. Every subscribe needs an unsubscribe (React: the effect's cleanup function).
- **Closures capturing big objects** that outlive their use (a cache holding every response; a handler capturing a large array).
- **Detached DOM nodes** still referenced from JavaScript.
- Unbounded caches and arrays (logs, history): cap them (LRU, Java core L2).

Find them with the browser's memory tools: take heap snapshots before and after repeating an action, compare, look at what retains the growing objects.

> **Kata `emitter`**: a typed emitter whose unsubscribe semantics survive removals during emit and failing listeners.

---

## 4. Performance

- **Network first**: fewer and smaller requests, compression, HTTP caching with hashed asset names (`app.3f9c1.js` cached forever, `index.html` revalidated), CDNs, code splitting (`import()` loads a route's code on demand), preloading critical resources.
- **Main thread**: long tasks (> 50 ms) block input; break work up, use Web Workers for heavy computation, virtualise long lists (render only the visible rows).
- **Core Web Vitals** measure what users feel: LCP (largest content painted, loading), INP (interaction to next paint, responsiveness), CLS (layout shift, visual stability).
- Measure with the browser's Performance panel and Lighthouse, then fix the biggest bottleneck; avoid micro-optimising code that is not on the hot path.

---

## 5. Tooling: from TypeScript to the browser

- **Type checking** (`tsc --noEmit`) and **transpiling** are separate: Vite/esbuild/SWC strip types very fast without checking them, so CI must run `tsc` (or the build fails silently on type errors in some setups).
- **Bundlers** (Vite, which uses Rollup/Rolldown for builds; webpack in older projects) resolve imports, bundle, minify, tree-shake and split code; the dev server serves ES modules directly with hot module replacement.
- **Linting** (ESLint with typescript-eslint) catches bugs types cannot (unused promises, missing hook dependencies), **formatting** (Prettier) ends style debates.
- **Testing**: Vitest/Jest for units and components, Testing Library for user-centric component tests, Playwright for end-to-end in real browsers.
- **Node vs browser**: same language, different APIs (no DOM in Node; `fs`, `process` only in Node; `fetch` in both now).

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `validate` | `unknown`, type guards, discriminated `Result`, safe copying |
| `emitter` | generic event maps, `keyof`, unsubscribe semantics, snapshot iteration, `AggregateError` |

```
cd web-katas && npx vitest run src/js/l3
```

## 7. Seen in the shop project

- `frontend/`: strict TypeScript with `tsc --noEmit` in the build script, Vite for dev and production builds, Vitest + Testing Library tests, hashed assets served by nginx with long cache headers (`frontend/nginx`).

## 8. Interview talk track

"In TypeScript I model state as discriminated unions so impossible combinations do not compile, use `unknown` at boundaries and validate it with type guards or a schema library, since types are erased at runtime, and keep `as` casts rare. On the async side I know the event loop's task and microtask ordering, never block the main thread, bound concurrency, and give every request a timeout or an AbortSignal. In long-lived SPAs every subscription has a matching cleanup and caches are bounded. For performance I measure first, with Core Web Vitals and the browser's profiler, and optimise the network and main-thread work that actually dominate. CI type-checks, lints and tests, because the bundler only strips types."

**Questions you should be able to answer:**
1. What is a discriminated union, and how do you make a `switch` over it exhaustive?
2. Why is `res.json() as User` dangerous? What do you do instead?
3. `Partial`, `Pick`, `Omit`, `Record`: what does each produce?
4. What is prototype pollution?
5. Name three causes of memory leaks in a SPA and how to find them.
6. What are LCP, INP and CLS?
7. Why does Vite not catch type errors, and what do you do about it?
8. How does code splitting work, and when does it help?
