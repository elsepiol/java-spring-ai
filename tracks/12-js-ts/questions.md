# JavaScript and TypeScript: question bank

Drill with `java quiz/Quiz.java --track js-ts`.

## L1

### JS-L1-001 · recall
`==` vs `===`?

<details><summary>Answer</summary>

`===` compares type and value without conversion. `==` converts types first by complicated rules (`'1' == 1`, `0 == ''`, `null == undefined` are true). Always use `===` (the one common exception some teams allow: `x == null` to test for null or undefined).

</details>

### JS-L1-002 · recall
Which values are falsy in JavaScript?

<details><summary>Answer</summary>

`false`, `0`, `-0`, `0n`, `''` (empty string), `null`, `undefined` and `NaN`. Everything else is truthy, including `'0'`, `'false'`, `[]` and `{}`.

</details>

### JS-L1-003 · bug
`const pageSize = options.pageSize || 20;` A caller passes `pageSize: 0` to mean "no paging". What happens, and what is the fix?

<details><summary>Answer</summary>

0 is falsy, so `||` replaces it with 20. Use nullish coalescing: `options.pageSize ?? 20`, which only replaces `null` and `undefined`.

</details>

### JS-L1-004 · predict
What do `{a: 1} === {a: 1}`, `[1] === [1]` and `NaN === NaN` evaluate to?

<details><summary>Answer</summary>

All false. Objects and arrays compare by reference (two different objects), and NaN is not equal to anything, itself included (`Number.isNaN(x)` or `Object.is(x, NaN)` to test).

</details>

### JS-L1-005 · recall
`var` vs `let` vs `const`?

<details><summary>Answer</summary>

`var` is function-scoped, hoisted (usable before its declaration as `undefined`) and can be redeclared. `let` and `const` are block-scoped and in a temporal dead zone before their declaration. `const` cannot be reassigned, but the object it refers to can still be mutated. Default to `const`, use `let` when reassigning, never `var`.

</details>

### JS-L1-006 · recall
What is a closure?

<details><summary>Answer</summary>

A function together with the variables from the scope where it was created, which it keeps access to even after that scope has returned. Used for private state (`counter()`), callbacks, partial application, memoisation and React hooks.

</details>

### JS-L1-007 · predict
```js
const fns = [];
for (var i = 0; i < 3; i++) fns.push(() => i);
console.log(fns.map(f => f()));
```
What is printed, and why? How does `let` change it?

<details><summary>Answer</summary>

`[3, 3, 3]`: `var` creates one function-scoped `i` shared by all three closures, and it is 3 when they run. With `let`, each iteration gets a fresh binding, so `[0, 1, 2]`.

</details>

### JS-L1-008 · recall
How is `this` determined in a regular function, and how do arrow functions differ?

<details><summary>Answer</summary>

For regular functions `this` is set by the call: `obj.f()` → obj; plain `f()` → undefined in strict mode; `new F()` → the new object; `f.call(x)`/`bind(x)` → x. Arrow functions have no own `this`: they use the `this` of the scope they were defined in, which makes them right for callbacks.

</details>

### JS-L1-009 · bug
`button.addEventListener('click', this.handleClick)` inside a class; `handleClick` uses `this.count` and crashes. Why, and the fixes?

<details><summary>Answer</summary>

The method is passed as a detached function, so `this` inside it is not the instance (it is the button or undefined). Fix: `this.handleClick.bind(this)`, an arrow wrapper `() => this.handleClick()`, or define `handleClick = () => {...}` as an arrow-function class field.

</details>

### JS-L1-010 · recall
What does the spread operator do with objects, and is the copy deep?

<details><summary>Answer</summary>

`{...a, b: 2}` creates a new object with a's own enumerable properties, then overrides `b`. It is a shallow copy: nested objects are shared with the original. Use `structuredClone` for deep copies of plain data.

</details>

### JS-L1-011 · recall
`map`, `filter`, `reduce`, `find`, `some`: what does each do?

<details><summary>Answer</summary>

