/** Reference solution for the promises kata. */
export class TimeoutError extends Error {
  constructor(readonly ms: number) {
    super(`timed out after ${ms} ms`);
    this.name = 'TimeoutError';
  }
}

export function all<T>(promises: Array<Promise<T>>): Promise<T[]> {
  return new Promise((resolve, reject) => {
    const results: T[] = new Array(promises.length);
    let remaining = promises.length;
    if (remaining === 0) {
      resolve(results);
      return;
    }
    promises.forEach((p, i) => {
      p.then((value) => {
        results[i] = value;
        remaining -= 1;
        if (remaining === 0) resolve(results);
      }, reject);
    });
  });
}

export function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new TimeoutError(ms)), ms);
    promise.then(
      (value) => {
        clearTimeout(timer);
        resolve(value);
      },
      (error: unknown) => {
        clearTimeout(timer);
        reject(error);
      },
    );
  });
}

export function mapLimit<T, R>(items: T[], limit: number, fn: (item: T) => Promise<R>): Promise<R[]> {
  return new Promise((resolve, reject) => {
    const results: R[] = new Array(items.length);
    let next = 0;
    let done = 0;
    let failed = false;
    if (items.length === 0) {
      resolve(results);
      return;
    }
    const launch = () => {
      if (failed || next >= items.length) return;
      const index = next++;
      fn(items[index]).then(
        (value) => {
          results[index] = value;
          done += 1;
          if (done === items.length) resolve(results);
          else launch();
        },
        (error: unknown) => {
          failed = true;
          reject(error);
        },
      );
    };
    for (let i = 0; i < Math.min(limit, items.length); i++) launch();
  });
}
