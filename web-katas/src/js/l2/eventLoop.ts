/**
 * KATA js L2 "eventLoop" (tracks/12-js-ts/L2-practical.md, section 1).
 *
 * Predict, then run. For each snippet in eventLoop.test.ts, write down the order in which the
 * log lines appear. Work it out on paper with the event loop rules (synchronous code first,
 * then ALL microtasks, then ONE task, then all microtasks again, ...) BEFORE running the test:
 * the failure message shows the real order, so running first spoils the exercise.
 */
export const predictions: Record<'basics' | 'asyncAwait' | 'nested', string[]> = {
  basics: [],
  asyncAwait: [],
  nested: [],
};
