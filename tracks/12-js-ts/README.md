# Track 12: JavaScript and TypeScript

The language of the frontend, for a Java developer: values, equality, closures, `this`, modules and TypeScript's types; then the event loop, promises and timers; then advanced types, runtime validation, memory leaks, performance and tooling.

| Level | Lesson | Katas (`web-katas/src/js/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): JS vs Java, equality and truthiness, scope, closures, `this`, objects and arrays, modules and npm, TypeScript basics | `l1/deepEqual`, `l1/closures` |
| L2 Practical | [L2-practical.md](L2-practical.md): the event loop (tasks and microtasks), promises and async/await, concurrency limits, cancellation, debounce and throttle | `l2/eventLoop` (predict the order), `l2/promises`, `l2/timing` |
| L3 Production | [L3-production.md](L3-production.md): discriminated unions and utility types, runtime validation, prototype pollution, memory leaks, Core Web Vitals, bundling and tooling | `l3/validate`, `l3/emitter` |

```
cd web-katas && npm install
npx vitest run src/js/l1          # one level; npx vitest src/js/l1 to watch
java quiz/Quiz.java --track js-ts
```

Question bank: [questions.md](questions.md) (60 questions).
