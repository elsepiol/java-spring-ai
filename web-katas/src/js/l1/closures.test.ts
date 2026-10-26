import { describe, expect, it, vi } from 'vitest';
import { makeButtonHandlers, memoize, once } from './closures';

describe('once', () => {
  it('calls the function only the first time', () => {
    const init = vi.fn(() => 42);
    const initOnce = once(init);
    expect(initOnce()).toBe(42);
    expect(initOnce()).toBe(42);
    expect(init).toHaveBeenCalledTimes(1);
  });

  it('remembers an undefined result too', () => {
    const fn = vi.fn((): undefined => undefined);
    const f = once(fn);
    f();
    f();
    expect(fn).toHaveBeenCalledTimes(1);
  });
});

describe('memoize', () => {
  it('caches by the first argument by default', () => {
    const square = vi.fn((n: number) => n * n);
    const fast = memoize(square);
    expect(fast(4)).toBe(16);
    expect(fast(4)).toBe(16);
    expect(fast(5)).toBe(25);
    expect(square).toHaveBeenCalledTimes(2);
  });

  it('supports a key function and caches undefined', () => {
    const lookup = vi.fn((a: string, b: string): string | undefined => (a === 'x' ? undefined : a + b));
    const fast = memoize(lookup, (a, b) => `${a}|${b}`);
    fast('x', '1');
    fast('x', '1');
    fast('a', 'b');
    fast('a', 'c');
    expect(lookup).toHaveBeenCalledTimes(3);
  });

  it('gives each memoized function its own cache', () => {
    const double = memoize((n: number) => n * 2);
    const triple = memoize((n: number) => n * 3);
    expect(double(2)).toBe(4);
    expect(triple(2)).toBe(6);
  });
});

describe('makeButtonHandlers', () => {
  it('gives every handler its own index', () => {
    expect(makeButtonHandlers(3).map((h) => h())).toEqual([0, 1, 2]);
  });
});
