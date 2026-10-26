package dojo.boot.l2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * KATA boot L2 "RequestIdFilter" (tracks/07-spring-boot-web/L2-practical.md, section 3: filters).
 *
 * Every request gets an id that appears in every log line written while handling it and in the
 * response, so a user's "error reference" leads straight to the logs:
 * - use the incoming X-Request-Id header if it is 1-64 characters of [A-Za-z0-9-]; otherwise (missing,
 *   too long, or anything else: it is user input) generate a random UUID;
 * - put it in SLF4J's MDC under "requestId" while the rest of the chain runs, and REMOVE it
 *   afterwards, also when the request fails (threads are pooled: L3 of the concurrency track);
 * - send it back in the X-Request-Id response header.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		chain.doFilter(request, response);
	}

}
