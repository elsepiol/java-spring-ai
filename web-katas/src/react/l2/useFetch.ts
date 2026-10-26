import { todo } from '../../kata';

/**
 * KATA react L2 "useFetch" (tracks/13-react/L2-practical.md, section 2).
 *
 * A data-fetching hook built on useEffect, with the three bugs every hand-written one has fixed:
 * - state is one discriminated union: { status: 'loading' } | { status: 'error', error } |
 *   { status: 'success', data }; it starts as loading and goes back to loading when url changes;
 * - non-2xx responses are errors: new Error(`HTTP ${status}`) (fetch does not reject on them);
 * - RACES: when url changes (or the component unmounts) before the response arrives, the old
 *   request is aborted (pass an AbortController's signal to fetch) and its result, even if it
 *   arrives anyway, must never overwrite the state of the newer url;
 * - an aborted request is not an error to show;
 * - reload() fetches the same url again.
 * Use the global fetch (tests replace it).
 */
export type FetchState<T> = { status: 'loading' } | { status: 'error'; error: Error } | { status: 'success'; data: T };

export function useFetch<T>(url: string): FetchState<T> & { reload: () => void } {
  return todo('tracks/13-react/L2-practical.md section 2');
}
