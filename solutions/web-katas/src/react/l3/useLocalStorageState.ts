import { useCallback, useEffect, useState } from 'react';

/** Reference solution for the useLocalStorageState kata. */
function read<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    return raw === null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

export function useLocalStorageState<T>(key: string, initial: T): [T, (next: T | ((prev: T) => T)) => void] {
  const [value, setValue] = useState<T>(() => read(key, initial)); // lazy: runs on the first render only

  const set = useCallback(
    (next: T | ((prev: T) => T)) => {
      setValue((prev) => {
        const resolved = typeof next === 'function' ? (next as (prev: T) => T)(prev) : next;
        try {
          window.localStorage.setItem(key, JSON.stringify(resolved));
        } catch {
          // quota exceeded or storage disabled: keep working in memory
        }
        return resolved;
      });
    },
    [key],
  );

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key !== key) return;
      if (e.newValue === null) {
        setValue(initial);
        return;
      }
      try {
        setValue(JSON.parse(e.newValue) as T);
      } catch {
        // another tab wrote garbage: ignore
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
    // initial is deliberately not a dependency: callers often pass a new literal every render
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return [value, set];
}
