# Security, L1: authentication, authorization, passwords

**Goal:** speak the vocabulary precisely (authentication vs authorization, 401 vs 403), design access rules that do not leak data, and store passwords the way a breach cannot undo.

Prerequisite: Web/HTTP L1. Katas: `cd katas && ./mvnw test -Dgroups="security & L1"`. Drill: `java quiz/Quiz.java --track security --level 1`.

---

## 1. The vocabulary

| Term | Question it answers | Fails with |
|---|---|---|
| **Authentication** (authn) | *Who are you?* Prove an identity: password, token, certificate, passkey | **401 Unauthorized** (badly named: it means *unauthenticated*), with a `WWW-Authenticate` challenge |
| **Authorization** (authz) | *What may you do?* Roles, permissions, ownership | **403 Forbidden** (we know who you are; the answer is no) |
| **Principal** | the authenticated identity (`alice`) | |
| **Credential** | what proves it (password, token) | |
| **Session / token** | how the next request is recognised without re-sending the password | |

The CIA triad: **confidentiality** (only the right people read it), **integrity** (nobody tampers with it), **availability** (it stays up). Security bugs break one of the three.

Principles you will apply everywhere:
- **Deny by default**: every route needs a rule; new endpoints are closed until opened.
- **Least privilege**: users, services and database accounts get only what they need.
- **Defence in depth**: URL rules *and* method/data checks *and* database constraints.
- **Never trust the client**: hidden fields, disabled buttons, JavaScript checks and the `role` in a request body are suggestions; the server decides.

---

## 2. Access control and IDOR

The most common serious vulnerability class (OWASP **A01: Broken Access Control**) is not exotic: it is an endpoint that checks *that* you are logged in but not *whether this object is yours*.

```
GET /api/invoices/1041   (logged in as alice)  -> alice's invoice
GET /api/invoices/1042   (still alice)         -> bob's invoice!   <- Insecure Direct Object Reference (IDOR)
```

Fix: every lookup by id checks ownership (or scopes the query: `where id = :id and owner_id = :me`). And decide what the response reveals:
- **404 for objects the caller may not see**: a 403 would confirm that invoice 1042 exists, which lets attackers enumerate ids.
- **403 when they can see it but not perform this action** (support staff may read, not delete).
- **401** only when nobody is logged in.

Role-based access control (**RBAC**: `ADMIN`, `SUPPORT`) answers "what kind of user"; ownership rules (attribute-based, **ABAC**) answer "which rows". Real systems need both. Random UUIDs instead of sequential ids make guessing harder, but they are not access control.

> **Kata `AccessPolicy`**: 401 vs 403 vs 404 for owners, support, admins and strangers.

---

## 3. Storing passwords

Assume the database **will** leak (backups, SQL injection, an insider). Then:
- **Plain text**: every account is gone, and people reuse passwords on other sites.
- **Encryption**: whoever has the key (the app server) can decrypt all of them. Passwords are never decrypted, so they must not be encrypted.
- **A fast hash** (`SHA-256(password)`): GPUs compute billions per second; precomputed tables (rainbow tables) reverse common passwords instantly; equal passwords have equal hashes.
- **A salted, slow key-derivation function**: correct.

```
stored = "pbkdf2-sha256$600000$<salt>$<hash>"
hash   = PBKDF2-HMAC-SHA256(password, salt = 16 random bytes, iterations = 600_000)
```

- **Salt**: random per password, stored next to the hash. Equal passwords get different hashes and precomputed tables are useless.
- **Work factor**: deliberately slow (~100 ms) so guessing costs the attacker as much as possible; raise it over the years, and **rehash on login** when a stored hash uses old parameters (you only see the plain password at login).
- **Algorithms**: **Argon2id** (memory-hard, the first choice), **bcrypt** (Spring Security's default `{bcrypt}`, limit 72 bytes), **scrypt**, **PBKDF2** (FIPS-approved, in the JDK). Never MD5/SHA-x alone.
- **Verify in constant time** (`MessageDigest.isEqual`): `String.equals` returns as soon as a byte differs, and timing can leak information.
- A **pepper** (a secret key in the application, HMAC'd into the hash) adds protection when only the database leaks.

In Spring: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` stores `{bcrypt}$2a$10$...`; the `{id}` prefix lets you change algorithms later while old hashes keep working; `upgradeEncoding()` tells you to rehash.

> **Kata `PasswordHasher`**: PBKDF2 with salt, self-describing format, constant-time verify, rehash detection.

---

## 4. Sessions vs tokens (preview of L2)

After login, the server must recognise the next requests:
- **Server-side session**: a random session id in a cookie (`JSESSIONID`), the state on the server (memory, Redis). Easy to revoke. Needs CSRF protection because browsers send cookies automatically.
- **Bearer token** (often a JWT): the client sends `Authorization: Bearer <token>`; a signed token carries the claims, so servers can verify it without shared state. Hard to revoke before it expires.

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `AccessPolicy` | authn vs authz, RBAC + ownership, 401/403/404, IDOR |
| `PasswordHasher` | salts, slow KDFs, constant-time comparison, upgrading hashes |

```
cd katas && ./mvnw test -Dgroups="security & L1"
```

## 6. Seen in the shop project

- `AuthService`: passwords hashed with Spring Security's `PasswordEncoder`; normalised user names; a dummy hash checked for unknown users so login takes the same time whether or not the account exists.
- `OrderService`: orders are looked up for the current customer; someone else's order id ends in `OrderNotFoundException` (404).


## 7. Interview checklist

1. Authentication vs authorization. 401 vs 403.
2. What is an IDOR? How do you prevent it? When answer 404 instead of 403?
3. RBAC vs ownership (attribute-based) rules.
4. Why not encrypt passwords? Why not SHA-256?
5. What do the salt and the work factor protect against?
6. bcrypt vs Argon2 vs PBKDF2.
7. How do you migrate users to a stronger hash without resetting all passwords?
8. Why constant-time comparison?
9. Sessions vs tokens: one advantage of each.
