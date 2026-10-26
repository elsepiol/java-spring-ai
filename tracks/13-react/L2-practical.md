# React, L2: effects, data fetching, custom hooks, forms, context, routing

**Goal:** use hooks correctly (especially `useEffect`, the most misused one), fetch data without race conditions, extract logic into custom hooks, build validated accessible forms, and share state with context and routing.

Prerequisite: L1, JS/TS L2 (promises, the event loop). Katas: `cd web-katas && npx vitest run src/react/l2`. Drill: `java quiz/Quiz.java --track react --level 2`.

---

## 1. Hooks and their rules

Hooks (`useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, `useContext`, `useReducer`, custom `useX`) attach state and behaviour to a component **by call order**. Hence the **rules of hooks**: call them only at the top level of a component or custom hook, never in conditions, loops or after an early return (the order must be the same every render). The ESLint plugin enforces it.

- `useRef`: a mutable box that survives renders without causing them (DOM elements, timers, previous values).
- `useReducer`: state transitions as `(state, action) => newState`, for complex state (a cart, a wizard).

---

## 2. Effects: synchronising with the outside world

`useEffect(fn, deps)` runs **after** React has updated the screen, whenever a dependency changed; the function it returns is the **cleanup**, run before the next effect and on unmount.

```tsx
useEffect(() => {
  const controller = new AbortController();
  let active = true;
  fetch(`/api/products?q=${query}`, { signal: controller.signal })
    .then((r) => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
    .then((data) => { if (active) setResults(data); })
    .catch((e) => { if (active && e.name !== 'AbortError') setError(e); });
  return () => { active = false; controller.abort(); };       // the next query (or unmount) cancels this one
}, [query]);
```

- Effects are for **synchronising with external systems**: network, subscriptions, timers, the DOM, non-React widgets. **You might not need an effect** for derived data (compute it during render), for reacting to a user event (do it in the handler), or for resetting state when a prop changes (use a `key`).
- **Dependencies**: list every value from the component used inside (the lint rule tells you). Missing ones cause stale values; objects and functions created during render change every render and re-run the effect (memoise them or move them inside the effect).
- **Race conditions**: responses can arrive out of order; ignore stale ones (`active` flag) and abort them.
- In development, **Strict Mode runs effects twice** (mount, cleanup, mount) to expose missing cleanups.
- In real apps, prefer a data-fetching library (**TanStack Query**, SWR) or your framework's loaders: caching, deduplication, retries, background refresh and race handling built in.

> **Kata `useFetch`**: a fetching hook with discriminated state, HTTP error handling, aborts and stale-response protection.

---

## 3. Forms that work for everyone

- Validate on submit (and optionally on blur); show messages **next to the field**, connect them with `aria-describedby`, mark fields `aria-invalid`; move focus to the first error or announce a summary.
- Disable the submit button while submitting (no double orders) and show progress; re-enable on failure; show server errors (`role="alert"`).
- Never trust client validation alone: the server validates again (Spring Boot L2) and its field errors map back to fields.
- Big forms: React Hook Form (uncontrolled inputs, fast) with a schema (Zod) shared between validation and types; React 19 also has form actions (`<form action={fn}>`, `useActionState`, `useFormStatus`).

> **Kata `SignupForm`**: validation, accessible error messages, pending state, server errors.

---

## 4. Custom hooks

A custom hook is a function starting with `use` that calls other hooks, to reuse **stateful logic** (not state: each component calling it gets its own state): `useFetch`, `useDebouncedValue`, `useLocalStorageState`, `useMediaQuery`. Test them with `renderHook`.

---

## 5. Sharing state: props, context, stores

- Start with **lifting state up** and props.
- **Context** (`createContext` + a provider + `useContext`) passes values deep without prop drilling: the current user, the theme, the locale, the API client. Every consumer re-renders when the context value changes, so keep it for things that change rarely, or split contexts.
- **Server state** (data from the API) belongs in a query cache (TanStack Query), not in a global store.
- **Client state** shared widely (a cart, a multi-step wizard): a reducer + context, or a small store (Zustand, Redux Toolkit).
- URL state: filters, pagination and the selected tab belong in the URL (shareable, back button works).

---

## 6. Routing

A router (React Router, TanStack Router, or a framework) maps URLs to components, with nested layouts, URL parameters (`/orders/:id`), navigation without full reloads, and protected routes (redirect to login when unauthenticated: a UX convenience, the API still enforces security). Code-split by route (`lazy`) so each page loads only its code.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `useFetch` | `useEffect` with cleanup, `AbortController`, stale responses, discriminated state, `renderHook` |
| `SignupForm` | controlled forms, validation, `aria-invalid`/`aria-describedby`, pending and error states |

```
cd web-katas && npx vitest run src/react/l2
```

## 8. Seen in the shop project

- `frontend/src/auth/ShopContext.tsx`: a context providing the logged-in user and API client; `cart/CartContext.tsx`: cart state shared through context.
- `pages/NotificationsPage.tsx`: polling in an effect with an interval and a cancelled flag in the cleanup.
- `App.tsx`: React Router routes, with a guard that redirects to `/login` (remembering where the user wanted to go) for pages that need a login.

## 9. Interview checklist

1. What are the rules of hooks, and why do they exist?
2. When does an effect run? What is the cleanup for?
3. Name three situations where you should NOT use `useEffect`.
4. How do you avoid race conditions when fetching in an effect?
5. Why does React Strict Mode run effects twice in development?
6. What goes in the dependency array? What happens with objects or functions in it?
7. Context vs props vs a store. What problem can context cause?
8. Server state vs client state: where does each belong?
9. How do you make form errors accessible?
