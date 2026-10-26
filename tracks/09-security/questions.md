# Security: question bank

Drill with `java quiz/Quiz.java --track security`.

## L1

### SEC-L1-001 · recall
Authentication vs authorization?

<details><summary>Answer</summary>

Authentication establishes who the caller is (password, token, certificate, passkey). Authorization decides what that authenticated caller may do (roles, permissions, ownership). Authentication comes first; authorization needs its result.

</details>

### SEC-L1-002 · recall
When does an API answer 401, and when 403?

<details><summary>Answer</summary>

401 Unauthorized: no valid authentication (missing, wrong or expired credentials); it should carry a `WWW-Authenticate` challenge. 403 Forbidden: the caller is authenticated but not allowed to do this. Logging in again does not help with a 403.

</details>

### SEC-L1-003 · recall
What is an IDOR?

<details><summary>Answer</summary>

Insecure Direct Object Reference: the server uses an id from the request (`/invoices/1042`) without checking that the caller may access that object, so changing the id exposes other users' data. It is the classic broken-access-control bug (OWASP A01).

</details>

### SEC-L1-004 · why
Why answer 404 rather than 403 when a user requests another user's private document?

<details><summary>Answer</summary>

A 403 confirms that the document exists, so ids can be enumerated (which order numbers, user ids, file names exist). A 404 makes "not yours" indistinguishable from "does not exist". Use 403 when the caller can already see the object but may not perform the action.

</details>

### SEC-L1-005 · recall
Name four security principles that should shape every design.

<details><summary>Answer</summary>

Deny by default (everything closed unless explicitly opened), least privilege (minimum rights for users, services and database accounts), defence in depth (several independent layers), and never trust the client (all checks on the server). Also: fail securely, minimise the attack surface, keep it simple.

</details>

### SEC-L1-006 · bug
A React app hides the "Delete user" button unless `user.role === 'ADMIN'`. The API endpoint `DELETE /api/users/{id}` only requires a logged-in user. Problem?

<details><summary>Answer</summary>

The UI check is cosmetic: anyone can call the API directly (curl, devtools) and delete users. Authorization must be enforced on the server (`hasRole('ADMIN')` on the route or the method). The UI check only improves the user experience.

</details>

### SEC-L1-007 · recall
RBAC vs ownership/attribute-based rules: examples of each.

<details><summary>Answer</summary>

RBAC (role-based): "ADMINs may delete products", "SUPPORT may read tickets". Attribute/ownership-based (ABAC): "a user may read an order only if order.customerId = user.id", "a manager may approve expenses of their own team under 5,000". Most applications need both: roles for kinds of users, ownership for which rows.

</details>

### SEC-L1-008 · why
Why must passwords not be stored encrypted?

<details><summary>Answer</summary>

Encryption is reversible: whoever has the key (the application, an attacker who compromises it, an insider) can decrypt every password. Login never needs the original password, only a check that the given one matches, so a one-way slow hash is the right tool.

</details>

### SEC-L1-009 · why
Why is `SHA-256(password)` not good enough?

<details><summary>Answer</summary>

It is fast: GPUs compute billions per second, so common passwords fall immediately; without a salt, precomputed rainbow tables reverse hashes instantly and equal passwords have equal hashes (one cracked = all cracked). You need a per-password salt and a deliberately slow, ideally memory-hard function.

</details>

### SEC-L1-010 · recall
What does a salt protect against, and is it secret?

<details><summary>Answer</summary>

A random per-password value mixed into the hash: equal passwords get different hashes, and precomputed tables are useless because each hash must be attacked individually. It is not secret: it is stored next to the hash. (A secret value is a pepper.)

</details>

### SEC-L1-011 · recall
Which password hashing algorithms are acceptable today?

<details><summary>Answer</summary>

