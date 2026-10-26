# React: question bank

Drill with `java quiz/Quiz.java --track react`.

## L1

### REACT-L1-001 · recall
What does "UI is a function of state" mean in React?

<details><summary>Answer</summary>

Components describe what the UI should look like for the current props and state; when state changes, React calls the component again, compares the new element tree with the previous one (reconciliation) and applies only the necessary DOM changes. You never manipulate the DOM by hand for normal updates.

</details>

### REACT-L1-002 · recall
Props vs state?

<details><summary>Answer</summary>

Props are inputs passed by the parent, read-only for the component. State is data the component owns and changes over time with its setter, causing re-renders. Data flows down as props; changes flow up through callback props.

</details>

### REACT-L1-003 · recall
What causes a component to re-render?

<details><summary>Answer</summary>

Its state changes (a setter called with a different value), its parent re-renders (by default all children re-render), or a context it consumes changes value.

</details>

### REACT-L1-004 · bug
```tsx
const add = (item) => { items.push(item); setItems(items); };
```
The list does not update. Why?

<details><summary>Answer</summary>

The array was mutated and the same reference passed to setItems; React compares with Object.is, sees no change and skips the re-render (and mutation breaks other assumptions). Create a new array: `setItems([...items, item])` or `setItems(prev => [...prev, item])`.

</details>

### REACT-L1-005 · predict
`const [n, setN] = useState(0);` A click handler calls `setN(n + 1); setN(n + 1);`. What is n after the click? And with `setN(x => x + 1)` twice?

<details><summary>Answer</summary>

1: both calls use the n from this render (0), so both set 1. With the updater form each call receives the latest pending value: 2.

</details>

### REACT-L1-006 · recall
What is JSX?

<details><summary>Answer</summary>

Syntax that compiles to React element creation calls (`jsx('h1', { children: ... })`): HTML-like tags with JavaScript expressions in braces. Differences from HTML: `className`, `htmlFor`, camelCase events and styles, one root element (or a fragment), and automatic escaping of text.

</details>

### REACT-L1-007 · bug
`{items.length && <List items={items} />}` shows a stray "0" when the list is empty. Why?

<details><summary>Answer</summary>

`&&` returns its left operand when falsy: 0 is a number, which React renders. Use a boolean: `items.length > 0 && ...`, or a ternary.

</details>

### REACT-L1-008 · recall
What is derived state, and why should you avoid storing it?

<details><summary>Answer</summary>

Values computable from existing props or state (a filtered list, a total, "is valid", "at max"). Storing them in separate state duplicates the truth and lets them go out of sync (forgetting to update one). Compute them during render (with useMemo only if expensive).

</details>

### REACT-L1-009 · recall
Controlled vs uncontrolled inputs?

<details><summary>Answer</summary>

Controlled: React state holds the value (`value` + `onChange`), so it is the single source of truth and can be validated or transformed on every keystroke. Uncontrolled: the DOM holds the value (`defaultValue`), read on submit via a ref or FormData; simpler, fewer re-renders, good for simple forms.

</details>

### REACT-L1-010 · recall
What are keys in lists for?

<details><summary>Answer</summary>

They tell React which item is which between renders, so it can keep the right component instance (state, DOM, focus) for each item when items are added, removed or reordered. Keys must be stable and unique among siblings, typically database ids.

</details>

### REACT-L1-011 · bug
A todo list uses `key={index}`. A user checks the third item, then deletes the first; now the second item looks checked. Why?

<details><summary>Answer</summary>

After deleting, items shift: the old third item now has index 1, so React reuses the component (and its uncontrolled checkbox state) that belonged to index 1 for a different item. With stable ids as keys, state follows the right item.

</details>

### REACT-L1-012 · bug
`key={Math.random()}` on list items. What happens?

<details><summary>Answer</summary>

Every render gives every item a new key, so React unmounts and remounts all items each time: state is lost, inputs lose focus, and performance suffers.

</details>

### REACT-L1-013 · recall
Why use a `<form>` with `onSubmit` rather than a button with `onClick`?

<details><summary>Answer</summary>

Enter submits from any input, buttons default to submit, browsers and assistive technology understand it, and you handle all submission paths in one place. Call `e.preventDefault()` to stop the full-page submission.

</details>

### REACT-L1-014 · recall
What does "lifting state up" mean?

<details><summary>Answer</summary>

