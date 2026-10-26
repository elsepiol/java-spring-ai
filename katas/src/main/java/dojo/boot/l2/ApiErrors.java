package dojo.boot.l2;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * KATA boot L2 "Registration" part 2 (tracks/07-spring-boot-web/L2-practical.md, section 2: errors).
 *
 * One place that turns exceptions into RFC 9457 problem details (Content-Type application/problem+json):
 * - invalid input -> 400, title "Invalid request", and a property "errors": {field: message}
 *   with one entry per invalid field (e.g. {"email": "must be a well-formed email address"});
 * - UserDirectory.DuplicateEmailException -> 409, title "Email already registered";
 * - nothing internal (exception class names, stack traces) in any body.
 * Hint: extend ResponseEntityExceptionHandler, and use ProblemDetail.
 */
@RestControllerAdvice
public class ApiErrors {
}