Argon2id (first choice, memory-hard), bcrypt (Spring Security's default, cost 10-12, inputs limited to 72 bytes), scrypt, and PBKDF2-HMAC-SHA256 with a high iteration count (≈600,000 per OWASP 2023, 210,000 for SHA-512; FIPS environments). Not MD5, SHA-1 or a single SHA-2 pass.

</details>

### SEC-L1-012 · scenario
Your user table has 2 million bcrypt hashes with cost 8. You want cost 12. How do you migrate without a password reset?

<details><summary>Answer</summary>

Rehash on login: after a successful verification you have the plain password, so if the stored hash uses old parameters (`upgradeEncoding()`/`needsRehash`), hash it again with the new ones and update the row. A self-describing format (`$2a$08$...`, `{bcrypt}`, `pbkdf2$iterations$...`) makes old and new coexist. For accounts that never log in, wrap the old hash in a new one (hash the hash) or expire them.

</details>

### SEC-L1-013 · why
Why compare hashes with `MessageDigest.isEqual` rather than `Arrays.equals`?

<details><summary>Answer</summary>

`Arrays.equals`/`String.equals` stop at the first differing byte, so response time leaks how many leading bytes matched (a timing side channel). `MessageDigest.isEqual` is constant-time for equal-length inputs. It matters most for MACs and tokens compared directly, such as HMAC signatures and API keys.

</details>

### SEC-L1-014 · recall
What does `{bcrypt}` at the start of a Spring Security password hash mean?

<details><summary>Answer</summary>

`DelegatingPasswordEncoder` stores the algorithm id as a prefix. It verifies with whichever encoder the prefix names and encodes new passwords with the current default, so you can change algorithms without breaking existing hashes. `upgradeEncoding` reports hashes to rehash.

</details>

### SEC-L1-015 · recall
Session cookie vs bearer token: one advantage and one drawback of each.

<details><summary>Answer</summary>

Session (random id in a cookie, state on the server): easy to revoke and small, but needs server-side storage shared by all instances and CSRF protection because browsers send cookies automatically. Bearer token (often a JWT in the Authorization header): stateless verification and no CSRF exposure, but hard to revoke before expiry, and the client has to store it somewhere safe.

</details>

### SEC-L1-016 · recall
What are the C, I and A in the CIA triad? Give an attack on each.

<details><summary>Answer</summary>

Confidentiality (data leak via IDOR or SQL injection), integrity (tampering, e.g. a forged JWT granting admin, a price changed in the request), availability (denial of service, brute-force lockouts, a regex DoS).

</details>

### SEC-L1-017 · scenario
Your API uses sequential ids. A colleague proposes switching to UUIDs "for security". Comment.

<details><summary>Answer</summary>

UUIDs make ids unguessable, which slows enumeration and hides business volumes (order 100231 says how many orders you have). They are not access control: a leaked or shared UUID still grants access if the ownership check is missing. Fix the authorization check first; UUIDs are a nice extra layer.

</details>

### SEC-L1-018 · bug
The login endpoint returns "unknown user" for missing accounts and "wrong password" for existing ones. Problem and fix?

<details><summary>Answer</summary>

User enumeration: attackers learn which emails have accounts (for credential stuffing or phishing). Answer "invalid email or password" in both cases, and make the timing equal too (verify against a dummy hash when the user does not exist, as the shop's AuthService does).

</details>

### SEC-L1-019 · interview
"How would you design authorization for a multi-tenant SaaS API?"

<details><summary>Answer</summary>

Authenticate users with tokens that carry the tenant and roles. Enforce tenant isolation on every query (a tenant_id column in every table, added by a repository layer or row-level security in PostgreSQL, never trusting a tenant id from the request body). Use roles for coarse permissions and ownership rules per object, deny by default, answer 404 for other tenants' ids, and test cross-tenant access explicitly. Audit-log sensitive actions.

</details>

### SEC-L1-020 · dsa
An attacker can try 10 billion SHA-256 hashes per second. Roughly how long does it take to try all 8-character lowercase-plus-digit passwords? And with bcrypt at 10 hashes per second per core?

<details><summary>Answer</summary>

36^8 ≈ 2.8 × 10^12 candidates. At 10^10/s: about 280 seconds (under 5 minutes). At 10/s: about 2.8 × 10^11 seconds, thousands of years per core. The search space is the same; the work factor multiplies the cost of every guess, which is the whole point of slow hashes.

</details>

## L2

### SEC-L2-001 · recall
What are the three parts of a JWT, and how is each produced?

<details><summary>Answer</summary>

Header (JSON with alg and typ), payload (JSON claims: sub, exp, iat, iss, aud, custom roles), and signature (for HS256: HMAC-SHA256(secret, base64url(header) + "." + base64url(payload))). Each part is base64url without padding, joined by dots.

</details>

### SEC-L2-002 · predict
Can a user read the roles inside their JWT access token? Can they change them?

<details><summary>Answer</summary>

Read: yes, the payload is only base64url-encoded, not encrypted. Change: they can edit the payload, but the signature no longer matches and verification fails (as long as the server checks the signature with an algorithm it chose itself).

</details>

### SEC-L2-003 · recall
Explain the `alg: none` attack.

<details><summary>Answer</summary>

The attacker changes the header to `{"alg":"none"}`, edits the payload (role ADMIN) and sends an empty signature. A naive library that trusts the header's alg skips verification. Fix: the verifier fixes the accepted algorithm(s) itself and never lets the token choose.

</details>

### SEC-L2-004 · recall
Explain the RS256-to-HS256 algorithm confusion attack.

<details><summary>Answer</summary>

The server verifies RS256 tokens with a public key. The attacker creates an HS256 token signed with HMAC using that public key (which is public) as the secret. A library that picks the algorithm from the header and uses "the key" for whatever algorithm accepts it. Fix: bind keys to algorithms; accept only RS256 for that key.

</details>

### SEC-L2-005 · design
HS256 or RS256 for tokens your auth service issues and ten microservices verify?

<details><summary>Answer</summary>

RS256 (or ES256): the auth service signs with its private key; services verify with the public key fetched from a JWKS endpoint, and none of them can mint tokens. With HS256 every verifier holds the secret and can forge tokens, and rotating the secret means touching all ten. Use HS256 only when the issuer and the verifier are the same service.

</details>

### SEC-L2-006 · scenario
A user's laptop is stolen; their JWT is valid for 24 hours. What can you do, and how should the system have been designed?

<details><summary>Answer</summary>

With pure stateless JWTs, little: wait for expiry, or rotate the signing key (logging everyone out), or add the token's `jti` or the user to a denylist checked on each request. Better design: short-lived access tokens (5-15 minutes) plus revocable, rotating refresh tokens stored server-side; revoking the refresh token ends the session within minutes. Add a `tokens-valid-after` timestamp per user for "log out everywhere".

</details>

### SEC-L2-007 · recall
Which checks must JWT verification perform besides the signature?

<details><summary>Answer</summary>

The algorithm is the expected one; `exp` not passed and `nbf` reached (with a small clock skew); `iss` is the trusted issuer; `aud` includes this service; required claims (`sub`) are present; the key id (`kid`) maps to a known key. Then map claims to authorities.

</details>

### SEC-L2-008 · recall
Describe Spring Security's architecture in a few sentences.

<details><summary>Answer</summary>

A `DelegatingFilterProxy` in the servlet container delegates to `FilterChainProxy`, which picks the first `SecurityFilterChain` matching the request and runs its filters: load the security context, CSRF, authentication filters (Basic, bearer token, form login) that call the `AuthenticationManager` → providers → `UserDetailsService` + `PasswordEncoder`, `ExceptionTranslationFilter` (401/403), and `AuthorizationFilter` applying the `authorizeHttpRequests` rules. The authenticated user lives in the thread-bound `SecurityContextHolder`.

</details>

### SEC-L2-009 · bug
```java
.authorizeHttpRequests(a -> a
    .anyRequest().authenticated()
    .requestMatchers("/api/public/**").permitAll())
```
What happens?

<details><summary>Answer</summary>

Spring Security rejects the configuration at startup: `anyRequest()` must be last, since matchers are evaluated in order and the first match wins, so anything after a catch-all could never apply. Put specific matchers first and `anyRequest()` last.

</details>

### SEC-L2-010 · recall
`hasRole("ADMIN")` vs `hasAuthority("ADMIN")`.

<details><summary>Answer</summary>

`hasRole("ADMIN")` checks for the authority `ROLE_ADMIN` (it adds the `ROLE_` prefix); `hasAuthority("ADMIN")` checks the exact string `ADMIN`. Mixing them up is a common reason for unexpected 403s, e.g. when JWT scopes are mapped to `SCOPE_x` authorities.

</details>

### SEC-L2-011 · predict
In a `@Transactional` service method you start `CompletableFuture.runAsync(() -> auditService.record())`, and `record()` reads `SecurityContextHolder.getContext().getAuthentication()`. What does it get?

<details><summary>Answer</summary>

Usually null: the security context is held in a ThreadLocal, and the pool thread is a different thread. Propagate it with `DelegatingSecurityContextExecutor`/`DelegatingSecurityContextRunnable`, or pass the user explicitly as an argument (clearer).

</details>

### SEC-L2-012 · recall
What is CSRF? Describe an attack.

<details><summary>Answer</summary>

Cross-site request forgery: a malicious site makes the victim's browser send a state-changing request to a site where the victim is logged in. The browser attaches that site's cookies automatically, e.g. an auto-submitting hidden form `POST https://bank.example/transfer` on evil.example, so the request runs with the victim's session.

</details>

### SEC-L2-013 · recall
What are the defences against CSRF?

<details><summary>Answer</summary>

CSRF tokens (synchronizer token per session or form, or double-submit cookie + header for SPAs), SameSite cookies (Lax/Strict), checking `Origin`/`Sec-Fetch-Site` headers, and never changing state on GET. Spring Security enables CSRF tokens by default.

</details>

### SEC-L2-014 · why
Why is it safe to disable CSRF protection for an API authenticated only by `Authorization: Bearer` headers?

<details><summary>Answer</summary>

CSRF relies on credentials the browser attaches automatically (cookies, Basic auth cached by the browser, client certificates). A cross-site page cannot make the browser add an Authorization header with your token, and it cannot read the token. If the token is moved into a cookie, CSRF protection is needed again.

</details>

### SEC-L2-015 · recall
What does CORS do, and what does it not do?

<details><summary>Answer</summary>

It lets a server tell browsers which other origins may read its responses (and send credentials or non-simple requests, after a preflight). It does not protect the server: it is enforced only by browsers, non-browser clients ignore it, and requests may still reach the server. It is a relaxation of the same-origin policy, not an access-control mechanism.

</details>

### SEC-L2-016 · bug
A Spring app reflects any request's `Origin` into `Access-Control-Allow-Origin` and sends `Access-Control-Allow-Credentials: true`. Risk?

<details><summary>Answer</summary>

Any website can make the victim's browser call your API with their cookies and read the responses, exfiltrating private data. It defeats the same-origin policy entirely. Use an explicit allowlist of trusted origins.

</details>

### SEC-L2-017 · recall
What do the cookie flags HttpOnly, Secure and SameSite do?

<details><summary>Answer</summary>

HttpOnly: JavaScript cannot read the cookie (XSS cannot steal the session). Secure: sent only over HTTPS. SameSite=Lax: not sent on cross-site subrequests and POSTs (CSRF mitigation), sent on top-level GET navigations; Strict: never cross-site; None: always (requires Secure).

</details>

### SEC-L2-018 · design
Where should a single-page application keep its access token?

<details><summary>Answer</summary>

Options: memory only (lost on reload; use a refresh flow), localStorage (persistent but readable by any injected script: XSS = token theft), or an HttpOnly Secure SameSite cookie (not readable by scripts but needs CSRF protection). The currently recommended pattern is a backend-for-frontend: the server keeps the OAuth tokens and the browser holds only an HttpOnly session cookie.

</details>

### SEC-L2-019 · recall
What is session fixation, and how does Spring Security prevent it?

<details><summary>Answer</summary>

An attacker plants a known session id in the victim's browser, waits for them to log in, then uses the same session id as an authenticated user. Spring Security changes the session id at login (`changeSessionId` by default), so the pre-login id becomes useless.

</details>

### SEC-L2-020 · interview
"Explain OAuth2 and OpenID Connect to a backend developer."

<details><summary>Answer</summary>

OAuth2 is delegated authorization: an authorization server issues access tokens that let a client call resource servers (APIs) on a user's behalf, with scopes; flows include authorization code + PKCE (browsers, mobile) and client credentials (service to service). OpenID Connect adds authentication on top: an ID token (a JWT about the user) and a userinfo endpoint. Your Spring API is typically a resource server: it validates access tokens from the issuer (Keycloak, Auth0, Cognito) via its JWKS and maps scopes/roles to authorities.

</details>

## L3

### SEC-L3-001 · why
Why are URL-based rules in a SecurityFilterChain not enough?

<details><summary>Answer</summary>

They see paths, methods and roles, not data: they cannot know that order 42 belongs to bob, or that a manager may approve only their team's expenses. Object-level rules go into method security (`@PreAuthorize`/`@PostAuthorize`), domain checks, or queries scoped to the caller. Defence in depth also protects against a forgotten URL rule.

</details>

### SEC-L3-002 · recall
What does `@EnableMethodSecurity` do, and how does `@PreAuthorize` work?

<details><summary>Answer</summary>

It registers advisors that wrap beans with security annotations in AOP proxies. Before the method runs, `@PreAuthorize`'s SpEL expression is evaluated with the current `Authentication` (and the method arguments by name); false → `AccessDeniedException`, no authentication → `AuthenticationCredentialsNotFoundException`.

</details>

### SEC-L3-003 · bug
`deleteAll()` in a service loops over ids calling `this.delete(id)`, which is annotated `@PreAuthorize("hasRole('ADMIN')")`. `deleteAll` has no annotation. What happens when a normal user calls `deleteAll`?

<details><summary>Answer</summary>

The deletes run: self-invocation bypasses the proxy, so the `@PreAuthorize` on `delete` is never evaluated. Annotate the public entry point (`deleteAll`) too, or move the check to where every path passes through the proxy.

</details>

### SEC-L3-004 · bug
`@PostAuthorize("returnObject.owner == authentication.name") public Note archive(long id)` sets the note archived and returns it. Problem?

<details><summary>Answer</summary>

`@PostAuthorize` checks after the method ran: the archive already happened (and, inside a transaction that does not roll back on that exception path, may even be committed). Use post-checks only on side-effect-free reads; authorise state changes before acting (`@PreAuthorize` with a bean that loads the owner).

</details>

### SEC-L3-005 · recall
What is the root cause shared by SQL injection, XSS and command injection, and the general fix?

<details><summary>Answer</summary>

Untrusted data is concatenated into something an interpreter parses as code (SQL, HTML/JavaScript, shell). The fix is to keep data separate from code for that interpreter: bound parameters, context-aware output encoding, argument lists instead of shell strings, plus allowlist validation.

</details>

### SEC-L3-006 · recall
Stored vs reflected vs DOM-based XSS.

<details><summary>Answer</summary>

Stored: the payload is saved (a comment, a profile name) and executed in other users' browsers. Reflected: echoed back from the current request (a search term in the results page), delivered via a crafted link. DOM-based: client-side JavaScript writes untrusted data (location.hash, postMessage) into the page with innerHTML or similar, and the server may never see it.

</details>

### SEC-L3-007 · bug
A React component renders `<div dangerouslySetInnerHTML={{ __html: comment.body }} />`. Risk and fix?

<details><summary>Answer</summary>

Stored XSS: any HTML/JavaScript in a comment runs in other users' sessions (stealing tokens from localStorage, acting as them). React escapes by default when you render `{comment.body}`; use that. If rich text is needed, sanitise with an allowlist sanitiser (DOMPurify) and add a Content-Security-Policy.

</details>

### SEC-L3-008 · recall
What is a Content-Security-Policy?

<details><summary>Answer</summary>

A response header telling the browser which sources may provide scripts, styles, images, frames and connections, e.g. `default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'`. It blocks inline and injected scripts unless explicitly allowed (nonces or hashes), which limits the damage of XSS. It is a second layer, not a replacement for escaping.

</details>

### SEC-L3-009 · bug
`Path file = Path.of("/data/uploads/" + request.getParameter("name")); return Files.readAllBytes(file);` Attack and fix?

<details><summary>Answer</summary>

Path traversal: `name=../../etc/passwd` (or an absolute path) reads any file the process can read. Fix: resolve against the base directory, normalise, and verify `resolved.startsWith(base)` on Path objects (not string prefixes); better, store files under generated ids and keep the original names in the database.

</details>

### SEC-L3-010 · recall
What is an open redirect, and why does it matter?

<details><summary>Answer</summary>

An endpoint that redirects to a URL taken from the request (`/login?next=https://evil.example`). Attackers send links on your trusted domain that land on a phishing page, and redirect URIs in OAuth flows can leak tokens. Allow only local paths (starting with a single `/`) or an exact allowlist of hosts.

</details>

### SEC-L3-011 · recall
What is SSRF, and why is it especially dangerous in the cloud?

<details><summary>Answer</summary>

Server-side request forgery: the server fetches a URL supplied by the user (webhooks, image import, PDF rendering), so the attacker makes your server call internal addresses: admin services, databases' HTTP ports, or the cloud metadata endpoint `169.254.169.254`, which can hand out IAM credentials. Defend with allowlists, blocking private/link-local ranges after DNS resolution (and on redirects), IMDSv2, and egress network policies.

</details>

### SEC-L3-012 · bug
`objectInputStream.readObject()` is used on a cookie value. Why is this critical?

<details><summary>Answer</summary>

Java deserialization of untrusted data can execute code through "gadget chains" in classes on the classpath (Commons Collections and many others): remote code execution without any bug in your own code. Never deserialize untrusted Java serialization; use JSON into specific types, and with Jackson never enable default typing for untrusted input.

</details>

### SEC-L3-013 · scenario
Login is hit by credential stuffing: 50,000 attempts per hour from 3,000 IPs, each IP trying a few accounts. Per-IP rate limiting does not help. What do you do?

<details><summary>Answer</summary>

Layer defences: per-account throttling and progressive delays, bot detection or a CAPTCHA when risk signals appear (new device, failure spikes), checking passwords against breached lists, pushing users to MFA, global anomaly alerts on the failure rate, and making responses uniform (no enumeration). Temporary step-up rather than hard lockouts avoids letting attackers lock out real users.

</details>

### SEC-L3-014 · why
Why should a login throttle normalise the username before counting failures?

<details><summary>Answer</summary>

Otherwise each spelling (`Alice`, `alice `, `ALICE`, Unicode look-alikes) gets its own failure budget while authentication treats them as the same account, multiplying the attacker's guesses. Normalise exactly as the user lookup does (trim, case-fold, Unicode normalisation).

</details>

### SEC-L3-015 · scenario
An AWS key was committed to a public GitHub repository an hour ago. What do you do, in order?

<details><summary>Answer</summary>

1) Revoke or rotate the key immediately (bots scan GitHub within minutes). 2) Check the provider's logs (CloudTrail) for use of the key and contain any damage. 3) Remove it from the code and load it from a secret manager or environment. 4) Optionally purge history (it may already be cloned). 5) Add secret scanning and pre-commit hooks so it does not happen again, and write a short postmortem.

