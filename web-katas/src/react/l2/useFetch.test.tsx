import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useFetch } from './useFetch';

type Pending = { url: string; signal: AbortSignal; resolve: (r: Response) => void; reject: (e: unknown) => void };

/** A fetch we answer by hand, so tests control the order in which responses arrive. */
function controlledFetch() {
  const calls: Pending[] = [];
  const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    return new Promise<Response>((resolve, reject) => {
      calls.push({ url, signal: init?.signal as AbortSignal, resolve, reject });
    });
  });
  vi.stubGlobal('fetch', fetchMock);
  return { calls, fetchMock };
}

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

afterEach(() => vi.unstubAllGlobals());

describe('useFetch', () => {
  it('goes from loading to success', async () => {
    const { calls } = controlledFetch();
    const { result } = renderHook(() => useFetch<{ name: string }>('/api/me'));
    expect(result.current.status).toBe('loading');
    await act(async () => calls[0].resolve(json({ name: 'ada' })));
    await waitFor(() => expect(result.current.status).toBe('success'));
    expect(result.current).toMatchObject({ status: 'success', data: { name: 'ada' } });
  });

  it('treats HTTP errors as errors', async () => {
    const { calls } = controlledFetch();
    const { result } = renderHook(() => useFetch('/api/missing'));
    await act(async () => calls[0].resolve(json({ title: 'Not Found' }, 404)));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current).toMatchObject({ status: 'error', error: new Error('HTTP 404') });
  });

  it('aborts the old request and ignores its late answer when the url changes', async () => {
    const { calls } = controlledFetch();
    const { result, rerender } = renderHook(({ url }) => useFetch<string>(url), { initialProps: { url: '/api/search?q=ja' } });
    rerender({ url: '/api/search?q=java' });
    expect(calls).toHaveLength(2);
    expect(calls[0].signal.aborted).toBe(true);
    await act(async () => calls[1].resolve(json('results for java')));
    await waitFor(() => expect(result.current.status).toBe('success'));
    await act(async () => calls[0].resolve(json('results for ja'))); // the stale one arrives last
    expect(result.current).toMatchObject({ data: 'results for java' });
  });

  it('does not report aborts as errors', async () => {
    const { calls } = controlledFetch();
    const { result, rerender } = renderHook(({ url }) => useFetch(url), { initialProps: { url: '/a' } });
    rerender({ url: '/b' });
    await act(async () => calls[0].reject(new DOMException('aborted', 'AbortError')));
    expect(result.current.status).toBe('loading');
  });

  it('aborts on unmount', () => {
    const { calls } = controlledFetch();
    const { unmount } = renderHook(() => useFetch('/api/slow'));
    unmount();
    expect(calls[0].signal.aborted).toBe(true);
  });

  it('reloads on demand', async () => {
    const { calls } = controlledFetch();
    const { result } = renderHook(() => useFetch<number>('/api/count'));
    await act(async () => calls[0].resolve(json(1)));
    await waitFor(() => expect(result.current.status).toBe('success'));
    act(() => result.current.reload());
    expect(result.current.status).toBe('loading');
    await act(async () => calls[1].resolve(json(2)));
    await waitFor(() => expect(result.current).toMatchObject({ data: 2 }));
  });
});
