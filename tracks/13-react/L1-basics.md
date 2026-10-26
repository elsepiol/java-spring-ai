# React, L1: components, JSX, props, state, events, lists

**Goal:** build interactive UIs from components: understand what React does when state changes, write components with props and state, handle events and forms, render lists correctly, and test components the way users use them.

Prerequisite: JS/TS L1. Katas: `cd web-katas && npx vitest run src/react/l1`. Drill: `java quiz/Quiz.java --track react --level 1`.

---

## 1. The idea

**UI = f(state).** You describe what the screen should look like for the current data; React works out how to change the DOM. Components are functions that take **props** and return **JSX**; when a component's **state** (or its parent's props) changes, React calls the function again (**re-renders**), compares the new element tree with the previous one (**reconciliation**), and applies the minimal DOM changes.

```tsx
export function ProductCard({ name, priceCents, onAdd }: { name: string; priceCents: number; onAdd: () => void }) {
  return (
    <article className="card">
      <h2>{name}</h2>
      <p>{formatCents(priceCents)}</p>
      <button onClick={onAdd}>Add to cart</button>
    </article>
  );
}
```

React 19 with a modern toolchain: Vite for development, function components and hooks only (class components remain for error boundaries, L3), and a framework (Next.js, React Router) when you need routing, server rendering and data loading.

---

## 2. JSX

JSX is syntax for `React.createElement` calls: HTML-like, but JavaScript:
- `{expression}` embeds values; `className` instead of `class`, `htmlFor` instead of `for`, camelCase events (`onClick`) and styles (`style={{ marginTop: 8 }}`).
- Conditional rendering: `{error && <p role="alert">{error}</p>}`, `{items.length === 0 ? <Empty /> : <List items={items} />}`. Careful: `{count && ...}` renders `0` when count is 0.
- One root element per component (or a fragment `<>...</>`).
- Text is **escaped automatically**: `{userInput}` cannot inject HTML. `dangerouslySetInnerHTML` bypasses that (Security track L3).

---

## 3. Props and state

- **Props** are inputs from the parent: read-only. Data flows **down**; events flow **up** through callback props (`onAdd`).
- **State** (`useState`) is data owned by a component that changes over time:

```tsx
const [quantity, setQuantity] = useState(1);
<button onClick={() => setQuantity((q) => q + 1)}>+</button>      // updater form: based on the latest value
```

- Setting state **schedules** a re-render; the variable keeps its old value until then (`setQ(q + 1); setQ(q + 1)` adds 1, the updater form adds 2).
- **Never mutate state**: `items.push(x); setItems(items)` changes nothing visible (same reference, React skips the render). Create new values: `setItems([...items, x])`, `items.map(...)`, `items.filter(...)`, `{ ...item, done: true }`.
- **Derive, don't duplicate**: if a value can be computed from props or state (`openCount = items.filter(i => !i.done).length`, "is at max"), compute it during render instead of storing it in more state that can go out of sync.
- **Lift state up** to the closest common parent when two components need the same data; pass it down as props.

> **Kata `Counter`**: state, limits as disabled buttons, derived values, change callbacks.

---

## 4. Events and forms

```tsx
const [email, setEmail] = useState('');
<form onSubmit={(e) => { e.preventDefault(); save(email); }}>
  <label htmlFor="email">Email</label>
  <input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />   // controlled input
  <button type="submit">Save</button>
</form>
```

- **Controlled inputs**: React state is the single source of truth (`value` + `onChange`). Uncontrolled inputs (`defaultValue`, read via a ref or `FormData` on submit) are fine for simple forms.
- Use a real `<form>` with `onSubmit`: Enter submits, buttons inside default to `type="submit"`, and accessibility tools understand it. `e.preventDefault()` stops the browser's full-page submission.
- Label every input (`<label>`): screen readers and tests (`getByLabelText`) need it.

---

## 5. Lists and keys

```tsx
<ul>{items.map((item) => <li key={item.id}>{item.text}</li>)}</ul>
```

React matches list items between renders by **key**. Keys must be **stable and unique** among siblings: database ids, or an id generated when the item is created. **Not the array index** (when an item is removed or inserted, indexes shift, and React reuses the wrong component instance, with its state, focus and checkbox values, for the wrong item) and never `Math.random()` (a new key each render remounts everything).

> **Kata `TodoList`**: a form, immutable updates, derived counts, empty states, stable keys.

---

## 6. Testing components

**React Testing Library** renders components into a DOM (jsdom) and queries them **the way users find things**: by role and accessible name (`getByRole('button', { name: 'Add' })`), by label (`getByLabelText('Email')`), by text. `user-event` simulates real interactions (typing, clicking, keyboard).

```tsx
render(<Counter initial={2} />);
await user.click(screen.getByRole('button', { name: 'Increase' }));
expect(screen.getByRole('status')).toHaveTextContent('3');
```

- Test behaviour, not implementation: no reaching into state or component internals, so refactoring keeps tests green.
- `getBy` throws if absent, `queryBy` returns null (for asserting absence), `findBy` waits (async UI).
- If an element is hard to query by role or label, it is probably hard to use with a screen reader too: improve the markup.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `Counter` | `useState`, event handlers, disabled states, derived values, callbacks |
| `TodoList` | forms, controlled inputs, immutable array updates, keys, conditional rendering |

```
cd web-katas && npx vitest run src/react/l1
```

## 8. Seen in the shop project

- `frontend/src/pages`: function components with props and state (`ProductsPage`, `CartPage`, `LoginPage`), lists keyed by id, labelled forms, and Testing Library tests next to them (`ProductsPage.test.tsx`, `CartPage.test.tsx`).

## 9. Interview checklist

1. What does "UI is a function of state" mean? What triggers a re-render?
2. Props vs state.
3. Why must you not mutate state? How do you update an array or object in state?
4. Why does `setCount(count + 1)` twice only add 1? What is the updater form?
5. What is derived state, and why avoid storing it?
6. Controlled vs uncontrolled inputs.
7. What are keys for? Why not use the index?
8. How does React Testing Library want you to query elements, and why?