</details>

### SEC-L3-016 · recall
Where should an application's production secrets come from?

<details><summary>Answer</summary>

A secret manager (Vault, AWS Secrets Manager, GCP Secret Manager, Kubernetes Secrets with encryption at rest and RBAC) injected at runtime as environment variables or mounted files, ideally short-lived credentials (IAM roles, workload identity, dynamic database users). Never in source control, container images, logs or frontend bundles; separate secrets per environment.

</details>

### SEC-L3-017 · recall
Name the OWASP Top 10 (2021) categories.

<details><summary>Answer</summary>

A01 Broken Access Control, A02 Cryptographic Failures, A03 Injection (includes XSS), A04 Insecure Design, A05 Security Misconfiguration, A06 Vulnerable and Outdated Components, A07 Identification and Authentication Failures, A08 Software and Data Integrity Failures, A09 Security Logging and Monitoring Failures, A10 Server-Side Request Forgery.

</details>

### SEC-L3-018 · scenario
A security scanner reports that a transitive dependency has a critical CVE. How do you handle it?

<details><summary>Answer</summary>

Confirm it is really on the runtime classpath (`mvn dependency:tree`) and whether the vulnerable code path is reachable. Upgrade the direct dependency that brings it, or pin the fixed version in dependencyManagement (BOM order matters). Run the tests and redeploy. If no fix exists, mitigate (configuration, WAF rule, disabling the feature) and track it. Keep automated scanning (Dependabot/Renovate, OSV, dependency-check) and an SBOM so the next one is found in hours, not months.

</details>

### SEC-L3-019 · interview
"What would you check in a security review of a Spring Boot REST service?"

<details><summary>Answer</summary>

AuthN (token validation: issuer, audience, algorithm, expiry), authZ (deny by default, object-level checks, IDOR tests), input handling (bound SQL parameters, validation, file paths, redirects, SSRF on outbound URLs), output (no stack traces, problem details), CSRF/CORS configuration matching the auth method, security headers and TLS, actuator exposure, secrets handling, dependency scanning, rate limits on login and expensive endpoints, logging of auth events without sensitive data, and least-privilege database and container users.

</details>

### SEC-L3-020 · dsa
A login throttle keeps, per user, the timestamps of recent failures. Which data structure, and what is the cost per recorded failure?

<details><summary>Answer</summary>

A deque (queue) of timestamps per user in a hash map: drop timestamps older than the window from the front, push the new one on the back, compare the size with the limit. Amortised O(1) per failure, and memory bounded by maxFailures per user because the queue is cleared at lockout. It is the same sliding-window log as a rate limiter (track 05); a counter with fixed windows saves memory but is less exact.

</details>
