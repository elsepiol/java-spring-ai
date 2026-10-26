# JavaScript and TypeScript, L2: the event loop, promises, async code, timers

**Goal:** understand how one thread handles thousands of concurrent operations, predict execution order, write correct promise-based code (errors, timeouts, concurrency limits, cancellation), and control timing with debounce and throttle.

Prerequisite: L1, Concurrency L1 (for contrast). Katas: `cd web-katas && npx vitest run src/js/l2`. Drill: `java quiz/Quiz.java --track js-ts --level 2`.

---

## 1. The event loop

JavaScript runs your code on **one thread**. Slow work (network, timers, file I/O) is handed to the environment (browser or Node's libuv), and its callbacks are queued:

```
call stack (sync code runs to completion, never interrupted)
   ↓ empty?
microtask queue: promise callbacks (.then, await continuations), queueMicrotask  -> ALL of them, including ones queued meanwhile
   ↓ empty?
(browser: render if needed)
task queue: setTimeout/setInterval, I/O callbacks, UI events                     -> ONE task, then microtasks again
```

Consequences:
- `setTimeout(fn, 0)` runs **after** all current sync code and all pending microtasks.
- An `async` function runs **synchronously until its first `await`**; the rest continues as a microtask.
- A long synchronous loop (parsing a 50 MB JSON, a heavy calculation) **blocks everything**: no clicks, no rendering, no other requests in Node. Split the work, move it to a Web Worker / worker thread, or do it on the server.
- An endless chain of microtasks starves rendering and timers.
- Node: same model (plus `process.nextTick` before promise microtasks, and phases for timers and I/O); this is why one Node process handles many connections without threads, and why CPU-heavy work hurts it.

Compare with Java: a Java server gives each request a thread (or a virtual thread); JavaScript gives every request the same thread and never blocks it.

> **Kata `eventLoop`**: predict the order of logs for three snippets, then run them.

---

## 2. Promises and async/await

A **Promise** is a value that will be available later: pending → fulfilled (value) or rejected (reason), once.

```ts
async function loadOrder(id: number): Promise<Order> {
  const res = await fetch(`/api/orders/${id}`);           // fetch only rejects on NETWORK errors
  if (!res.ok) throw new HttpError(res.status);            // 404/500 must be checked yourself
  return (await res.json()) as Order;                      // (validate it: L3)
}

try { const order = await loadOrder(42); } catch (e) { showError(e); }
```

Combinators:
- `Promise.all([a, b])`: parallel, results in input order, **rejects on the first failure** (the others keep running but are ignored).
- `Promise.allSettled`: waits for all, gives `{status, value | reason}` for each.
- `Promise.race`: the first to settle; `Promise.any`: the first to fulfil.
- Sequential vs parallel: `for (const id of ids) await load(id)` is sequential; `await Promise.all(ids.map(load))` is parallel and **unbounded** (1,000 ids = 1,000 simultaneous requests): use a concurrency limit.

Pitfalls:
- A forgotten `await` returns a promise instead of a value; errors become **unhandled rejections** (Node crashes on them by default).
- `array.forEach(async ...)` does not wait for anything; use `for...of` with `await` or `Promise.all(array.map(...))`.
- Errors thrown inside `.then` become rejections; always end chains with handling.
- Timeouts and cancellation: promises cannot be cancelled by themselves; `AbortController` cancels `fetch` (and anything that accepts a signal): `fetch(url, { signal: AbortSignal.timeout(5000) })`.

> **Kata `promises`**: `all` without the built-in, `withTimeout` without leaking timers, and `mapLimit` (bounded concurrency).

---

## 3. Timers: debounce and throttle

- **Debounce**: run once after events **stop** for N ms (search-as-you-type, autosave, window resize end).
- **Throttle**: run at most once per N ms while events continue (scroll position, mouse move, progress reporting).
- Always clear timers you no longer need (`clearTimeout`), especially in components that unmount (track 13).
- Tests use **fake timers** (`vi.useFakeTimers()`, `vi.advanceTimersByTime`), like an injected Clock in Java (Testing track L1).

> **Kata `timing`**: debounce with cancel, throttle with a trailing call.

---

## 4. Error handling and robustness

- Throw `Error` objects (stack traces), not strings; subclass for kinds (`class HttpError extends Error`); `cause` for wrapping (`new Error('load failed', { cause: e })`).
- `catch (e)` gives `unknown` in strict TypeScript: narrow it (`e instanceof HttpError`).
- Retries with backoff and jitter for idempotent requests only (Web track L3); show errors to users in a way they can act on.
- Global safety nets: `window.addEventListener('unhandledrejection', ...)`, error tracking (Sentry), React error boundaries (track 13).

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `eventLoop` | tasks vs microtasks, `async`/`await` ordering |
| `promises` | promise mechanics, timeouts, bounded concurrency, error propagation |
| `timing` | debounce, throttle, cancellation, fake timers |

```
cd web-katas && npx vitest run src/js/l2
```

## 6. Seen in the shop project

- `frontend/src/api/client.ts`: one `fetch` wrapper that sends the JWT, checks the status, and turns problem-details bodies into an `ApiError` with field errors and the trace id (see `client.test.ts`); `retry.ts` retries; `pages/NotificationsPage.tsx` cancels its polling when the component goes away.

## 7. Interview checklist

1. Explain the event loop. Microtasks vs tasks. Where do `setTimeout` and `.then` callbacks go?
2. What does this print? (any snippet mixing sync code, `setTimeout`, promises and `await`)
3. What happens if the main thread runs a 2-second loop?
4. `Promise.all` vs `allSettled` vs `race` vs `any`.
5. Why doesn't `fetch` reject on a 500?
6. How do you run 1,000 requests with at most 5 at a time?
7. Why is `array.forEach(async ...)` a bug?
8. How do you cancel a fetch or add a timeout?
9. Debounce vs throttle, with a use case each.
