import { afterEach, describe, expect, it, vi } from 'vitest';
import { all, mapLimit, TimeoutError, withTimeout } from './promises';

const delay = <T>(ms: number, value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), ms));
const fail = (ms: number, reason: string) => new Promise<never>((_, reject) => setTimeout(() => reject(new Error(reason)), ms));

afterEach(() => vi.useRealTimers());

describe('all', () => {
  it('keeps input order, not completion order', async () => {
    await expect(all([delay(30, 'slow'), delay(5, 'fast'), Promise.resolve('now')])).resolves.toEqual(['slow', 'fast', 'now']);
  });

  it('rejects with the first rejection', async () => {
    await expect(all([delay(50, 1), fail(10, 'boom'), fail(30, 'later')])).rejects.toThrow('boom');
  });

  it('resolves an empty list', async () => {
    await expect(all([])).resolves.toEqual([]);
  });

  it('does not use the built-in', async () => {
    const spy = vi.spyOn(Promise, 'all');
    await all([Promise.resolve(1)]);
    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
  });
});

describe('withTimeout', () => {
  it('passes results through when fast enough', async () => {
    await expect(withTimeout(delay(5, 'ok'), 100)).resolves.toBe('ok');
    await expect(withTimeout(fail(5, 'nope'), 100)).rejects.toThrow('nope');
  });

  it('rejects with a TimeoutError when too slow', async () => {
    const result = withTimeout(delay(200, 'late'), 20);
    await expect(result).rejects.toBeInstanceOf(TimeoutError);
    await expect(result).rejects.toThrow('timed out after 20 ms');
  });

  it('leaves no timer behind', async () => {
    vi.useFakeTimers();
    const p = withTimeout(Promise.resolve('ok'), 10_000);
    await vi.advanceTimersByTimeAsync(0);
    await expect(p).resolves.toBe('ok');
    expect(vi.getTimerCount()).toBe(0);
  });
});

describe('mapLimit', () => {
  it('never runs more than limit calls at once and keeps order', async () => {
    let running = 0;
    let maxRunning = 0;
    const results = await mapLimit([30, 10, 20, 5, 15, 25], 2, async (ms) => {
      running += 1;
      maxRunning = Math.max(maxRunning, running);
      await delay(ms, null);
      running -= 1;
      return ms * 2;
    });
    expect(results).toEqual([60, 20, 40, 10, 30, 50]);
    expect(maxRunning).toBe(2);
  });

  it('uses the whole limit', async () => {
    let maxRunning = 0;
    let running = 0;
    await mapLimit([1, 2, 3, 4, 5, 6, 7, 8], 4, async () => {
      running += 1;
      maxRunning = Math.max(maxRunning, running);
      await delay(10, null);
      running -= 1;
    });
    expect(maxRunning).toBe(4);
  });

  it('stops starting new work after a failure', async () => {
    const started: number[] = [];
    await expect(
      mapLimit([1, 2, 3, 4, 5], 1, async (n) => {
        started.push(n);
        if (n === 2) throw new Error('item 2 failed');
        return n;
      }),
    ).rejects.toThrow('item 2 failed');
    await delay(20, null);
    expect(started).toEqual([1, 2]);
  });

  it('handles an empty list', async () => {
    await expect(mapLimit([], 3, async (x) => x)).resolves.toEqual([]);
  });
});