Moving shared state to the closest common ancestor of the components that need it and passing it down as props (with callbacks to change it), so there is one source of truth instead of copies that diverge.

</details>

### REACT-L1-015 · recall
How does React Testing Library want you to find elements, and why?

<details><summary>Answer</summary>

As users do: by role and accessible name (`getByRole('button', { name: 'Save' })`), by label (`getByLabelText`), by text; test IDs as a last resort. Tests then exercise behaviour rather than implementation details, survive refactoring, and push you towards accessible markup.

</details>

### REACT-L1-016 · recall
`getBy` vs `queryBy` vs `findBy`?

<details><summary>Answer</summary>

`getBy` returns the element or throws if missing (or if several match). `queryBy` returns null when missing (to assert absence). `findBy` returns a promise that waits until the element appears (async UI), rejecting after a timeout.

</details>

### REACT-L1-017 · why
Is `{userComment}` in JSX vulnerable to XSS?

<details><summary>Answer</summary>

No: React escapes text content, so HTML in the string is shown as text. XSS appears through escape hatches: `dangerouslySetInnerHTML`, user-controlled `href`/`src` with `javascript:` URLs, or injecting into scripts and styles.

</details>

### REACT-L1-018 · recall
What are React components allowed to do while rendering?

<details><summary>Answer</summary>

Rendering must be pure: compute JSX from props, state and context, without side effects (no API calls, subscriptions, mutating external variables or DOM access). Side effects go in event handlers or effects. React may render more than once (Strict Mode, concurrent rendering).

</details>

### REACT-L1-019 · interview
"How would you build a product list page with a search box and an 'add to cart' button per product?"

<details><summary>Answer</summary>

A page component holding the search query state (controlled input, debounced), fetching products for the query (a query library or an effect with cancellation) with loading/error/empty states, rendering a list of ProductCard components keyed by product id, each with an onAdd callback that updates cart state lifted to a cart context. Filtering derived during render if done client-side, accessible labels and buttons, and Testing Library tests by role and label.

</details>

### REACT-L1-020 · dsa
Reconciliation compares two trees. Why doesn't React use a general tree-diff algorithm, and what assumptions make its diff O(n)?

<details><summary>Answer</summary>

A general minimal tree edit distance is O(n³). React assumes elements of different types produce different trees (replace the subtree rather than diff it) and that keys identify children across renders, so it compares level by level, matching children by key: linear in the number of elements. Wrong keys break the second assumption.

</details>

## L2

### REACT-L2-001 · recall
What are the rules of hooks, and why?

<details><summary>Answer</summary>

Call hooks only at the top level of function components or custom hooks, never inside conditions, loops, nested functions or after early returns. React identifies each hook's state by call order, so the order must be identical on every render. The eslint-plugin-react-hooks rule enforces it.

</details>

### REACT-L2-002 · recall
When does a `useEffect` run, and what is its cleanup?

<details><summary>Answer</summary>

After React has committed the render to the screen: on mount and whenever a dependency changed (every render without a dependency array, once with `[]`). The function it returns is the cleanup: run before the effect re-runs and on unmount, to undo what the effect set up (unsubscribe, clear timers, abort requests).

</details>

### REACT-L2-003 · recall
Name situations where you should not use `useEffect`.

<details><summary>Answer</summary>

Deriving data from props/state (compute during render), reacting to user events (do it in the handler: submit, add to cart), resetting state when a prop changes (use a key), sending an analytics event for a click (handler), transforming data for rendering (useMemo if expensive), and chaining state updates. Effects are for synchronising with external systems.

</details>

### REACT-L2-004 · bug
```tsx
useEffect(() => {
  fetch(`/api/products?q=${query}`).then(r => r.json()).then(setResults);
}, [query]);
```
Name two bugs.

<details><summary>Answer</summary>

Race condition: a slow response for an older query can arrive after the newer one and overwrite it; there is no cancellation (abort and/or an `active` flag in the cleanup). HTTP errors are not handled (`r.ok` unchecked; no error state), and setting state after unmount does wasted work.

</details>

### REACT-L2-005 · bug
```tsx
useEffect(() => {
  const id = setInterval(() => setSeconds(seconds + 1), 1000);
  return () => clearInterval(id);
}, []);
```
The counter stops at 1. Why, and the fix?

<details><summary>Answer</summary>

