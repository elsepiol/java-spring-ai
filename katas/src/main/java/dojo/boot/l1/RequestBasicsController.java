package dojo.boot.l1;

import org.springframework.web.bind.annotation.RestController;

/**
 * KATA boot L1 "RequestBasicsController" (tracks/07-spring-boot-web/L1-basics.md, section 4: reading a request).
 *
 *   GET /api/hello?name=Ann          -> {"message":"Hello, Ann!"}    (name defaults to "world")
 *   GET /api/convert/{cents}?to=USD  -> {"cents":1000,"currency":"USD","amount":"10.00"}
 *                                       `to` is required: missing -> 400 (Spring does it for you)
 *   GET /api/whoami                  -> {"client":"<value of the X-Client header>","agent":"<User-Agent or 'unknown'>"}
 * Return records; Spring Boot turns them into JSON with Jackson.
 */
@RestController
public class RequestBasicsController {
}
