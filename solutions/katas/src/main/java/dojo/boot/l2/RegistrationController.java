package dojo.boot.l2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reference solution: constraints on the record, @Valid on the parameter. */
@RestController
public class RegistrationController {

	public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 12) String password, @Min(13) int age) {
	}

	public record Registered(long id) {
	}

	private final UserDirectory users;

	public RegistrationController(UserDirectory users) {
		this.users = users;
	}

	@PostMapping("/api/users")
	@ResponseStatus(HttpStatus.CREATED)
	public Registered register(@Valid @RequestBody RegisterRequest request) { // invalid -> MethodArgumentNotValidException, before this runs
		return new Registered(users.register(request.email()));
	}

}