The effect closes over `seconds` from the first render (a stale closure), so it always sets 0 + 1. Use the updater form `setSeconds(s => s + 1)` (or add seconds to the dependencies, which recreates the interval every second).

</details>

### REACT-L2-006 · recall
Why does React Strict Mode run effects twice in development?

<details><summary>Answer</summary>

It mounts, unmounts and remounts components to reveal effects without proper cleanup (double subscriptions, leaked timers, duplicate requests) that would break with features like fast refresh, offscreen rendering and remounting. Production runs them once.

</details>

### REACT-L2-007 · bug
`useEffect(() => { load(options); }, [options])` re-runs on every render although options never really change. Why?

<details><summary>Answer</summary>

`options` is an object created during each render (`const options = { page: 1 }` or an inline prop), so it has a new identity every time and the dependency always "changes". Depend on primitive fields (`options.page`), create it with useMemo, or move it inside the effect.

</details>

### REACT-L2-008 · recall
What is a custom hook?

<details><summary>Answer</summary>

A function whose name starts with `use` that calls other hooks to package reusable stateful logic (useFetch, useDebouncedValue, useLocalStorageState). Each component calling it gets its own independent state. Test with `renderHook`.

</details>

### REACT-L2-009 · recall
`useRef` vs `useState`?

<details><summary>Answer</summary>

Both persist across renders. Changing state triggers a re-render; changing `ref.current` does not. Refs hold DOM elements, timer ids, the latest value for callbacks, or anything that should not drive rendering. Do not read or write refs during rendering for values that affect output.

</details>

### REACT-L2-010 · recall
When is `useReducer` better than `useState`?

<details><summary>Answer</summary>

When state has several related fields updated together or transitions follow rules (a cart, a form wizard, a fetch state machine): actions describe what happened, a pure reducer computes the next state, logic is testable without rendering and the dispatch function is stable.

</details>

### REACT-L2-011 · recall
What is React context for, and what is its performance caveat?

<details><summary>Answer</summary>

Passing values (current user, theme, locale, API client) to deep descendants without prop drilling. Every consumer re-renders when the provider's value changes, so a frequently changing value (or a new object created on each render) re-renders large parts of the app. Memoise the value, split contexts, or use a store with selectors.

</details>

### REACT-L2-012 · design
Server state vs client state: where should each live?

<details><summary>Answer</summary>

Server state (data owned by the backend: products, orders) belongs in a query cache such as TanStack Query, which handles fetching, caching, deduplication, invalidation and background refresh. Client state (UI-only: open dialogs, form drafts, a selected tab) lives locally in components, in the URL when shareable, or in a small store when global. Mixing them into one global store means re-implementing caching badly.

</details>

### REACT-L2-013 · recall
What does TanStack Query (React Query) give you over fetching in effects?

<details><summary>Answer</summary>

Caching by query key, request deduplication, background refetching and stale-while-revalidate, retries, pagination and infinite queries, mutations with invalidation and optimistic updates, cancellation and race-free results, plus devtools. Most of it is hard to get right by hand.

</details>

### REACT-L2-014 · recall
How do you make form validation errors accessible?

<details><summary>Answer</summary>

Show the message next to its field in text (not only colour), link it with `aria-describedby`, set `aria-invalid="true"` on the field, use real labels, and on submit move focus to the first invalid field or announce an error summary (`role="alert"`).

</details>

### REACT-L2-015 · scenario
Users sometimes create two orders by double-clicking "Place order". Frontend and backend fixes?

<details><summary>Answer</summary>

Frontend: disable the button and show a pending state while the request is in flight, prevent re-submission in the handler. Backend (the real guarantee): an idempotency key per checkout attempt so duplicates return the first result (Web track L2).

</details>

### REACT-L2-016 · recall
Which state belongs in the URL?

<details><summary>Answer</summary>

Anything a user would want to share, bookmark or get back to with the back button: search queries, filters, sorting, pagination, the selected tab or item id. The router's search params become the source of truth.

</details>

### REACT-L2-017 · recall
What does a client-side protected route do, and what doesn't it do?

<details><summary>Answer</summary>

It redirects unauthenticated users to the login page (remembering where they wanted to go) and hides UI they cannot use: a UX convenience. It does not protect data: the JavaScript and API are reachable by anyone, so the backend must authenticate and authorise every request.

</details>

### REACT-L2-018 · recall
How do you reset a component's state when a prop changes, without an effect?

<details><summary>Answer</summary>

