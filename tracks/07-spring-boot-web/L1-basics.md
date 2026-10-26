# Spring Boot web, L1: from `main` to a JSON response

**Goal:** understand what Spring Boot adds on top of Spring (track 06), how an HTTP request reaches your controller method and comes back as JSON, and write REST endpoints with correct status codes, tested with MockMvc.

Prerequisite: tracks 05 (HTTP) and 06 (Spring core) L1. Katas: `cd katas && ./mvnw test -Dgroups="boot & L1"`. Drill: `java quiz/Quiz.java --track spring-boot-web --level 1`.

---

## 1. What Spring Boot is

Spring Boot = Spring + **opinionated defaults** so a production-ready service needs almost no configuration:
- **Starters**: one dependency per capability (`spring-boot-starter-webmvc`, `-data-jpa`, `-security`, `-actuator`) that brings compatible libraries, versions managed by Boot's BOM.
- **Auto-configuration**: `@Bean` definitions inside Boot's jars that apply *conditionally*: Tomcat and Spring MVC because the web starter is on the classpath; a `DataSource` because a JDBC driver and `spring.datasource.url` exist; Jackson's `ObjectMapper`/`JsonMapper` for JSON. Your own bean of the same type wins (L3 builds one).
- **An embedded server**: the app is a plain `java -jar app.jar`; Tomcat runs inside it (no war deployed into a server).
- **Externalised configuration** (`application.properties`, env vars, profiles) and **Actuator** (health, metrics) for operations.

```java
@SpringBootApplication            // = @Configuration + @EnableAutoConfiguration + @ComponentScan (this package and below)
public class ShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);   // create the context, start Tomcat
    }
}
```

`SpringApplication.run`: decides the application type (servlet web), loads configuration, creates the ApplicationContext, applies auto-configuration, creates beans, starts the embedded Tomcat on port 8080, publishes `ApplicationReadyEvent`. Run with `--debug` to print the **conditions report**: which auto-configurations applied and why the others did not.

---

## 2. The request's path through Spring MVC

```
HTTP request -> Tomcat (thread from its pool) -> Filters (security, request id, CORS...)
  -> DispatcherServlet (the front controller)
     -> HandlerMapping: which @RequestMapping method matches method + path?   (no match: 404; wrong method: 405)
     -> HandlerInterceptors (preHandle)
     -> argument resolvers: @PathVariable, @RequestParam, @RequestHeader, @RequestBody (JSON -> record via Jackson), @Valid
     -> YOUR controller method
     -> return value handling: object -> JSON via HttpMessageConverter (Jackson); ResponseEntity -> status + headers
     -> exception? -> @ExceptionHandler / @RestControllerAdvice -> error response
  <- interceptors (postHandle / afterCompletion) <- filters <- Tomcat <- HTTP response
```

One request = one Tomcat thread for its whole duration (blocking model; virtual threads optional). Your controller is a **singleton** called by many threads at once: keep it stateless.

---

## 3. A REST controller

```java
@RestController                                  // @Controller + @ResponseBody: return values are the body (JSON)
@RequestMapping("/api/books")
public class BookController {

    private final BookStore store;               // constructor injection
    public BookController(BookStore store) { this.store = store; }

    @GetMapping
    public List<Book> all() { return store.all(); }                                 // 200 + JSON array

    @GetMapping("/{id}")
    public ResponseEntity<Book> one(@PathVariable long id) {
        return ResponseEntity.of(store.find(id));                                  // 200, or 404 if empty
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody NewBook body) {
        Book book = store.add(body.title(), body.author(), body.priceCents());
        return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);   // 201 + Location
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return store.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
```

- Return a plain object for 200; `ResponseEntity` when you choose status and headers; `@ResponseStatus(HttpStatus.CREATED)` for a fixed status.
- Use **records** as request and response bodies (DTOs), not your JPA entities: the API contract must not change when the database schema does, and entities can leak fields (password hashes) or trigger lazy loading during serialisation.
- JSON field names follow the record components (`priceCents`).

> **Kata `BookController`**: the full CRUD with 200/201/204/404 and `Location`.

---

## 4. Reading the request

| Annotation | Reads | Missing |
|---|---|---|
| `@PathVariable long id` | `/api/books/{id}` | (path does not match) |
| `@RequestParam String to` / `@RequestParam(defaultValue = "world") String name` | `?to=USD` | **400** unless optional/defaulted |
| `@RequestHeader("X-Client") String client` | a header | 400 unless optional/defaulted |
| `@RequestBody NewBook body` | the JSON body, converted by Jackson | 400 (and 415 for a wrong Content-Type) |
| `@CookieValue`, `HttpServletRequest`, `Principal` | cookies, the raw request, the user | |

Type conversion is automatic: `/api/books/abc` for a `long` gives **400** (not 500). Binding by name needs the **`-parameters`** compiler flag (the Spring Boot parent POM sets it; without it, write `@PathVariable("id")`). The katas' POM sets it explicitly: it failed without it while this track was written.

> **Kata `RequestBasicsController`**: query parameters with defaults, path variables, required parameters, headers.

---

## 5. Testing controllers with MockMvc

```java
@WebMvcTest(BookController.class)      // a "slice": MVC infrastructure + this controller, no server, no database
@Import(BookStore.class)               // add what the controller needs (or @MockitoBean a fake)
class BookControllerTest {
    @Autowired MockMvc mvc;

    @Test void createsWith201() throws Exception {
        mvc.perform(post("/api/books").contentType(APPLICATION_JSON).content("{\"title\":\"DDD\",\"author\":\"Evans\",\"priceCents\":6100}"))
           .andExpect(status().isCreated())
           .andExpect(header().string("Location", "/api/books/1"))
           .andExpect(jsonPath("$.title").value("DDD"));
    }
}
```

MockMvc runs the whole MVC pipeline (filters, mapping, conversion, validation, exception handlers) in-process, in milliseconds. `@SpringBootTest` starts everything (optionally with a real port); use it for a few end-to-end tests. Track 10 covers testing strategy.

---

## 6. Katas for this level

| Kata | Practises |
|---|---|
| `BookController` | `@RestController`, mappings, `ResponseEntity`, 200/201/204/404, `Location` |
| `RequestBasicsController` | `@RequestParam`, `@PathVariable`, `@RequestHeader`, defaults, automatic 400s |

```
cd katas && ./mvnw test -Dgroups="boot & L1"
```

Run the app (`dojo.boot.KataBootApp` from your IDE) and call it with curl: `curl -i localhost:8080/api/books`.

## 7. Seen in the shop project

- `ShopApplication` with `@SpringBootApplication`; starters in `pom.xml`.
- Controllers returning records (`ProductResponse`, `OrderResponse`) with `ResponseEntity.created(...)` for POSTs: `ProductController`, `OrderController`.
- `@WebMvcTest` slices (`PingControllerTest`) and full `@SpringBootTest` API tests (`ProductApiTest`).

## 8. Interview checklist

1. What does Spring Boot add to Spring? What is a starter? What is auto-configuration?
2. What does `@SpringBootApplication` consist of?
3. What happens in `SpringApplication.run`?
4. Trace an HTTP request through Spring MVC to your method and back.
5. `@Controller` vs `@RestController`.
6. How do you return 201 with a Location header? 204? 404?
7. Why use DTOs (records) instead of entities in the API?
8. `@PathVariable` vs `@RequestParam` vs `@RequestBody`. What happens when a required parameter is missing, or `abc` is sent for a `long`?
9. `@WebMvcTest` vs `@SpringBootTest`.
