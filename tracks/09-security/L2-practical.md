# Security, L2: JWT, Spring Security, CSRF, CORS, cookies

**Goal:** know exactly what a JWT is and what verifying one means, configure Spring Security's filter chain for an API, and understand the browser-side attacks (CSRF) and controls (CORS, cookie flags) that decide your configuration.

Prerequisite: L1, Spring Boot web L1-L2. Katas: `cd katas && ./mvnw test -Dgroups="security & L2"`. Drill: `java quiz/Quiz.java --track security --level 2`.

---

## 1. How Spring Security works

Spring Security is a chain of **servlet filters** in front of `DispatcherServlet` (`DelegatingFilterProxy` → `FilterChainProxy` → your `SecurityFilterChain`s):

```
request -> ... -> SecurityContextHolderFilter        (load the SecurityContext, e.g. from the session)
               -> CsrfFilter                          (state-changing requests need a token)
               -> BasicAuthenticationFilter / BearerTokenAuthenticationFilter / UsernamePasswordAuthenticationFilter
                     -> AuthenticationManager -> AuthenticationProvider -> UserDetailsService + PasswordEncoder
               -> ExceptionTranslationFilter          (AuthenticationException -> 401/login; AccessDeniedException -> 403)
               -> AuthorizationFilter                 (the authorizeHttpRequests rules)
               -> DispatcherServlet -> your controller
```

