/** Reference solution for the closures kata. */
export function once<A extends unknown[], R>(fn: (...args: A) => R): (...args: A) => R {
  let called = false;
  let result: R;
  return (...args: A) => {
    if (!called) {
      called = true;
      result = fn(...args);
    }
    return result;
  };
}

export function memoize<A extends unknown[], R>(fn: (...args: A) => R, key: (...args: A) => unknown = (...args) => args[0]): (...args: A) => R {
  const cache = new Map<unknown, R>();
  return (...args: A) => {
    const k = key(...args);
    if (cache.has(k)) return cache.get(k) as R;
    const value = fn(...args);
    cache.set(k, value);
    return value;
  };
}

export function makeButtonHandlers(count: number): Array<() => number> {
  const handlers: Array<() => number> = [];
  // let creates a new binding per iteration; var had ONE binding shared by every closure
  for (let i = 0; i < count; i++) {
    handlers.push(() => i);
  }
  return handlers;
}
