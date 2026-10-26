import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useLocalStorageState } from './useLocalStorageState';

beforeEach(() => localStorage.clear());
afterEach(() => vi.restoreAllMocks());

describe('useLocalStorageState', () => {
  it('starts from the stored value', () => {
    localStorage.setItem('theme', JSON.stringify('dark'));
    const { result } = renderHook(() => useLocalStorageState('theme', 'light'));
    expect(result.current[0]).toBe('dark');
  });

  it('falls back to the initial value when nothing or garbage is stored', () => {
    expect(renderHook(() => useLocalStorageState('missing', 7)).result.current[0]).toBe(7);
    localStorage.setItem('broken', '{not json');
    expect(renderHook(() => useLocalStorageState('broken', { items: [] })).result.current[0]).toEqual({ items: [] });
  });

  it('reads storage only once', () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem');
    const { rerender } = renderHook(() => useLocalStorageState('cart', 0));
    rerender();
    rerender();
    expect(getItem).toHaveBeenCalledTimes(1);
  });

  it('writes JSON and supports updater functions', () => {
    const { result } = renderHook(() => useLocalStorageState<string[]>('cart', []));
    act(() => result.current[1](['book']));
    act(() => result.current[1]((prev) => [...prev, 'pen']));
    expect(result.current[0]).toEqual(['book', 'pen']);
    expect(JSON.parse(localStorage.getItem('cart')!)).toEqual(['book', 'pen']);
  });

  it('survives a storage that throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('denied', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('full', 'QuotaExceededError');
    });
    const { result } = renderHook(() => useLocalStorageState('cart', 1));
    expect(result.current[0]).toBe(1);
    act(() => result.current[1](2));
    expect(result.current[0]).toBe(2);
  });

  it('follows changes made in other tabs and stops listening on unmount', () => {
    const { result, unmount } = renderHook(() => useLocalStorageState('theme', 'light'));
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: 'theme', newValue: JSON.stringify('dark') }));
    });
    expect(result.current[0]).toBe('dark');
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: 'other', newValue: '"x"' }));
    });
    expect(result.current[0]).toBe('dark');
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: 'theme', newValue: null }));
    });
    expect(result.current[0]).toBe('light');

    const remove = vi.spyOn(window, 'removeEventListener');
    unmount();
    expect(remove).toHaveBeenCalledWith('storage', expect.any(Function));
  });
});
