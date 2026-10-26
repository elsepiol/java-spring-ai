# Track 07: Spring Boot web

Spring Boot on top of Spring core: from `main` to a JSON response, then validation and errors, then what Boot does automatically and how to run it in production.

| Level | Lesson | Katas (`katas/src/main/java/dojo/boot/`) |
|---|---|---|
| L1 Basic | [L1-basics.md](L1-basics.md): what Boot adds, the request path through Spring MVC, REST controllers, reading requests, MockMvc tests | `BookController` (CRUD, 201/204/404), `RequestBasicsController` |
| L2 Practical | [L2-practical.md](L2-practical.md): Bean Validation, problem details in one advice, filters vs interceptors vs aspects, Jackson 3, everyday MVC features | `RegistrationController` + `ApiErrors`, `RequestIdFilter` (MDC) |
| L3 Production | [L3-production.md](L3-production.md): auto-configuration internals and starters, validated configuration properties, actuator and health groups, threads, timeouts, graceful shutdown, observability | `ShippingProperties`, `GreeterAutoConfiguration` (your own auto-config), `PaymentProviderHealth` |

```
cd katas && ./mvnw test -Dgroups="boot & L1"
java quiz/Quiz.java --track spring-boot-web
```

`dojo.boot.KataBootApp` is the `@SpringBootApplication` for the katas; run it from your IDE to try the controllers with curl.

Question bank: [questions.md](questions.md) (60 questions).
