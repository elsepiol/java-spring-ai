package dojo.boot.l2;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Reference solution. ResponseEntityExceptionHandler already maps Spring MVC's own exceptions
 * (bad JSON, missing parameters, 405, 415...) to ProblemDetail; we customise validation and add ours.
 */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
		problem.setTitle("Invalid request");
		Map<String, String> errors = new TreeMap<>();
		for (FieldError e : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(e.getField(), e.getDefaultMessage());
		}
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(UserDirectory.DuplicateEmailException.class)
	ProblemDetail duplicate(UserDirectory.DuplicateEmailException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An account with this email already exists");
		problem.setTitle("Email already registered");
		return problem;
	}

}
