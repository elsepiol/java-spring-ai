/** Reference solution for the timing kata. */
export interface Cancellable<A extends unknown[]> {
  (...args: A): void;
  cancel(): void;
}

export function debounce<A extends unknown[]>(fn: (...args: A) => void, ms: number): Cancellable<A> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  const debounced = ((...args: A) => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      timer = undefined;
      fn(...args);
    }, ms);
  }) as Cancellable<A>;
  debounced.cancel = () => {
    clearTimeout(timer);
    timer = undefined;
  };
  return debounced;
}

export function throttle<A extends unknown[]>(fn: (...args: A) => void, ms: number): (...args: A) => void {
  let windowOpen = false;
  let pending: A | undefined;
  const endWindow = () => {
    if (pending) {
      const args = pending;
      pending = undefined;
      fn(...args);
      setTimeout(endWindow, ms); // the trailing call starts a new window
    } else {
      windowOpen = false;
    }
  };
  return (...args: A) => {
    if (!windowOpen) {
      windowOpen = true;
      fn(...args);
      setTimeout(endWindow, ms);
    } else {
      pending = args;
    }
  };
}
