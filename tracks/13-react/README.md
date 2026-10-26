# Track 13: React

React from the first component to production: state, events, forms, lists and Testing Library; then effects, data fetching, custom hooks, accessible forms, context and routing; then rendering performance, error boundaries, persistence, accessibility, frontend security and architecture.

| Level | Lesson | Katas (`web-katas/src/react/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): UI as a function of state, JSX, props and state, events and forms, lists and keys, testing by role | `l1/Counter`, `l1/TodoList` |
| L2 Practical | [L2-practical.md](L2-practical.md): rules of hooks, effects and cleanups, race-free data fetching, forms, custom hooks, context and stores, routing | `l2/useFetch`, `l2/SignupForm` |
| L3 Production | [L3-production.md](L3-production.md): re-renders and memoisation, localStorage safely, error boundaries and failure states, a11y, XSS and secrets, CSR/SSR/RSC, architecture | `l3/useLocalStorageState`, `l3/ErrorBoundary` |

```
cd web-katas && npm install
npx vitest run src/react/l1
java quiz/Quiz.java --track react
```

The shop project's `frontend/` (React 19 + Vite + React Router) is the full-size example of everything here.

Question bank: [questions.md](questions.md) (60 questions).
