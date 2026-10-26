package dojo.boot.l2;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * KATA boot L2 "Registration" part 1 (tracks/07-spring-boot-web/L2-practical.md, sections 1-2).
 *
 * POST /api/users {"email": ..., "password": ..., "age": ...} -> 201 {"id": n}
 * The input must be validated BEFORE any code runs, with Bean Validation annotations:
 *   email: required, a valid email address; password: at least 12 characters; age: at least 13.
 * Errors are handled in ApiErrors (part 2). Right now everything is accepted.
 */
@RestController
public class RegistrationController {

	public record RegisterRequest(String email, String password, int age) {
	}

	public record Registered(long id) {
	}

	private final UserDirectory users;

	public RegistrationController(UserDirectory users) {
		this.users = users;
	}

	@PostMapping("/api/users")
	@ResponseStatus(HttpStatus.CREATED)
	public Registered register(@RequestBody RegisterRequest request) {
		return new Registered(users.register(request.email()));
	}

}