`map` transforms each element into a new array; `filter` keeps elements matching a predicate; `reduce` folds elements into one value; `find` returns the first matching element (or undefined); `some`/`every` test whether any/all match. All return new values without mutating the array (unlike `sort`, `reverse`, `splice`, which mutate; `toSorted` does not).

</details>

### JS-L1-012 · predict
What does `0.1 + 0.2 === 0.3` give, and how do you handle money in JavaScript?

<details><summary>Answer</summary>

false (0.30000000000000004): numbers are IEEE-754 doubles. Store money as integer cents (or `bigint`), format with `Intl.NumberFormat`, and let the backend do authoritative calculations.

</details>

### JS-L1-013 · recall
`null` vs `undefined`?

<details><summary>Answer</summary>

`undefined` means "no value assigned": missing properties, missing arguments, functions without a return. `null` is an explicit "no value" set by a programmer or an API. JSON has null but no undefined (properties with undefined are dropped by JSON.stringify).

</details>

### JS-L1-014 · recall
What do `package.json` and `package-lock.json` each do? `npm install` vs `npm ci`?

<details><summary>Answer</summary>

`package.json` declares dependencies with version ranges plus scripts and metadata. `package-lock.json` records the exact resolved version of every package in the tree (commit it). `npm install` resolves and may update the lock file; `npm ci` installs exactly what the lock file says from scratch (CI, Docker builds) and fails if it disagrees with package.json.

</details>

### JS-L1-015 · recall
What does the caret in `"react": "^19.3.0"` allow?

<details><summary>Answer</summary>

Any version compatible under semver: >= 19.3.0 and < 20.0.0 (minor and patch updates). `~19.3.0` allows only patches (< 19.4.0); an exact `19.3.0` allows nothing else. The lock file still pins what is installed.

</details>

### JS-L1-016 · recall
Structural vs nominal typing: which does TypeScript use?

<details><summary>Answer</summary>

Structural: a value is compatible with a type if it has the required shape, whatever it was declared as (Java is nominal: a class must declare `implements`). So `{ id: 1, name: 'x' }` is a valid `Product` if Product only requires id and name.

</details>

### JS-L1-017 · recall
`unknown` vs `any`?

<details><summary>Answer</summary>

`any` disables type checking for that value and spreads silently. `unknown` accepts any value but forces you to narrow it (typeof, instanceof, type guards) before using it. Use `unknown` for untrusted input and caught errors; avoid `any`.

</details>

### JS-L1-018 · predict
Is this valid TypeScript, and what happens at runtime? `const p = JSON.parse(text) as Product; console.log(p.name.toUpperCase());`

<details><summary>Answer</summary>

It compiles (JSON.parse returns `any`, and `as` asserts without checking). At runtime nothing verifies the shape: if `name` is missing it throws "Cannot read properties of undefined". Types are erased at runtime; validate external data.

</details>

### JS-L1-019 · interview
"What are the biggest differences a Java developer should know about JavaScript?"

<details><summary>Answer</summary>

Dynamic typing (TypeScript adds static types that are erased), one thread with an event loop instead of threads, functions as first-class values and closures everywhere, `this` decided by how a function is called, `null` and `undefined`, a single floating-point `number`, objects as property bags compared by reference with no equals/hashCode, prototype-based classes, ES modules and npm with lock files, and a browser runtime (DOM, security model).

</details>

### JS-L1-020 · dsa
How would you remove duplicates from an array of user ids while keeping the first occurrence order, and what is the complexity?

<details><summary>Answer</summary>

`[...new Set(ids)]`: a Set keeps insertion order and ignores repeats, O(n) time and space. For objects deduplicated by a key: a Map from key to the first object (`new Map(users.map(u => [u.id, u]))` keeps the last; check `has` before set to keep the first). The naive `filter((x, i) => ids.indexOf(x) === i)` is O(n²).

</details>

## L2

### JS-L2-001 · recall
Explain the JavaScript event loop.

<details><summary>Answer</summary>