Give it a key derived from that prop (`<ProfileForm key={userId} user={user} />`): a different key makes React unmount the old instance and mount a fresh one with initial state.

</details>

### REACT-L2-019 · interview
"Walk me through how you would fetch and display paginated orders in React."

<details><summary>Answer</summary>

Page number and filters in the URL; a query (TanStack Query with key ['orders', page, filters]) calling the API with an AbortSignal; render loading (skeleton), error (message + retry), empty and success states; keep previous data while the next page loads (placeholderData) to avoid flicker; prefetch the next page; keyed rows; accessible pagination controls; invalidate the query after mutations. Without a library: an effect with cleanup and stale-response protection.

</details>

### REACT-L2-020 · dsa
A useFetch hook ignores stale responses with an `active` flag per effect run. Why is a per-run flag enough, rather than comparing URLs or timestamps?

<details><summary>Answer</summary>

Each effect run creates its own closure with its own flag, and its cleanup (run before the next run or on unmount) sets that flag to false. Only the latest run's flag is still true, so exactly the latest request can write state, whatever order responses arrive in: a version check where the closure is the version.

</details>

## L3

### REACT-L3-001 · recall
What does `React.memo` do, and when does it help?

<details><summary>Answer</summary>

It wraps a component so React skips re-rendering it when its props are shallowly equal to the previous ones. It helps for components that are expensive to render and re-rendered often by parents with unchanged props; it is useless if a prop gets a new identity every render (inline objects, arrays, functions).

</details>

### REACT-L3-002 · recall
`useMemo` vs `useCallback`?

<details><summary>Answer</summary>

`useMemo(() => compute(a, b), [a, b])` caches a computed value; `useCallback(fn, deps)` caches a function identity (it is `useMemo(() => fn, deps)`). Use them for expensive calculations and for props/dependencies whose identity matters (memoised children, effect dependencies), not everywhere.

</details>

### REACT-L3-003 · recall
How do you find unnecessary re-renders?

<details><summary>Answer</summary>

React DevTools Profiler (record an interaction, see which components rendered, why, and how long each took) and "Highlight updates when components render"; the browser Performance panel for long tasks. Measure before and after any optimisation.

</details>

### REACT-L3-004 · scenario
Typing in a search box above a 2,000-row table is laggy. What can you do?

<details><summary>Answer</summary>

Keep the input's state in a small component so typing does not re-render the table; useDeferredValue/useTransition for the filtered result so typing stays responsive; memoise the filtering and the rows (React.memo with stable props); virtualise the table; or filter on the server with debouncing.

</details>

### REACT-L3-005 · recall
What is the React Compiler?

<details><summary>Answer</summary>

A build-time compiler that automatically memoises components and values (the effect of useMemo/useCallback/React.memo) based on analysis of the code, assuming the rules of React are followed (pure rendering, immutable props and state). It reduces manual memoisation.

</details>

### REACT-L3-006 · recall
What does an error boundary catch, and what doesn't it?

<details><summary>Answer</summary>

Errors thrown while rendering, in lifecycle methods and in constructors of components below it; it renders a fallback instead of unmounting the whole app. It does not catch errors in event handlers, asynchronous code (promises, timers), server-side rendering, or in the boundary itself.

</details>

### REACT-L3-007 · recall
Why are error boundaries still class components?

<details><summary>Answer</summary>

They rely on `static getDerivedStateFromError` (render a fallback) and `componentDidCatch` (side effects such as reporting), which have no hook equivalents yet. Libraries such as react-error-boundary wrap this in a reusable component with reset helpers.

</details>

### REACT-L3-008 · design
Where would you place error boundaries in an application?

<details><summary>Answer</summary>

Around each route (one broken page does not take down navigation), around independent widgets on dashboards (one failing chart shows its fallback, the rest works), and one at the top as a last resort. Reset on route change (reset keys) and report every error with context.

</details>

### REACT-L3-009 · recall
What are the four states every data-driven view needs?

<details><summary>Answer</summary>

Loading (skeletons preferred), error (a useful message and a retry), empty (explain and suggest an action), and success. Also consider refreshing (keep showing old data) and partial failure.

</details>

### REACT-L3-010 · bug
`const [cart, setCart] = useState(JSON.parse(localStorage.getItem('cart')));` Problems?

<details><summary>Answer</summary>

