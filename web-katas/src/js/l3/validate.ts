import { todo } from '../../kata';

/**
 * KATA js L3 "validate" (tracks/12-js-ts/L3-production.md, section 2).
 *
 * TypeScript types disappear at runtime: `await res.json() as User` is a promise nobody checks.
 * Validate untrusted data (API responses, localStorage, form input) at the boundary and turn it
 * into a typed value or a list of problems, without exceptions and without `any`:
 *
 * parseUser(input: unknown) -> { ok: true, value: User } | { ok: false, errors: string[] }
 * - input must be a plain object (not null, not an array): else errors ["expected an object"];
 * - id: a positive integer                       -> "id must be a positive integer"
 * - email: a string containing "@", trimmed and lower-cased in the result -> "email is invalid"
 * - role: one of "USER" | "ADMIN"                -> "role must be USER or ADMIN"
 * - tags: optional; if present an array of strings, default []           -> "tags must be strings"
 * Report ALL problems at once, in that field order. Unknown extra fields are dropped from the
 * result (do not pass `__proto__` or anything else through).
 */
export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  role: Role;
  tags: string[];
}

export type Result<T> = { ok: true; value: T } | { ok: false; errors: string[] };

export function parseUser(input: unknown): Result<User> {
  return todo('tracks/12-js-ts/L3-production.md section 2');
}