- The result of authentication is an `Authentication` (principal + authorities) in the **`SecurityContextHolder`**, a ThreadLocal for the request's thread (track 02: ThreadLocals and thread pools).
- **Authorities** are strings; roles are authorities prefixed with `ROLE_` (`hasRole("ADMIN")` checks `ROLE_ADMIN`). Spring Security 7 also adds **factor authorities** (`FACTOR_PASSWORD`) recording *how* the user authenticated, for multi-factor rules (the kata's `/api/me` shows it).
- Rules are evaluated **in order; the first match wins**: specific matchers first, `anyRequest()` last.
- With Spring Boot's starter, a default chain secures everything with a generated password; defining your own `SecurityFilterChain` bean replaces it.

---

## 2. JSON Web Tokens

`header.payload.signature`, each part **base64url** without padding:

```
{"alg":"HS256","typ":"JWT"} . {"sub":"alice","roles":["USER"],"iat":1767261600,"exp":1767262500} . HMAC-SHA256(secret, part1 + "." + part2)
```

- **Signed, not encrypted**: anyone can decode the payload. Never put secrets or personal data you would not show the user in it. (JWE is the encrypted variant.)
- **HS256** (HMAC, one shared secret, at least 256 bits) vs **RS256/ES256** (the issuer signs with a private key, everyone verifies with the public key from a **JWKS** endpoint): use asymmetric keys when other services verify tokens they must not be able to mint.
- **Verifying** means all of: the algorithm is the one *you* expect (the classic attacks: `alg: none`, and switching RS256 to HS256 so the public key is used as an HMAC secret); the signature matches (constant-time); `exp` (and `nbf`) with a small clock skew; `iss` and `aud` are yours; required claims present.
- **Revocation is hard**: a stolen token works until it expires. Keep **access tokens short** (5-15 minutes) and use **refresh tokens** (long-lived, stored server-side, rotated on use, revocable) to get new ones. A denylist of token ids (`jti`) is possible but brings state back.
- In real code use a library (Nimbus, which Spring Security's resource server uses: `spring-boot-starter-oauth2-resource-server` + `spring.security.oauth2.resourceserver.jwt.issuer-uri`), never hand-rolled parsing. The kata hand-rolls it once so you know what the library does.
- **OAuth2 / OpenID Connect** in one line: an authorization server (Keycloak, Auth0, Okta, Cognito) logs the user in and issues tokens; your API is a *resource server* that only verifies them. The SPA uses the authorization-code flow with PKCE.

> **Kata `JwtCodec`**: issue and verify HS256 tokens; reject `none`, tampering, expiry, weak secrets.

---

## 3. A filter chain for a JSON API

```java
@Configuration
@EnableWebSecurity
class ApiSecurityConfig {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())                          // deny by default
            .httpBasic(Customizer.withDefaults())                      // or .oauth2ResourceServer(o -> o.jwt(...))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())                              // ONLY because no cookie authenticates (section 4)
            .build();
    }
}
```

Testing: `spring-security-test` gives `httpBasic(...)`, `jwt()`, `user(...)`, `csrf()` request post-processors for MockMvc and `@WithMockUser` for method-level tests. Test the **negative** cases (401, 403, other users' data); they are the ones that regress.

> **Kata `ApiSecurity`**: public, authenticated and admin routes, deny by default, Basic, stateless.

---

## 4. CSRF

**Cross-site request forgery**: you are logged in to `bank.com` (a session cookie). You visit `evil.com`, which contains `<form action="https://bank.com/transfer" method="POST">` and submits it with JavaScript. The browser **attaches bank.com's cookies automatically**; the bank sees an authenticated request.

Defences:
- **Synchronizer / CSRF token**: state-changing requests must carry a token the attacker's page cannot read (Spring Security's default for session-based apps; SPAs read it from an `XSRF-TOKEN` cookie and send it back in a header).
- **`SameSite` cookies**: `Lax` (the browser default in most browsers now) stops cookies on cross-site POSTs; `Strict` also on top-level navigations. A strong layer, not the only one.
- Never change state on `GET`.

When can you disable CSRF protection? When **no ambient credential** exists: the API authenticates with an `Authorization` header (Bearer/Basic) that the browser never adds by itself. If you store the JWT in a cookie, CSRF is back.

---

## 5. CORS and the same-origin policy

The **same-origin policy**: a page from `https://app.example.com` can send requests to other origins but cannot **read** the responses, unless the other origin allows it through **CORS** headers:

```
Origin: https://app.example.com                        (request)
Access-Control-Allow-Origin: https://app.example.com   (response: an exact origin, or *)
Access-Control-Allow-Credentials: true                 (cookies allowed; then the origin cannot be *)
```

Non-simple requests (JSON bodies, custom headers, PUT/DELETE) first send a **preflight** `OPTIONS`. CORS is enforced by browsers to protect *users*; it is **not** server-side access control (curl ignores it). Never reflect any `Origin` with credentials allowed. In Spring Security: `.cors(Customizer.withDefaults())` + a `CorsConfigurationSource` bean with an explicit origin list.

---

## 6. Cookies and where tokens live

Session cookie flags: **`HttpOnly`** (JavaScript cannot read it: XSS cannot steal it), **`Secure`** (HTTPS only), **`SameSite=Lax/Strict`**, a narrow `Path`, no `Domain` unless needed; the `__Host-` prefix enforces Secure + no Domain + Path=/.

Where should a SPA keep its token? `localStorage` is readable by any script on the page (one XSS = stolen tokens). An `HttpOnly` cookie cannot be read by scripts but needs CSRF protection. The common recommendation for SPAs today: a **backend-for-frontend** (BFF) holds the tokens and gives the browser an HttpOnly, SameSite session cookie.

Also: **regenerate the session id at login** (session fixation; Spring Security does it), invalidate it at logout, set idle and absolute timeouts.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `JwtCodec` | the JWT format, HMAC signatures, algorithm confusion, expiry, constant-time compare |
| `ApiSecurity` | `SecurityFilterChain`, `authorizeHttpRequests` ordering, Basic auth, stateless sessions, CSRF decision |

```
cd katas && ./mvnw test -Dgroups="security & L2"
```

## 8. Seen in the shop project

- `security/SecurityConfig`: CSRF disabled (bearer tokens only, with the reason in a comment), CORS first so preflights pass, stateless sessions, ordered `authorizeHttpRequests` rules (public GETs on products, admin-only writes and actuator metrics, `anyRequest().authenticated()` last), `oauth2ResourceServer().jwt()`.
- `JwtConfig`/`JwtService`: the app issues its own short-lived tokens and validates expiry, not-before and the issuer.
- Tests: `TestUsers` builds `jwt()` post-processors with `ROLE_USER`/`ROLE_ADMIN`; API tests cover 401 and 403.


## 9. Interview checklist

1. Walk through Spring Security's filter chain for one request.
2. Where is the current user stored? Why does that matter for `@Async` code?
3. `hasRole` vs `hasAuthority`.
4. What is inside a JWT? Is it encrypted? How do you verify one?
5. What is the `alg: none` attack? The RS256 → HS256 confusion?
6. How do you revoke JWTs? Access vs refresh tokens.
7. What is CSRF? When is it safe to disable CSRF protection?
8. What does CORS protect, and what does it not? What is a preflight?
9. HttpOnly, Secure, SameSite: what each prevents.
10. Where should a SPA store tokens?
