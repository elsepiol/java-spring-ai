# Security, L3: method security, injection, abuse, secrets and the OWASP Top 10

**Goal:** enforce authorization where the data is, close the injection family (SQL, XSS, path traversal, open redirects, SSRF), defend against automated abuse, and run a service whose secrets, dependencies and logs do not become the breach.

Prerequisite: L1, L2, Spring core L3 (proxies). Katas: `cd katas && ./mvnw test -Dgroups="security & L3"`. Drill: `java quiz/Quiz.java --track security --level 3`.

---

## 1. Method security

URL rules know paths and roles; they do not know that note 42 belongs to bob. Put the rule on the service method:

```java
@Configuration
@EnableMethodSecurity                       // turns on @PreAuthorize/@PostAuthorize (a proxy, track 06 L3)
class SecurityConfig {}

@Service
class NoteService {
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(long id) { ... }

    @PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
    public Note read(long id) { ... }        // the check runs on the result

    @PreAuthorize("#ownerId == authentication.name")
    public List<Note> byOwner(String ownerId) { ... }   // method parameters by name (-parameters flag)
}
```

- No `Authentication` at all → `AuthenticationCredentialsNotFoundException` (an `AuthenticationException` → 401); denied → `AccessDeniedException` (→ 403, or map it to 404 for IDOR-sensitive resources).
- It is a proxy: **self-invocation skips it**, like `@Transactional`.
- `@PostAuthorize` runs *after* the method: do not use it on methods with side effects. For lists, `@PostFilter` filters in memory; better to query only the caller's rows.
- Complex rules belong in a bean: `@PreAuthorize("@noteAccess.canEdit(#id, authentication)")`.

> **Kata `MethodSecurity`**: owner-or-admin reads, admin-only deletes, anonymous rejected.

---

## 2. The injection family

One root cause: **untrusted data interpreted as code** by some interpreter. The fix is always to keep data and code separate for *that* interpreter.

| Attack | Interpreter | Fix |
|---|---|---|
| **SQL injection** | the database | bind parameters (`?`, `:name`) always; allowlist identifiers (sort columns) (SQL track L1) |
| **XSS** (cross-site scripting) | the browser's HTML/JS | **context-aware output encoding** (React/Thymeleaf escape by default; `dangerouslySetInnerHTML`/`th:utext` do not), a **Content-Security-Policy**, HttpOnly cookies to limit the damage |
| **Path traversal** | the file system | resolve, **normalise**, and check the result is inside the base directory; better, store files under generated names |
| **Open redirect** | the browser's navigation | allow only local paths or an allowlist of hosts; it turns your trusted domain into a phishing link |
| **Command injection** | the shell | never build shell strings; `ProcessBuilder` with an argument list, allowlisted values |
| **SSRF** (server-side request forgery) | your server's HTTP client | user-supplied URLs fetched by the server can reach `169.254.169.254` (cloud metadata credentials) or internal services: allowlist hosts, block private ranges after DNS resolution, no redirects |
| **Deserialization** | `ObjectInputStream`, polymorphic JSON | never deserialize untrusted Java serialization; no default typing in Jackson |
| **Log injection** | log viewers | newlines in user input forge log lines: structured (JSON) logs, encode CR/LF |
| **XXE** | XML parsers | disable external entities and DTDs |

XSS types: **stored** (saved in the database, shown to others), **reflected** (echoed from the request), **DOM-based** (client-side JavaScript writes untrusted data into the page). Escaping depends on the context: HTML text, attributes, JavaScript, URLs and CSS each need their own encoder; validation on input helps but output encoding is the real defence.

> **Kata `InputGuards`**: HTML escaping, a traversal-proof upload path, and an open-redirect check that survives the classic bypasses.

---

## 3. Abuse: brute force, credential stuffing, enumeration

