/** Reference solution for the eventLoop kata. */
export const predictions: Record<'basics' | 'asyncAwait' | 'nested', string[]> = {
  // sync first; then microtasks in the order queued; the timeout is a (macro)task, last
  basics: ['1 sync', '5 sync', '3 then', '4 queueMicrotask', '2 timeout'],
  // an async function runs synchronously until its first await; the rest is a microtask
  asyncAwait: ['before', 'f start', 'after', 'f after await'],
  // the microtask runs before any task; each task is followed by all microtasks it queued;
  // the timeout queued by m1 lands behind the two already waiting
  nested: ['m1', 't1', 't1 micro', 't2', 'm1 timeout'],
};
