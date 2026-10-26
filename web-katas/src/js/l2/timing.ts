import { todo } from '../../kata';

/**
 * KATA js L2 "timing" (tracks/12-js-ts/L2-practical.md, section 3).
 *
 * debounce(fn, ms): wait until calls STOP for ms, then call fn once with the LAST arguments
 * (search-as-you-type: one request after the user pauses). The returned function has
 * cancel(), which drops a pending call.
 *
 * throttle(fn, ms): call fn at most once per ms: the first call runs immediately; calls during
 * the next ms are collapsed into ONE trailing call with the latest arguments, made when the
 * window ends (scroll and resize handlers).
 */
export interface Cancellable<A extends unknown[]> {
  (...args: A): void;
  cancel(): void;
}

export function debounce<A extends unknown[]>(fn: (...args: A) => void, ms: number): Cancellable<A> {
  return todo('tracks/12-js-ts/L2-practical.md section 3');
}

export function throttle<A extends unknown[]>(fn: (...args: A) => void, ms: number): (...args: A) => void {
  return todo('tracks/12-js-ts/L2-practical.md section 3');
}
