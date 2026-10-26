package dojo.security.l2.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Given: the endpoints to protect. Nothing to change here. */
@RestController
public class ApiController {

	@GetMapping("/api/public/ping")
	public Map<String, String> ping() {
		return Map.of("status", "up");
	}

	@GetMapping("/api/me")
	public Map<String, Object> me(Authentication auth) {
		// Spring Security 7 also adds a FACTOR_PASSWORD authority (how the user proved who they are).
		return Map.of("name", auth.getName(), "authorities", auth.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList());
	}

	@PostMapping("/api/notes")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, String> createNote(@RequestBody Map<String, String> body, Authentication auth) {
		return Map.of("owner", auth.getName(), "text", body.getOrDefault("text", ""));
	}

	@GetMapping("/api/admin/stats")
	public Map<String, Integer> stats() {
		return Map.of("users", 2);
	}

}
