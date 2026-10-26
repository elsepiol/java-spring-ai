import { todo } from '../../kata';

/**
 * KATA js L3 "emitter" (tracks/12-js-ts/L3-production.md, section 3).
 *
 * A typed event emitter: the event map type decides which events exist and what payload each
 * carries, so `emit('login', 42)` does not compile when 'login' carries a User.
 * - on(event, listener) registers and returns an unsubscribe function (calling it twice is
 *   harmless). Forgetting to unsubscribe is the classic memory leak in SPAs.
 * - once(event, listener): runs at most once, then removes itself;
 * - emit(event, payload): calls the listeners registered at the moment emit starts, in
 *   registration order. A listener that unsubscribes itself or others DURING the emit must
 *   not make anyone else get skipped; listeners added during the emit run from the next emit.
 *   A listener that throws must not stop the others: collect the errors and, after all ran,
 *   throw an AggregateError if there were any.
 * - listenerCount(event).
 */
export type Listener<T> = (payload: T) => void;

export class Emitter<Events extends Record<string, unknown>> {
  on<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    return todo('tracks/12-js-ts/L3-production.md section 3');
  }

  once<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    return todo('tracks/12-js-ts/L3-production.md section 3');
  }

  emit<K extends keyof Events>(event: K, payload: Events[K]): void {
    todo('tracks/12-js-ts/L3-production.md section 3');
  }

  listenerCount<K extends keyof Events>(event: K): number {
    return todo('tracks/12-js-ts/L3-production.md section 3');
  }
}
