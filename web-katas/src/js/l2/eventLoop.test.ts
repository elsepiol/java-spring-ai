import { describe, expect, it } from 'vitest';
import { predictions } from './eventLoop';

const settle = () => new Promise((resolve) => setTimeout(resolve, 30));

describe('event loop predictions', () => {
  it('basics: sync, microtasks, tasks', async () => {
    const log: string[] = [];
    log.push('1 sync');
    setTimeout(() => log.push('2 timeout'), 0);
    Promise.resolve().then(() => log.push('3 then'));
    queueMicrotask(() => log.push('4 queueMicrotask'));
    log.push('5 sync');
    await settle();
    expect(predictions.basics).toEqual(log);
  });

  it('asyncAwait: where an async function pauses', async () => {
    const log: string[] = [];
    async function f() {
      log.push('f start');
      await null;
      log.push('f after await');
    }
    log.push('before');
    void f();
    log.push('after');
    await settle();
    expect(predictions.asyncAwait).toEqual(log);
  });

  it('nested: tasks queue microtasks and microtasks queue tasks', async () => {
    const log: string[] = [];
    setTimeout(() => {
      log.push('t1');
      Promise.resolve().then(() => log.push('t1 micro'));
    }, 0);
    setTimeout(() => log.push('t2'), 0);
    Promise.resolve().then(() => {
      log.push('m1');
      setTimeout(() => log.push('m1 timeout'), 0);
    });
    await settle();
    expect(predictions.nested).toEqual(log);
  });
});