JavaScript runs on one thread with a call stack. Asynchronous work is done by the environment and its callbacks are queued. When the stack is empty, the loop drains the whole microtask queue (promise callbacks, await continuations, queueMicrotask), the browser may render, then the loop runs ONE task from the task queue (timers, I/O, events), then microtasks again, and so on.

</details>

### JS-L2-002 · predict
```js
console.log('A');
setTimeout(() => console.log('B'), 0);
Promise.resolve().then(() => console.log('C'));
console.log('D');
```
Output order?

<details><summary>Answer</summary>

A, D, C, B. Synchronous code first; then microtasks (the promise callback C); then the timer task B, even with 0 ms.

</details>

### JS-L2-003 · predict
```js
async function f() { console.log(1); await null; console.log(2); }
console.log(0); f(); console.log(3);
```
Output order?

<details><summary>Answer</summary>

0, 1, 3, 2. An async function runs synchronously until its first await; the code after the await continues as a microtask, after the current synchronous code (`console.log(3)`) finishes.

</details>

### JS-L2-004 · scenario
Clicking "Export" freezes the page for 3 seconds while a big CSV is generated. Why, and what can you do?

<details><summary>Answer</summary>

The work runs synchronously on the main thread, which also handles input and rendering, so nothing else can happen until it finishes. Options: move it to a Web Worker, split it into chunks yielding to the event loop (setTimeout/scheduler.yield), stream it, or generate the file on the server.

</details>

### JS-L2-005 · recall
What states does a Promise have?

<details><summary>Answer</summary>

Pending, then settled exactly once: fulfilled with a value or rejected with a reason. Callbacks attached with then/catch/finally (or await) run as microtasks after settlement, even if attached later.

</details>

### JS-L2-006 · recall
`Promise.all` vs `Promise.allSettled` vs `Promise.race` vs `Promise.any`?

<details><summary>Answer</summary>

`all`: fulfils with all values in input order, rejects on the first rejection. `allSettled`: waits for every promise and reports each outcome. `race`: settles like the first promise to settle (fulfilled or rejected). `any`: fulfils with the first fulfilment, rejects with an AggregateError only if all reject.

</details>

### JS-L2-007 · bug
```js
async function saveAll(items) {
  items.forEach(async (item) => { await api.save(item); });
  console.log('all saved');
}
```
Problem?

<details><summary>Answer</summary>

`forEach` ignores the promises returned by the async callback, so "all saved" is logged immediately, failures become unhandled rejections and `saveAll` resolves before anything is saved. Use `for (const item of items) await api.save(item)` (sequential) or `await Promise.all(items.map(i => api.save(i)))` (parallel, ideally with a concurrency limit).

</details>

### JS-L2-008 · bug
`const res = await fetch('/api/orders'); const orders = await res.json();` The server answers 500 with an HTML error page. What happens?

<details><summary>Answer</summary>

fetch only rejects on network failures; a 500 is a fulfilled response. `res.json()` then fails to parse HTML with a SyntaxError, a confusing error. Check `res.ok` (or `res.status`) first and handle error bodies (e.g. problem details) explicitly.

</details>

### JS-L2-009 · design
You must call an API for 2,000 ids. Why not `Promise.all(ids.map(fetchOne))`, and what instead?

<details><summary>Answer</summary>

It starts 2,000 requests at once: the browser queues them per host anyway, the server sees a burst (rate limits, overload), memory grows, and one failure rejects everything. Use a concurrency limit (e.g. 5-10 in flight with a small pool like p-limit or mapLimit), a batch endpoint if available, and per-request error handling.

</details>

### JS-L2-010 · recall
How do you cancel a fetch or give it a timeout?

<details><summary>Answer</summary>

Pass an AbortSignal: `const c = new AbortController(); fetch(url, { signal: c.signal }); c.abort();`, or `fetch(url, { signal: AbortSignal.timeout(5000) })`. The promise rejects with an AbortError/TimeoutError; ignore it if the abort was intentional.

</details>

