import { todo } from '../../kata';

/**
 * KATA js L1 "closures" (tracks/12-js-ts/L1-basics.md, section 3).
 *
 * A closure is a function plus the variables it captured from where it was created.
 *
 * once(fn): calls fn the first time and returns its result; later calls return that same
 * result without calling fn again (even if the first result was undefined).
 *
 * memoize(fn, key?): caches results per key (default key: the first argument). A cached
 * undefined is still a cache hit. Each memoized function has its own cache.
 *
 * makeButtonsHandlers is BUGGY (find the bug): every handler should return its own index,
 * but they all return 3. Fix it with the smallest change.
 */
export function once<A extends unknown[], R>(fn: (...args: A) => R): (...args: A) => R {
  return todo('tracks/12-js-ts/L1-basics.md section 3');
}

export function memoize<A extends unknown[], R>(fn: (...args: A) => R, key: (...args: A) => unknown = (...args) => args[0]): (...args: A) => R {
  return todo('tracks/12-js-ts/L1-basics.md section 3');
}

export function makeButtonHandlers(count: number): Array<() => number> {
  const handlers: Array<() => number> = [];
  // eslint-disable-next-line no-var
  for (var i = 0; i < count; i++) {
    handlers.push(() => i);
  }
  return handlers;
}
