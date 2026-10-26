import { todo } from '../../kata';

/**
 * KATA js L2 "promises" (tracks/12-js-ts/L2-practical.md, section 2).
 *
 * all(promises): your own Promise.all: resolves with the results IN INPUT ORDER (not completion
 * order) once all resolve; rejects as soon as ONE rejects, with that reason; [] resolves to [].
 * Do not use Promise.all/allSettled/any/race.
 *
 * withTimeout(promise, ms): settles like the promise, unless ms pass first: then rejects with
 * new TimeoutError(ms). Clear the timer when the promise wins (no timer left behind).
 *
 * mapLimit(items, limit, fn): runs fn on every item with at most `limit` calls in flight at
 * once (e.g. 4 parallel HTTP requests, not 1,000); resolves with results in input order;
 * rejects on the first failure (no NEW calls start after it).
 */
export class TimeoutError extends Error {
  constructor(readonly ms: number) {
    super(`timed out after ${ms} ms`);
    this.name = 'TimeoutError';
  }
}

export function all<T>(promises: Array<Promise<T>>): Promise<T[]> {
  return todo('tracks/12-js-ts/L2-practical.md section 2');
}

export function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return todo('tracks/12-js-ts/L2-practical.md section 2');
}

export function mapLimit<T, R>(items: T[], limit: number, fn: (item: T) => Promise<R>): Promise<R[]> {
  return todo('tracks/12-js-ts/L2-practical.md section 2');
}
