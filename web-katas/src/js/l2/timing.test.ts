import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { debounce, throttle } from './timing';

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('debounce', () => {
  it('calls once with the last arguments after calls stop', () => {
    const search = vi.fn();
    const onType = debounce(search, 300);
    onType('j');
    vi.advanceTimersByTime(100);
    onType('ja');
    vi.advanceTimersByTime(100);
    onType('jav');
    vi.advanceTimersByTime(299);
    expect(search).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith('jav');
  });

  it('can be cancelled', () => {
    const save = vi.fn();
    const autosave = debounce(save, 1000);
    autosave('draft');
    autosave.cancel();
    vi.advanceTimersByTime(5000);
    expect(save).not.toHaveBeenCalled();
    autosave('again');
    vi.advanceTimersByTime(1000);
    expect(save).toHaveBeenCalledWith('again');
  });
});

describe('throttle', () => {
  it('runs immediately, then at most once per window with the latest arguments', () => {
    const onScroll = vi.fn();
    const throttled = throttle(onScroll, 100);
    throttled(1);
    expect(onScroll).toHaveBeenLastCalledWith(1);
    throttled(2);
    throttled(3);
    expect(onScroll).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(100);
    expect(onScroll).toHaveBeenCalledTimes(2);
    expect(onScroll).toHaveBeenLastCalledWith(3);
  });

  it('does not make a trailing call when nothing happened in the window', () => {
    const fn = vi.fn();
    const throttled = throttle(fn, 100);
    throttled('a');
    vi.advanceTimersByTime(500);
    expect(fn).toHaveBeenCalledTimes(1);
    throttled('b');
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('keeps the rate under continuous calls', () => {
    const fn = vi.fn();
    const throttled = throttle(fn, 100);
    for (let t = 0; t < 1000; t += 10) {
      throttled(t);
      vi.advanceTimersByTime(10);
    }
    vi.advanceTimersByTime(200);
    expect(fn.mock.calls.length).toBeGreaterThanOrEqual(10);
    expect(fn.mock.calls.length).toBeLessThanOrEqual(11);
  });
});
