import { todo } from '../../kata';

/**
 * KATA js L1 "deepEqual" (tracks/12-js-ts/L1-basics.md, section 2).
 *
 * `===` compares objects by reference: {a: 1} === {a: 1} is false. Write the structural
 * comparison that test libraries use (like Vitest's toEqual, strict about types):
 * - primitives compare with Object.is semantics: NaN equals NaN, but 0 and -0 are EQUAL here
 *   (use === for numbers, plus the NaN case);
 * - arrays: same length and deepEqual element by element; an array never equals a plain object;
 * - Dates: same time value;
 * - plain objects: the same own enumerable keys (in any order) with deepEqual values;
 * - null is not an object here: deepEqual(null, {}) is false; undefined only equals undefined;
 * - functions compare by reference;
 * - cycles must not overflow the stack: two structures that are both cyclic in the same way
 *   are equal (remember which pairs you are already comparing).
 */
export function deepEqual(a: unknown, b: unknown): boolean {
  return todo('tracks/12-js-ts/L1-basics.md section 2');
}
