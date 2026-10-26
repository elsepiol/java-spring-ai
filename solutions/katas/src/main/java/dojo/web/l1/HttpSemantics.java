package dojo.web.l1;

import java.util.Set;

/** Reference solution (RFC 9110). */
public final class HttpSemantics {

	public enum Outcome {
		FOUND, CREATED, DELETED, ACCEPTED_FOR_LATER, CLIENT_COPY_IS_CURRENT, MOVED_FOREVER, INVALID_INPUT, NOT_LOGGED_IN,
		NOT_ALLOWED, NO_SUCH_RESOURCE, WRONG_METHOD, CONFLICT_WITH_CURRENT_STATE, STALE_VERSION, UNSUPPORTED_FORMAT,
		TOO_MANY, OUR_BUG, TEMPORARILY_UNAVAILABLE, UPSTREAM_TIMEOUT
	}

	private HttpSemantics() {
	}

	public static int status(Outcome outcome) {
		return switch (outcome) {
			case FOUND -> 200;
			case CREATED -> 201;
			case ACCEPTED_FOR_LATER -> 202;
			case DELETED -> 204;
			case MOVED_FOREVER -> 301;              // 308 if the method must be kept on redirect
			case CLIENT_COPY_IS_CURRENT -> 304;
			case INVALID_INPUT -> 400;              // some APIs use 422 for well-formed but invalid
			case NOT_LOGGED_IN -> 401;              // "unauthenticated", despite the name
			case NOT_ALLOWED -> 403;
			case NO_SUCH_RESOURCE -> 404;
			case WRONG_METHOD -> 405;               // with an Allow header
			case CONFLICT_WITH_CURRENT_STATE -> 409;
			case STALE_VERSION -> 412;
			case UNSUPPORTED_FORMAT -> 415;
			case TOO_MANY -> 429;                   // with Retry-After
			case OUR_BUG -> 500;
			case TEMPORARILY_UNAVAILABLE -> 503;    // with Retry-After
			case UPSTREAM_TIMEOUT -> 504;
		};
	}

	public static boolean isSafe(String method) {
		return Set.of("GET", "HEAD", "OPTIONS", "TRACE").contains(method);
	}

	public static boolean isIdempotent(String method) {
		return isSafe(method) || Set.of("PUT", "DELETE").contains(method); // POST and PATCH are not
	}

}
