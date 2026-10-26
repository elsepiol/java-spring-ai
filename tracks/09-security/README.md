# Track 09: Security

Application security for a full-stack Java developer: authentication vs authorization and password storage, then JWTs, Spring Security's filter chain and the browser-side rules (CSRF, CORS, cookies), then method security, the injection family, abuse protection, secrets and the OWASP Top 10.

| Level | Lesson | Katas (`katas/src/main/java/dojo/security/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): authn vs authz, 401/403/404, IDOR, password hashing, sessions vs tokens | `l1/AccessPolicy`, `l1/PasswordHasher` |
| L2 Practical | [L2-practical.md](L2-practical.md): Spring Security's architecture, JWT inside out, a filter chain for an API, CSRF, CORS, cookies and token storage | `l2/JwtCodec`, `l2/api/ApiSecurityConfig` (ApiSecurity) |
| L3 Production | [L3-production.md](L3-production.md): method security, injection (SQL, XSS, traversal, redirects, SSRF, deserialization), brute force and enumeration, secrets, production hygiene, OWASP Top 10 | `l3/notes` (MethodSecurity), `l3/InputGuards`, `l3/LoginThrottle` |

```
cd katas && ./mvnw test -Dgroups="security & L1"
java quiz/Quiz.java --track security
```

The katas use Spring Security directly, without Spring Boot's security auto-configuration, so the other web katas stay open and you see every piece you configure.

Question bank: [questions.md](questions.md) (60 questions).
