# Spring Boot web, L2: validation, errors, filters and interceptors, JSON

**Goal:** reject bad input before it reaches your logic, return consistent errors, add cross-cutting HTTP behaviour (request ids, logging, auth checks) in the right layer, and control JSON.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="boot & L2"`. Drill: `java quiz/Quiz.java --track spring-boot-web --level 2`.

---

## 1. Bean Validation

Declare the rules on the DTO; ask for validation with `@Valid`:

```java
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12) String password,
        @Min(13) int age,
        @Valid @NotNull Address address,          // cascade into nested objects
        @NotEmpty List<@Valid Item> items) {}      // and into list elements

@PostMapping("/api/users")
Registered register(@Valid @RequestBody RegisterRequest request) { ... }   // invalid -> MethodArgumentNotValidException, method never runs
```

- Needs `spring-boot-starter-validation` (Hibernate Validator).
- Common constraints: `@NotNull`, `@NotBlank` (strings), `@NotEmpty` (collections), `@Size`, `@Min`/`@Max`, `@Positive`, `@Email`, `@Pattern`, `@Past`/`@Future`, `@Digits`.
- **`@NotNull` vs primitives:** a missing JSON field becomes `0` for an `int`; use `Integer` with `@NotNull` when "absent" must be an error.
- Validate **path and query parameters** too: `@Min(1) @RequestParam int size` (Spring 6.1+ validates method parameters with constraints automatically for controllers).
- Validation is the *first* line: business rules that need the database (email already taken) are checked in the service, and constraints in the database are the last line (SQL track).
- Custom rules: write a constraint annotation + `ConstraintValidator`, or validate in the service and throw a domain exception.

---

## 2. One place for errors: `@RestControllerAdvice` and ProblemDetail

```java
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {          // already maps MVC's own exceptions to ProblemDetail

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", fieldErrors(ex));                  // {"email": "must be a well-formed email address"}
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail duplicate(DuplicateEmailException ex) {                // 409
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An account with this email already exists");
        p.setTitle("Email already registered");
        return p;
    }
}
```

- `ProblemDetail` is serialised as RFC 9457 `application/problem+json`.
- `ResponseEntityExceptionHandler` covers Spring MVC's exceptions consistently: malformed JSON (400), missing parameter (400), type mismatch (400), method not allowed (405), unsupported media type (415), not acceptable (406). Or set `spring.mvc.problemdetails.enabled=true` for the defaults without a class.
- Map **domain exceptions** (`ProductNotFoundException` → 404, `InsufficientStockException` → 409/422) in one place, so controllers stay clean and every endpoint answers the same way.
- **Unexpected exceptions** → 500 with a generic message and a trace id; log the full stack trace server-side. Never return exception messages from unknown exceptions (they can contain SQL, file paths, other users' data).
- `@ExceptionHandler` inside one controller handles only that controller; `@RestControllerAdvice` handles all (scope with `basePackages` or `assignableTypes`).

> **Kata `Registration`**: validation annotations, `@Valid`, problem details with field errors, 409 for duplicates, no internals leaked.

---

## 3. Filters vs interceptors vs aspects

| | Servlet `Filter` | `HandlerInterceptor` | AOP aspect |
|---|---|---|---|
| Runs | around **everything** reaching Tomcat, before Spring MVC | inside Spring MVC, around controller **handler** methods | around **bean methods** (any layer) |
| Knows | raw request/response | which handler (controller method) will run | method, arguments |
| Typical | request ids, logging, CORS, compression, **security** (Spring Security is a filter chain), rate limiting | per-handler checks (feature flags, simple auth by annotation), timing per route | transactions, caching, metrics on services |
| Sees static resources and errors from other filters | yes | no | no |

```java
@Component
public class RequestIdFilter extends OncePerRequestFilter {               // once per request, also on internal forwards
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ... {
        String id = valid(req.getHeader("X-Request-Id")) ? req.getHeader("X-Request-Id") : UUID.randomUUID().toString();
        res.setHeader("X-Request-Id", id);                                 // before the body is committed
        MDC.put("requestId", id);                                          // every log line of this request gets it
        try { chain.doFilter(req, res); } finally { MDC.remove("requestId"); }   // pooled threads!
    }
}
```

- Filter **order** matters (`@Order`, or `FilterRegistrationBean`): request id first, so even security failures are logged with it.
- Treat incoming ids as **untrusted input**: validate (length, characters) to prevent log injection and header injection.
- Headers must be set before the response is committed (the first body byte is written).

> **Kata `RequestIdFilter`**: validate or generate, MDC, cleanup in `finally`, response header.

---

## 4. JSON with Jackson

Spring Boot 4 uses **Jackson 3** (`tools.jackson.databind.json.JsonMapper`); Boot 3 used Jackson 2 (`com.fasterxml.jackson.databind.ObjectMapper`). Defaults to know:
- Records are serialised by component name; constructors used for deserialisation.
- Dates and times (`Instant`, `LocalDate`) as ISO-8601 strings.
- **Unknown properties are ignored** on input (Boot's default): good for evolving APIs.
- Configure with `spring.jackson.*` properties or a customizer bean, not by replacing the mapper.
- `@JsonProperty("total_cents")`, `@JsonIgnore`, `@JsonInclude(NON_NULL)` for per-field control; prefer separate DTOs over many annotations.
- Never serialise JPA entities directly: lazy collections, cycles (`StackOverflowError`), and accidental field exposure.

---

## 5. Other everyday MVC features

- **Pagination parameters**: `@RequestParam(defaultValue = "20") @Max(100) int size` and a cursor (track 05 L2).
- **CORS**: `@CrossOrigin` per controller, or a global `CorsConfigurationSource` (with Spring Security, configure it there).
- **Content negotiation**: `produces = "text/csv"` on a mapping; Spring picks by `Accept`.
- **File upload**: `@RequestParam MultipartFile file`, with `spring.servlet.multipart.max-file-size` limits.
- **Async/streaming**: `StreamingResponseBody`, `SseEmitter` for server-sent events.
- **HTTP clients**: `RestClient` (synchronous, fluent) with timeouts configured on the request factory; declarative HTTP interfaces (`@HttpExchange`).

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `Registration` (`RegistrationController` + `ApiErrors`) | Bean Validation, `@Valid`, `@RestControllerAdvice`, ProblemDetail, 400/409 |
| `RequestIdFilter` | `OncePerRequestFilter`, MDC, untrusted headers, cleanup |

```
cd katas && ./mvnw test -Dgroups="boot & L2"
```

## 7. Seen in the shop project

- Validation on request records (`CreateOrderRequest` with `@NotEmpty @Size(max = 50) List<@Valid Item>`, `@Min(1) @Max(100) int quantity`).
- `error/ApiExceptionHandler.java`: one advice mapping domain exceptions and validation to problem details with a trace id.
- `observability/TraceIdHeaderFilter.java` and `ratelimit/RateLimitFilter.java`: filters, with an explicit order relative to Spring Security.

## 8. Interview checklist

1. How does Bean Validation work in a Spring controller? What happens when validation fails?
2. `@NotNull` vs `@NotBlank` vs `@NotEmpty`. Why use `Integer` instead of `int` sometimes?
3. How do you validate nested objects and list elements?
4. `@ExceptionHandler` vs `@RestControllerAdvice`. What does `ResponseEntityExceptionHandler` give you?
5. What should a 500 response contain, and what must it never contain?
6. Filter vs interceptor vs aspect: where would you put request ids, authentication, transactions?
7. Why must the MDC be cleared in `finally`?
8. Why should a request id from a client be validated?
9. Why not return JPA entities from controllers?
