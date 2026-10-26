import { todo } from '../../kata';

/**
 * KATA react L3 "useLocalStorageState" (tracks/13-react/L3-production.md, section 2).
 *
 * Like useState, but persisted in localStorage as JSON under `key`, e.g. a cart or a theme:
 * - the initial value comes from localStorage (read it ONCE, lazily: not on every render);
 *   missing, corrupt JSON, or a storage that throws (private mode, quota, blocked cookies) ->
 *   `initial`, never a crash;
 * - setting the value updates the state and writes JSON; a failing write keeps the new state
 *   in memory (the app still works, it just does not persist);
 * - the setter accepts a value or an updater function (prev => next), like useState's;
 * - other TABS changing the same key fire a "storage" event on window: apply the new value
 *   (event.newValue null means it was removed -> `initial`); remove the listener on unmount.
 */
export function useLocalStorageState<T>(key: string, initial: T): [T, (next: T | ((prev: T) => T)) => void] {
  return todo('tracks/13-react/L3-production.md section 2');
}
