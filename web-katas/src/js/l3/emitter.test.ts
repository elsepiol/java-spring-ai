import { describe, expect, it, vi } from 'vitest';
import { Emitter } from './emitter';

type Events = { login: { name: string }; logout: undefined; tick: number };

describe('Emitter', () => {
  it('calls listeners in registration order with the payload', () => {
    const e = new Emitter<Events>();
    const calls: string[] = [];
    e.on('login', (u) => calls.push(`a:${u.name}`));
    e.on('login', (u) => calls.push(`b:${u.name}`));
    e.emit('login', { name: 'ada' });
    expect(calls).toEqual(['a:ada', 'b:ada']);
    e.emit('logout', undefined); // no listeners: fine
  });

  it('unsubscribes, idempotently', () => {
    const e = new Emitter<Events>();
    const fn = vi.fn();
    const off = e.on('tick', fn);
    e.emit('tick', 1);
    off();
    off();
    e.emit('tick', 2);
    expect(fn).toHaveBeenCalledTimes(1);
    expect(e.listenerCount('tick')).toBe(0);
  });

  it('the same function registered twice is two registrations', () => {
    const e = new Emitter<Events>();
    const fn = vi.fn();
    const off1 = e.on('tick', fn);
    e.on('tick', fn);
    off1();
    e.emit('tick', 1);
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('once runs a single time', () => {
    const e = new Emitter<Events>();
    const fn = vi.fn();
    e.once('tick', fn);
    e.emit('tick', 1);
    e.emit('tick', 2);
    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn).toHaveBeenCalledWith(1);
    expect(e.listenerCount('tick')).toBe(0);
  });

  it('does not skip listeners when one unsubscribes during emit', () => {
    const e = new Emitter<Events>();
    const calls: string[] = [];
    const offA = e.on('tick', () => {
      calls.push('a');
      offA();
    });
    e.on('tick', () => calls.push('b'));
    e.on('tick', () => calls.push('c'));
    e.emit('tick', 1);
    expect(calls).toEqual(['a', 'b', 'c']);
  });

  it('runs listeners added during emit only from the next emit', () => {
    const e = new Emitter<Events>();
    const late = vi.fn();
    e.on('tick', () => {
      if (e.listenerCount('tick') === 1) e.on('tick', late);
    });
    e.emit('tick', 1);
    expect(late).not.toHaveBeenCalled();
    e.emit('tick', 2);
    expect(late).toHaveBeenCalledWith(2);
  });

  it('a throwing listener does not stop the others', () => {
    const e = new Emitter<Events>();
    const after = vi.fn();
    e.on('tick', () => {
      throw new Error('bad listener');
    });
    e.on('tick', after);
    expect(() => e.emit('tick', 1)).toThrow(AggregateError);
    expect(after).toHaveBeenCalledTimes(1);
  });
});
