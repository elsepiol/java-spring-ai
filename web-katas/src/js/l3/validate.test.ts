import { describe, expect, it } from 'vitest';
import { parseUser } from './validate';

describe('parseUser', () => {
  it('accepts and normalises a valid user', () => {
    expect(parseUser({ id: 7, email: '  Ada@Example.COM ', role: 'ADMIN', tags: ['beta'] })).toEqual({
      ok: true,
      value: { id: 7, email: 'ada@example.com', role: 'ADMIN', tags: ['beta'] },
    });
  });

  it('defaults tags and drops unknown fields', () => {
    const result = parseUser(JSON.parse('{"id":1,"email":"a@b.c","role":"USER","isAdmin":true,"__proto__":{"polluted":1}}'));
    expect(result).toEqual({ ok: true, value: { id: 1, email: 'a@b.c', role: 'USER', tags: [] } });
    if (result.ok) {
      expect(Object.keys(result.value)).toEqual(['id', 'email', 'role', 'tags']);
      expect((result.value as unknown as Record<string, unknown>).polluted).toBeUndefined();
    }
  });

  it('reports every problem at once, in field order', () => {
    expect(parseUser({ id: '7', email: 'nope', role: 'ROOT', tags: [1] })).toEqual({
      ok: false,
      errors: ['id must be a positive integer', 'email is invalid', 'role must be USER or ADMIN', 'tags must be strings'],
    });
  });

  it('checks numbers strictly', () => {
    for (const id of [0, -1, 1.5, NaN, Infinity]) {
      expect(parseUser({ id, email: 'a@b.c', role: 'USER' })).toEqual({ ok: false, errors: ['id must be a positive integer'] });
    }
  });

  it('rejects things that are not objects', () => {
    for (const input of [null, undefined, 'user', 42, [{ id: 1 }]]) {
      expect(parseUser(input)).toEqual({ ok: false, errors: ['expected an object'] });
    }
  });
});
