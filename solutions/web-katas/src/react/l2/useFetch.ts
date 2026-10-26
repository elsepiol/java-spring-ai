import { useCallback, useEffect, useState } from 'react';

/** Reference solution for the useFetch kata. */
export type FetchState<T> = { status: 'loading' } | { status: 'error'; error: Error } | { status: 'success'; data: T };

export function useFetch<T>(url: string): FetchState<T> & { reload: () => void } {
  const [state, setState] = useState<FetchState<T>>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true; // even if abort is ignored, a stale response never wins
    setState({ status: 'loading' });
    fetch(url, { signal: controller.signal })
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return (await res.json()) as T;
      })
      .then((data) => {
        if (active) setState({ status: 'success', data });
      })
      .catch((error: unknown) => {
        if (!active || (error instanceof DOMException && error.name === 'AbortError')) return;
        setState({ status: 'error', error: error instanceof Error ? error : new Error(String(error)) });
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [url, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  return { ...state, reload };
}
