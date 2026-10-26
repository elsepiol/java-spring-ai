/** Reference solution for the emitter kata. */
export type Listener<T> = (payload: T) => void;

export class Emitter<Events extends Record<string, unknown>> {
  private readonly listeners = new Map<keyof Events, Array<Listener<never>>>();

  on<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    const list = this.listeners.get(event) ?? [];
    // a new wrapper object per registration, so the same function can be registered twice
    const entry: Listener<Events[K]> = (payload) => listener(payload);
    this.listeners.set(event, [...list, entry as Listener<never>]);
    return () => {
      const current = this.listeners.get(event) ?? [];
      this.listeners.set(event, current.filter((l) => l !== entry));
    };
  }

  once<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    const off = this.on(event, (payload) => {
      off();
      listener(payload);
    });
    return off;
  }

  emit<K extends keyof Events>(event: K, payload: Events[K]): void {
    const snapshot = this.listeners.get(event) ?? []; // arrays are replaced, never mutated
    const errors: unknown[] = [];
    for (const listener of snapshot) {
      try {
        (listener as Listener<Events[K]>)(payload);
      } catch (e) {
        errors.push(e);
      }
    }
    if (errors.length > 0) throw new AggregateError(errors, `${errors.length} listener(s) failed for ${String(event)}`);
  }

  listenerCount<K extends keyof Events>(event: K): number {
    return (this.listeners.get(event) ?? []).length;
  }
}
