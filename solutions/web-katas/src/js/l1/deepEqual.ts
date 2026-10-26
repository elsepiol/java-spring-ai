/** Reference solution for the deepEqual kata. */
export function deepEqual(a: unknown, b: unknown): boolean {
  return eq(a, b, new Map());
}

function eq(a: unknown, b: unknown, seen: Map<object, object>): boolean {
  if (a === b) return true;
  if (typeof a === 'number' && typeof b === 'number') return Number.isNaN(a) && Number.isNaN(b);
  if (typeof a !== 'object' || typeof b !== 'object' || a === null || b === null) return false;
  if (seen.get(a) === b) return true; // already comparing this pair further up: assume equal
  seen.set(a, b);
  if (a instanceof Date || b instanceof Date) {
    return a instanceof Date && b instanceof Date && a.getTime() === b.getTime();
  }
  if (Array.isArray(a) !== Array.isArray(b)) return false;
  if (Array.isArray(a) && Array.isArray(b)) {
    return a.length === b.length && a.every((x, i) => eq(x, b[i], seen));
  }
  const ka = Object.keys(a);
  const kb = Object.keys(b);
  if (ka.length !== kb.length) return false;
  return ka.every(
    (k) => Object.prototype.hasOwnProperty.call(b, k) && eq((a as Record<string, unknown>)[k], (b as Record<string, unknown>)[k], seen),
  );
}
