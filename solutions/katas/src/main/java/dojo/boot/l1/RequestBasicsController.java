package dojo.boot.l1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reference solution. */
@RestController
public class RequestBasicsController {

	public record Greeting(String message) {
	}

	public record Conversion(long cents, String currency, String amount) {
	}

	public record Client(String client, String agent) {
	}

	@GetMapping("/api/hello")
	public Greeting hello(@RequestParam(defaultValue = "world") String name) {
		return new Greeting("Hello, " + name + "!");
	}

	@GetMapping("/api/convert/{cents}")
	public Conversion convert(@PathVariable long cents, @RequestParam("to") String currency) {
		return new Conversion(cents, currency, String.format("%d.%02d", cents / 100, cents % 100));
	}

	@GetMapping("/api/whoami")
	public Client whoami(@RequestHeader("X-Client") String client,
			@RequestHeader(value = "User-Agent", defaultValue = "unknown") String agent) {
		return new Client(client, agent);
	}

}