### JS-L2-011 · recall
What is an unhandled promise rejection, and why does it matter?

<details><summary>Answer</summary>

A rejected promise with no handler attached. Browsers log it (and fire `unhandledrejection`); Node terminates the process by default. It usually means a missing await or catch, so an error went unnoticed.

</details>

### JS-L2-012 · recall
Debounce vs throttle?

<details><summary>Answer</summary>

Debounce waits until events stop for N ms, then runs once (search input, autosave, resize end). Throttle runs at most once per N ms while events continue (scroll, mousemove, progress updates), often with a leading and/or trailing call.

</details>

### JS-L2-013 · recall
How do you test code that uses timers without waiting?

<details><summary>Answer</summary>

Fake timers: `vi.useFakeTimers()` (or Jest's), then `vi.advanceTimersByTime(ms)` / `runAllTimers()` to move virtual time; for promises combined with timers use the async variants (`advanceTimersByTimeAsync`). Restore with `vi.useRealTimers()`.

</details>

### JS-L2-014 · predict
```js
setTimeout(() => console.log('timeout'), 0);
Promise.resolve().then(function loop() { Promise.resolve().then(loop); });
```
What happens?

<details><summary>Answer</summary>

"timeout" never prints and the page hangs: each microtask queues another, and the microtask queue is drained completely before any task (timer) or rendering can run. Microtask starvation.

</details>

### JS-L2-015 · recall
Why do JavaScript errors in `catch (e)` have type `unknown` in strict TypeScript, and how do you handle them?

<details><summary>Answer</summary>

Anything can be thrown (strings, objects, Errors), so TypeScript cannot know. Narrow it: `if (e instanceof HttpError) ...`, `e instanceof Error ? e.message : String(e)`, and rethrow what you cannot handle.

</details>

### JS-L2-016 · recall
What does Node do differently from the browser regarding the event loop and APIs?

<details><summary>Answer</summary>

Same JavaScript and event loop idea, but libuv phases (timers, I/O polling, check for setImmediate, close callbacks), `process.nextTick` callbacks run before promise microtasks, and different APIs (fs, net, process, no DOM). One Node process serves many connections on one thread, so CPU-bound work must go to worker threads.

</details>

### JS-L2-017 · scenario
A search box fires a request per keystroke, and results sometimes show the answer for an older query. Why, and how do you fix it?

<details><summary>Answer</summary>

Responses arrive out of order: the request for "ja" can finish after the one for "java" and overwrite it. Debounce the input, and ignore or abort stale requests: an AbortController per request (abort the previous one) or compare a request id/the current query before applying the result.

</details>

### JS-L2-018 · recall
What is `structuredClone`, and what can't it copy?

<details><summary>Answer</summary>

A built-in deep copy using the structured clone algorithm (the one postMessage uses): handles nested objects, arrays, Maps, Sets, Dates and cycles. It cannot copy functions, DOM nodes, class prototypes (instances become plain objects) or symbols-keyed properties.

</details>

### JS-L2-019 · interview
"How is concurrency different in Node.js compared to a Spring Boot application?"

<details><summary>Answer</summary>

Spring MVC traditionally handles each request on its own thread (blocking I/O, thread pool sizing, shared-state synchronisation; virtual threads make blocking cheap). Node handles all requests on one thread with non-blocking I/O and callbacks/promises: no locks needed for in-memory state, very efficient for I/O-bound work, but one CPU-heavy or blocking call stalls every request, so CPU work goes to workers. Spring WebFlux is the Java counterpart of the event-loop model.

</details>

### JS-L2-020 · dsa
How would you implement a concurrency-limited `mapLimit(items, k, fn)` and what is its running time compared to sequential and unbounded execution?

<details><summary>Answer</summary>

Start k workers; each takes the next index from a shared counter, awaits fn, stores the result at that index, and takes the next one until none remain. With n items of duration d: sequential ≈ n·d, unbounded ≈ d (if the backend copes), limited ≈ ceil(n/k)·d. It is a worker pool over a queue, like a fixed thread pool in Java.

</details>

## L3

### JS-L3-001 · recall
What is a discriminated union, and why is it better than several booleans for UI state?

<details><summary>Answer</summary>

A union of object types sharing a literal "tag" field: `{status: 'loading'} | {status: 'error', error} | {status: 'ready', data}`. TypeScript narrows on the tag, so `data` only exists in the ready state. Separate `isLoading`, `error`, `data` fields allow contradictory combinations (loading with an error and stale data) that every component must guess about.

</details>

### JS-L3-002 · recall
How do you make a `switch` over a union exhaustive?

<details><summary>Answer</summary>

In the `default` branch pass the value to a function taking `never` (`assertNever(x)`). If a new member is added to the union and not handled, the value is no longer `never` there and compilation fails.

</details>

### JS-L3-003 · recall
What do `Partial<T>`, `Pick<T, K>`, `Omit<T, K>` and `Record<K, V>` produce?

<details><summary>Answer</summary>

Partial: all properties optional (patch payloads). Pick: only the listed keys. Omit: all except the listed keys (e.g. a create DTO without `id`). Record: an object type with keys K and values V (`Record<Status, string>` for labels per status).

</details>

### JS-L3-004 · why
Why is `await res.json() as User` risky, and what are the alternatives?

<details><summary>Answer</summary>

`as` is an unchecked assertion: if the API changed, returned an error body or was tampered with, the code proceeds with wrong data and fails somewhere far away. Validate at the boundary: type guards, a schema library (Zod/Valibot) that infers the TypeScript type from the schema, or generated clients from an OpenAPI contract plus validation.

</details>

### JS-L3-005 · recall
What is a type guard?

<details><summary>Answer</summary>

A function returning a type predicate (`function isUser(x: unknown): x is User`) whose true result narrows the argument's type in the calling code. TypeScript trusts the predicate, so the implementation must really check.

</details>

### JS-L3-006 · recall
What is prototype pollution?

<details><summary>Answer</summary>

Merging attacker-controlled JSON into objects (deep merge, `Object.assign` of parsed input) with keys like `__proto__` or `constructor.prototype` modifies `Object.prototype`, adding properties to every object in the app, which can bypass checks (`if (user.isAdmin)`) or crash code. Avoid deep-merging untrusted input, copy known fields explicitly, use `Object.create(null)` maps or `Map`.

</details>

### JS-L3-007 · recall
`satisfies` vs a type annotation?

<details><summary>Answer</summary>

`const routes = {...} satisfies Record<string, Route>` checks that the value matches the type but keeps the value's own, more precise inferred type (literal keys, specific values). An annotation `const routes: Record<string, Route>` widens it to the annotated type, losing that precision.

</details>

### JS-L3-008 · scenario
A dashboard tab's memory grows from 80 MB to 1.5 GB over a working day. Likely causes and how to investigate?

<details><summary>Answer</summary>

Listeners, intervals or subscriptions never removed (components unmounted without cleanup), unbounded caches or arrays (logs, chart history), closures retaining large data, detached DOM nodes. Take heap snapshots in DevTools before and after repeating an action several times, compare, and follow the retainer paths of the growing objects; then add cleanup and bounds.

</details>

### JS-L3-009 · recall
Why must every subscription in a long-lived page have a matching unsubscribe?

<details><summary>Answer</summary>

The emitter or global object keeps a reference to the listener, and the listener's closure keeps references to the component's state and DOM, so nothing can be garbage collected, and the old listener keeps running (duplicate requests, errors on unmounted UI). Return and call cleanup functions (React effect cleanups, `removeEventListener`, `clearInterval`, `AbortController` for listeners via `{ signal }`).

</details>

### JS-L3-010 · recall
What are the Core Web Vitals?

<details><summary>Answer</summary>

LCP (Largest Contentful Paint: loading, good under 2.5 s), INP (Interaction to Next Paint: responsiveness, good under 200 ms) and CLS (Cumulative Layout Shift: visual stability, good under 0.1), measured from real users at the 75th percentile.

</details>

### JS-L3-011 · recall
What is code splitting?

<details><summary>Answer</summary>

Splitting the bundle so code is loaded when needed: dynamic `import()` (React.lazy for routes and heavy components) makes the bundler emit separate chunks fetched on demand. It shrinks the initial download and speeds up first load, at the cost of a request when the chunk is first used (mitigated with preloading).

</details>

### JS-L3-012 · why
Why do hashed file names (`app.3f9c1a.js`) allow aggressive caching?

<details><summary>Answer</summary>

The name changes whenever the content changes, so the file can be cached "forever" (`Cache-Control: max-age=31536000, immutable`); a new release references new names from `index.html`, which itself must not be cached long (`no-cache`), so users get the new version immediately.

</details>

### JS-L3-013 · bug
The production build succeeds, but the app crashes on a property that does not exist on a type. `tsc` would have reported it. Why did the build pass?

<details><summary>Answer</summary>

Vite/esbuild/SWC only strip types; they do not type-check, for speed. Type errors are invisible unless `tsc --noEmit` (or vue-tsc etc.) runs. Add it to the build script and CI (as the shop's `"build": "tsc --noEmit && vite build"` does).

</details>

### JS-L3-014 · recall
What does tree shaking need to work?

<details><summary>Answer</summary>

Static ES module imports/exports (so the bundler can see which exports are used), code without side effects at module level (or `"sideEffects": false` in package.json), and production builds with minification. CommonJS `require` and barrel files re-exporting everything hinder it.

</details>

### JS-L3-015 · scenario
A list of 20,000 rows makes scrolling and typing slow. What do you do?

<details><summary>Answer</summary>

Virtualise: render only the visible rows plus a small buffer (react-window, TanStack Virtual), paginate or filter on the server, and avoid re-rendering all rows on every keystroke (memoised rows, deferred values). Measure with the Performance panel before and after.

</details>

### JS-L3-016 · recall
What is a Web Worker, and what can't it do?

<details><summary>Answer</summary>

A separate JavaScript thread with its own event loop for CPU-heavy work (parsing, image processing, crypto) that communicates with the page via postMessage (structured clone or transferable objects). It cannot access the DOM or the page's variables directly.

</details>

### JS-L3-017 · recall
ESLint vs TypeScript: what does each catch?

<details><summary>Answer</summary>

TypeScript catches type errors (wrong properties, wrong argument types, null access). ESLint (with typescript-eslint and plugins) catches patterns that are type-correct but buggy or risky: floating promises, missing React hook dependencies, unused variables, `==`, accidental `any`. Prettier only formats.

</details>

### JS-L3-018 · design
How would you keep frontend and backend types in sync?

<details><summary>Answer</summary>

Make the backend's API contract the source of truth: publish OpenAPI from Spring (springdoc) and generate TypeScript types or clients (openapi-typescript, orval) in the frontend build, so changes break compilation. Add runtime validation at the boundary for safety and contract tests for critical endpoints.

</details>

### JS-L3-019 · interview
"How do you make a large TypeScript frontend codebase maintainable?"

<details><summary>Answer</summary>

Strict TypeScript with no `any`, discriminated unions for state, validated boundaries, generated API types, small modules with clear ownership (feature folders), shared UI components, lint and format in CI, tests at the right levels (unit, component with Testing Library, a few Playwright E2E), code splitting by route, performance budgets, and dependency updates kept current.

</details>

### JS-L3-020 · dsa
An emitter must allow listeners to unsubscribe during an emit without skipping others. Compare iterating a copy vs copy-on-write, and their costs.

<details><summary>Answer</summary>

Copy on emit: snapshot the listener array at the start of every emit (O(n) per emit, cheap writes). Copy-on-write: never mutate the array; subscribe/unsubscribe create a new array (O(n) per change) and emit iterates the current reference with no copy. Copy-on-write is better when emits are frequent and subscriptions rare, the same trade-off as Java's CopyOnWriteArrayList.

</details>