It reads and parses localStorage on every render (the argument is evaluated each time, though only used once): use a lazy initializer `useState(() => ...)`. It crashes on corrupt JSON, returns null when missing, throws where storage is unavailable (private mode, blocked cookies) and breaks server rendering (no window). Parse defensively with a fallback.

</details>

### REACT-L3-011 · recall
What is the `storage` event?

<details><summary>Answer</summary>

A window event fired in other tabs/windows of the same origin when localStorage changes (not in the tab that made the change). Listening to it keeps state such as the cart or login status in sync across tabs; remove the listener on unmount.

</details>

### REACT-L3-012 · recall
What is hydration, and what causes hydration errors?

<details><summary>Answer</summary>

With server rendering, the browser receives HTML first, then React attaches event handlers and state to the existing markup (hydration). If the client's first render produces different markup than the server (reading window or localStorage, Date.now(), random values, locale differences), React reports a mismatch and may re-render. Read browser-only values in effects.

</details>

### REACT-L3-013 · recall
Five accessibility rules for React components.

<details><summary>Answer</summary>

Use semantic elements (button, a, label, headings, lists) instead of divs with click handlers; make everything keyboard-operable with visible focus; label every control and give icon buttons an accessible name; announce dynamic changes (role="status"/"alert", aria-live) and manage focus in dialogs; ensure sufficient colour contrast and never rely on colour alone.

</details>

### REACT-L3-014 · bug
`<div onClick={openMenu} className="button">Menu</div>`. What is wrong?

<details><summary>Answer</summary>

It is not focusable or operable with the keyboard, is not announced as a button by screen readers and has no disabled semantics. Use `<button type="button" onClick={openMenu}>` (plus `aria-expanded` for a menu toggle).

</details>

### REACT-L3-015 · recall
Where can XSS still occur in a React app?

<details><summary>Answer</summary>

`dangerouslySetInnerHTML` with unsanitised content, user-controlled URLs in `href`/`src` (`javascript:`), rendering user input into `<script>` or inline styles, server-rendered HTML built by string concatenation, third-party scripts and compromised dependencies, and DOM APIs used directly in refs/effects (`innerHTML`).

</details>

### REACT-L3-016 · why
Why must you never put secrets in frontend code or `VITE_` environment variables?

<details><summary>Answer</summary>

Everything bundled is shipped to every browser and readable in the source and network tab; build-time env variables are inlined as plain text. Only publishable keys (scoped, rate-limited, origin-restricted) belong there; secrets stay on the server or a backend-for-frontend.

</details>

### REACT-L3-017 · recall
CSR vs SSR vs SSG vs React Server Components?

<details><summary>Answer</summary>

CSR: the browser downloads JavaScript and renders everything (simple hosting, slower first paint, weaker SEO). SSR: the server renders HTML per request, then the client hydrates (fast first paint, server cost). SSG: HTML built ahead of time (fastest, for content that rarely changes). RSC: components that run only on the server, fetch data directly and ship no JavaScript for themselves, mixed with interactive client components (`'use client'`).

</details>

### REACT-L3-018 · design
How would you structure a large React codebase?

<details><summary>Answer</summary>

By feature (orders/, cart/, auth/) with each feature's components, hooks, API calls and tests together; a shared design-system component library; one API client with generated types; clear rules for state placement (query cache, URL, local, minimal global store); route-level code splitting; lint, type-check and test in CI; and documented conventions.

</details>

### REACT-L3-019 · interview
"Our React app feels slow. How do you approach it?"

<details><summary>Answer</summary>

Measure first: Core Web Vitals from real users, Lighthouse, the Performance panel and the React Profiler. Then target the biggest causes: large bundles (code splitting, removing heavy dependencies), slow or waterfall data loading (parallel requests, prefetching, caching, SSR), long main-thread tasks (virtualisation, memoisation, moving state down, transitions, workers), and layout shifts (reserved space, skeletons). Verify each change with the same measurements.

</details>

### REACT-L3-020 · dsa
`React.memo` does a shallow comparison of props. What is its cost compared to a deep comparison, and why does it work well with immutable updates?

<details><summary>Answer</summary>

Shallow comparison checks each prop with Object.is: O(number of props), independent of data size, while deep comparison is O(size of the data). With immutable updates, a changed piece of data always gets a new reference and unchanged parts keep theirs, so reference equality is a cheap and reliable "did it change" test: the same structural sharing idea as persistent data structures.

</details>
