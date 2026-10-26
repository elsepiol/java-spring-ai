import { useState, type FormEvent } from 'react';

/** Reference solution for the TodoList kata. */
interface Item {
  id: number;
  text: string;
  done: boolean;
}

let nextId = 1;

export function TodoList() {
  const [items, setItems] = useState<Item[]>([]);
  const [draft, setDraft] = useState('');

  const add = (e: FormEvent) => {
    e.preventDefault();
    const text = draft.trim();
    if (!text) return;
    setItems((current) => [...current, { id: nextId++, text, done: false }]);
    setDraft('');
  };

  const toggle = (id: number) => setItems((current) => current.map((i) => (i.id === id ? { ...i, done: !i.done } : i)));
  const remove = (id: number) => setItems((current) => current.filter((i) => i.id !== id));
  const open = items.filter((i) => !i.done).length;

  return (
    <section>
      <form onSubmit={add}>
        <label>
          New todo <input value={draft} onChange={(e) => setDraft(e.target.value)} />
        </label>
        <button type="submit">Add</button>
      </form>
      {items.length === 0 ? (
        <p>Nothing to do</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item.id}>
              <label>
                <input type="checkbox" checked={item.done} onChange={() => toggle(item.id)} /> {item.text}
              </label>
              <button type="button" onClick={() => remove(item.id)}>
                Delete {item.text}
              </button>
            </li>
          ))}
        </ul>
      )}
      <p>{open} open</p>
    </section>
  );
}
