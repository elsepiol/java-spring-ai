/** Reference solution for the validate kata: narrow unknown step by step. */
export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  role: Role;
  tags: string[];
}

export type Result<T> = { ok: true; value: T } | { ok: false; errors: string[] };

function isRecord(x: unknown): x is Record<string, unknown> {
  return typeof x === 'object' && x !== null && !Array.isArray(x);
}

function isRole(x: unknown): x is Role {
  return x === 'USER' || x === 'ADMIN';
}

export function parseUser(input: unknown): Result<User> {
  if (!isRecord(input)) return { ok: false, errors: ['expected an object'] };
  const errors: string[] = [];
  const { id, email, role, tags } = input;
  if (typeof id !== 'number' || !Number.isInteger(id) || id <= 0) errors.push('id must be a positive integer');
  const normalised = typeof email === 'string' ? email.trim().toLowerCase() : '';
  if (!normalised.includes('@')) errors.push('email is invalid');
  if (!isRole(role)) errors.push('role must be USER or ADMIN');
  if (tags !== undefined && !(Array.isArray(tags) && tags.every((t) => typeof t === 'string'))) errors.push('tags must be strings');
  if (errors.length > 0) return { ok: false, errors };
  return { ok: true, value: { id: id as number, email: normalised, role: role as Role, tags: tags === undefined ? [] : [...(tags as string[])] } };
}
