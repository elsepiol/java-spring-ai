import { todo } from '../../kata';

/**
 * KATA react L1 "TodoList" (tracks/13-react/L1-basics.md, sections 4-5).
 *
 * - a form with a text input labelled "New todo" and a submit button "Add": submitting adds the
 *   trimmed text as a new open item at the end and clears the input; blank text adds nothing;
 *   pressing Enter in the input submits too (it is a real <form>);
 * - a list (<ul>) with one <li> per item, containing a checkbox labelled with the item's text
 *   (checking it marks the item done) and a button "Delete <text>" removing it;
 * - a line "<n> open" counting items not done (derive it, do not store it);
 * - when there are no items, show "Nothing to do" instead of the list;
 * - each item gets a stable id when added (use it as the key, never the array index: deleting
 *   an item must not move checkbox state to its neighbour).
 * Never mutate state: create new arrays/objects (map, filter, spread).
 */
export function TodoList() {
  return todo('tracks/13-react/L1-basics.md sections 4-5');
}
