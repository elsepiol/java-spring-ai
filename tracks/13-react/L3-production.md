# React, L3: rendering performance, resilience, persistence, accessibility, security, architecture

**Goal:** know why components re-render and when that matters, keep the app alive when parts fail, persist client state safely, build accessible interfaces, avoid frontend security holes, and structure a large React codebase (including where server rendering and React Server Components fit).

Prerequisite: L1, L2, JS/TS L3. Katas: `cd web-katas && npx vitest run src/react/l3`. Drill: `java quiz/Quiz.java --track react --level 3`.

---

## 1. Rendering performance

A component re-renders when **its state changes, its parent re-renders, or a context it uses changes**. Re-rendering is usually cheap (it is just calling functions and diffing); **measure before optimising** (React DevTools Profiler, "highlight updates").

When it matters (big lists, expensive charts, frequent updates like typing):
- **Move state down** (keep the fast-changing input's state in a small component) and **lift content up** (pass expensive children as `children` so they are not re-created).
- `React.memo(Component)` skips re-rendering when props are shallowly equal, which only works if props keep their identity: `useMemo` for computed objects/arrays and `useCallback` for handlers passed to memoised children. The **React Compiler** (React 19 ecosystem) adds this memoisation automatically.
- `useMemo` for genuinely expensive calculations (sorting/filtering thousands of rows).
- **Virtualise** long lists; **split code** by route; `useTransition`/`useDeferredValue` keep typing responsive while a heavy result updates in the background.
- Keys that change every render (random) remount whole subtrees: a classic hidden cost.

---

## 2. Persisting client state

`localStorage` (synchronous, ~5 MB, strings only, per origin) for small preferences and drafts; IndexedDB for larger data; the server for anything that matters (a cart that must survive device changes).
- Read **lazily** (`useState(() => read())`), parse defensively (old versions, corrupt values, other code writing the key), handle **storage throwing** (Safari private mode, quotas, disabled cookies), and version your stored format.
- Other tabs: the `storage` event fires in *other* tabs when a key changes; listen to keep tabs in sync.
- Never store tokens or secrets in `localStorage` if you can avoid it: any XSS can read it (Security track L2).
- Server rendering: there is no `window` on the server: access storage only in effects or event handlers, or the HTML will not match (hydration errors).

> **Kata `useLocalStorageState`**: lazy reads, failing storage, updater functions, cross-tab sync.

---

## 3. Resilience: error boundaries and failure states

- A render error **unmounts the whole tree** below the nearest error boundary; without one, the user gets a blank page. Put boundaries around routes and independent widgets, show a useful fallback with a retry, and **report** errors (Sentry and similar) with the component stack.
- Boundaries do **not** catch errors in event handlers, async code or effects' promises: handle those and set error state, or rethrow into rendering.
- Every data view has four states: **loading, error, empty, success**; design all four. Show skeletons instead of spinners for layout stability; keep the previous data visible while refreshing.
- With Suspense (`<Suspense fallback={...}>`) components can suspend while loading code (`lazy`) or data (frameworks, TanStack Query's suspense mode), and boundaries handle the errors.

> **Kata `ErrorBoundary`**: a class boundary with fallback, reporting, manual reset and reset keys.

---

## 4. Accessibility (a11y)

- **Semantic HTML first**: `<button>` for actions (not a clickable `<div>`), `<a href>` for navigation, `<label>` for inputs, headings in order, lists as lists, `<table>` for tabular data. They come with keyboard support and roles for free.
- Everything works with the **keyboard** (Tab order, visible focus, Escape closes dialogs, focus is moved into dialogs and back when they close).
- ARIA only where HTML has no element: `aria-label` for icon buttons, `aria-live`/`role="status"`/`role="alert"` for announcements, `aria-expanded` for disclosure widgets, `aria-invalid` + `aria-describedby` for form errors.
- Colour contrast (WCAG AA 4.5:1 for text), don't convey meaning by colour alone, respect `prefers-reduced-motion`.
- Test with the keyboard and a screen reader occasionally, axe in automated tests, and Testing Library's role-based queries daily.

---

## 5. Frontend security

- React escapes text, so **XSS** comes through the escape hatches: `dangerouslySetInnerHTML` (sanitise with DOMPurify), `href={userUrl}` (block `javascript:` URLs), injecting into `<script>` or style tags, third-party scripts.
- A **Content-Security-Policy** limits damage; **never put secrets in frontend code**: everything shipped to the browser is public (API keys there must be publishable ones, scoped and rate-limited).
- Tokens: prefer HttpOnly cookies via a backend-for-frontend; protect against CSRF if cookies authenticate (Security track L2).
- Authorisation in the UI (hiding buttons) is UX; the API must enforce it.
- Keep dependencies updated (`npm audit`, Dependabot); a compromised npm package runs in your users' browsers.

---

## 6. Architecture and rendering strategies

- **Feature-based folders** (`orders/`, `cart/`, `auth/`) over type-based ones (`components/`, `hooks/`); shared UI components in a design system; API access in one client module with generated types.
- **Rendering strategies**: client-side rendering (SPA: Vite + React, simple hosting, slower first paint, weaker SEO), server-side rendering (HTML per request, then hydration), static generation (pre-built pages), and **React Server Components** (Next.js App Router, React Router framework mode): components that run only on the server, fetch data directly and send no JavaScript for themselves, combined with client components (`'use client'`) for interactivity. Choose by SEO needs, first-load performance and team/infrastructure constraints.
- State placement rules (L2): server state in a query cache, URL state in the URL, local state local, global client state minimal.
- Testing: Testing Library for components and hooks, MSW (Mock Service Worker) to fake the API at the network level, Playwright end-to-end for critical journeys, visual regression for design systems.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `useLocalStorageState` | lazy initial state, defensive parsing, storage failures, `storage` events and cleanup |
| `ErrorBoundary` | class components, `getDerivedStateFromError`, `componentDidCatch`, reset strategies |

```
cd web-katas && npx vitest run src/react/l3
```

## 8. Seen in the shop project

- The frontend level: a Vite SPA served by nginx with hashed assets, an API client with typed errors, an auth context, protected routes, and Testing Library tests that query by role and label.

## 9. Interview talk track

"React re-renders a component when its state, its parent or a context it reads changes; that is usually cheap, so I profile first and then move state down, memoise props for memoised children, virtualise long lists and split code by route. Effects are only for synchronising with external systems, with cleanups and protection against stale responses, and for server data I use a query library. Every data view handles loading, error, empty and success, and error boundaries around routes and widgets keep one failure from blanking the app, with errors reported. I build on semantic HTML for accessibility and test by role and label. Security-wise, React escapes text, so I watch the escape hatches, keep secrets out of the bundle and rely on the API for authorisation. For architecture I organise by feature, keep server, URL and client state apart, and choose SPA, SSR or server components by SEO and performance needs."

**Questions you should be able to answer:**
1. What causes a component to re-render? How do you find unnecessary re-renders?
2. When do `React.memo`, `useMemo` and `useCallback` help, and when are they useless?
3. What does an error boundary catch, and what not?
4. How do you persist state in `localStorage` safely? What breaks with server rendering?
5. Five accessibility rules every component should follow.
6. Where can XSS still happen in a React app?
7. CSR vs SSR vs static generation vs React Server Components.
8. How would you structure a large React codebase?