- **Credential stuffing**: passwords leaked elsewhere tried against your login, from many IPs. **Brute force**: many guesses for one account.
- Defences, layered: **rate limits per IP and per account** (track 05's limiters), **progressive delays or temporary lockouts** per account (careful: lockouts let attackers lock out victims; prefer short lockouts plus CAPTCHA or step-up), **MFA**, checking new passwords against breached-password lists (NIST 800-63B; no forced periodic rotation), and alerting on spikes.
- **User enumeration**: answer "invalid username or password" for both cases, take the same time whether the user exists (hash a dummy password), and make "forgot password" always say "if the account exists, we sent an email".
- Normalise identifiers (case, whitespace, Unicode) before counting attempts or comparing; otherwise each spelling gets its own budget.

> **Kata `LoginThrottle`**: per-account sliding window, lockout, normalised names, thread-safe.

---

## 4. Secrets

- Never in Git (including history), images, logs, error messages or the frontend bundle. Scan for them (gitleaks, GitHub secret scanning) in CI.
- Inject at runtime: environment variables or mounted files from a **secret manager** (Vault, AWS Secrets Manager, Kubernetes Secrets with encryption at rest); Spring Boot reads both (`spring.config.import=configtree:/run/secrets/`).
- Rotate them, use different secrets per environment, prefer short-lived credentials (cloud IAM roles, workload identity, dynamic database credentials) over long-lived keys.
- Leaked a secret? **Rotate first**, then clean history: removing the commit does not un-leak it.

---

## 5. Production hygiene

- **TLS everywhere** (HSTS: `Strict-Transport-Security`), including service to service where feasible.
- **Security headers**: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `frame-ancestors` (clickjacking), `Referrer-Policy`. Spring Security sends sensible defaults.
- **Dependencies** (OWASP A06): an SBOM, Dependabot/Renovate, `mvn dependency-check`/OSV scans in CI; Log4Shell showed one transitive library is enough (build track L3).
- **Errors**: no stack traces or SQL in responses (problem details with generic messages; Spring Boot L2).
- **Logging and monitoring**: log authentication events and authorization failures (without passwords or tokens), alert on anomalies, keep an audit trail for sensitive actions.
- **Least privilege at runtime**: the app's database user cannot `DROP`; containers run as non-root with read-only file systems (track 11).

### The OWASP Top 10 (2021) in one line each

A01 broken access control (IDOR, missing checks) · A02 cryptographic failures (plain text, weak hashes, no TLS) · A03 injection (SQL, XSS, ...) · A04 insecure design (no threat model, no rate limits) · A05 security misconfiguration (defaults, verbose errors, open actuator endpoints) · A06 vulnerable and outdated components · A07 identification and authentication failures (stuffing, weak sessions) · A08 software and data integrity failures (unsigned updates, unsafe deserialization, CI/CD tampering) · A09 logging and monitoring failures · A10 SSRF.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `MethodSecurity` | `@EnableMethodSecurity`, `@PreAuthorize`, `@PostAuthorize`, SpEL, 401 vs 403 at the method level |
| `InputGuards` | output encoding, path traversal, open redirects |
| `LoginThrottle` | brute-force defence, normalisation, thread safety |

```
cd katas && ./mvnw test -Dgroups="security & L3"
```

## 7. Seen in the shop project

- `SecurityProblemHandlers`: 401/403 answered as problem details, with no internals leaked.
- Actuator: health and info public, everything else `ADMIN` only.
- `RateLimitFilter` (Level 5) placed after authorization, so refused requests do not use up a quota; secrets come from environment variables in the deployment level.


## 8. Interview talk track

"I separate authentication from authorization and deny by default. URL rules in the SecurityFilterChain handle coarse access; ownership is enforced where the data is, with method security or scoped queries, and objects the caller may not see answer 404. Passwords are hashed with bcrypt or Argon2 and upgraded on login. For an API I use short-lived bearer tokens verified by a library against the issuer's keys, which also lets me disable CSRF safely since no cookie authenticates; for browser sessions I keep HttpOnly, Secure, SameSite cookies with CSRF tokens. Against injection I bind SQL parameters, rely on the frontend framework's escaping plus a CSP, and normalise and check paths and redirect targets. Login is rate limited per account and IP without revealing which usernames exist. Secrets come from a secret manager, dependencies are scanned in CI, and auth events are logged and alerted on."

**Questions you should be able to answer:**
1. URL-based vs method-based authorization: when do you need each?
2. Why does `@PreAuthorize` on a method called from the same class not work?
3. Name four injection attacks and the specific fix for each.
4. Stored vs reflected vs DOM XSS. What does a Content-Security-Policy do?
5. How do you safely serve user-uploaded files?
6. What is SSRF, and why is it dangerous in the cloud?
7. How do you prevent brute force without letting attackers lock out users?
8. A secret was committed to a public repo. What do you do, in order?
9. Pick three OWASP Top 10 items and give an example from a Spring app.
