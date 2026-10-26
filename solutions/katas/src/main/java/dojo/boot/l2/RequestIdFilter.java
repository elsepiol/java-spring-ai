package dojo.boot.l2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reference solution. The header is set BEFORE the chain runs: once the body is committed, headers are frozen. */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

	private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String incoming = request.getHeader("X-Request-Id");
		String id = incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
		response.setHeader("X-Request-Id", id);
		MDC.put("requestId", id);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove("requestId");
		}
	}

}
