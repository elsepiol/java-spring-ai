import { describe, expect, it } from 'vitest';
import { deepEqual } from './deepEqual';

describe('deepEqual', () => {
  it('compares primitives', () => {
    expect(deepEqual(1, 1)).toBe(true);
    expect(deepEqual('a', 'a')).toBe(true);
    expect(deepEqual(1, '1')).toBe(false);
    expect(deepEqual(NaN, NaN)).toBe(true);
    expect(deepEqual(0, -0)).toBe(true);
    expect(deepEqual(undefined, null)).toBe(false);
    expect(deepEqual(undefined, undefined)).toBe(true);
  });

  it('compares objects by structure, not reference', () => {
    expect(deepEqual({ a: 1, b: { c: [1, 2] } }, { b: { c: [1, 2] }, a: 1 })).toBe(true);
    expect(deepEqual({ a: 1 }, { a: 1, b: undefined })).toBe(false);
    expect(deepEqual({ a: 1 }, { a: 2 })).toBe(false);
    expect(deepEqual(null, {})).toBe(false);
    expect(deepEqual({}, null)).toBe(false);
  });

  it('compares arrays strictly', () => {
    expect(deepEqual([1, [2, 3]], [1, [2, 3]])).toBe(true);
    expect(deepEqual([1, 2], [1, 2, 3])).toBe(false);
    expect(deepEqual([1, 2], { 0: 1, 1: 2 })).toBe(false);
    expect(deepEqual([], {})).toBe(false);
  });

  it('compares dates by value and functions by reference', () => {
    expect(deepEqual(new Date(5), new Date(5))).toBe(true);
    expect(deepEqual(new Date(5), new Date(6))).toBe(false);
    expect(deepEqual(new Date(5), {})).toBe(false);
    const f = () => 1;
    expect(deepEqual(f, f)).toBe(true);
    expect(deepEqual(f, () => 1)).toBe(false);
  });

  it('survives cycles', () => {
    const a: Record<string, unknown> = { name: 'a' };
    a.self = a;
    const b: Record<string, unknown> = { name: 'a' };
    b.self = b;
    expect(deepEqual(a, b)).toBe(true);
    const c: Record<string, unknown> = { name: 'c' };
    c.self = c;
    expect(deepEqual(a, c)).toBe(false);
  });
});
