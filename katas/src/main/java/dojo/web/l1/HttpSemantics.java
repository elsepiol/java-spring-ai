package dojo.web.l1;

import dojo.Kata;

/**
 * KATA web L1 "HttpSemantics" (tracks/05-web-http/L1-basics.md, sections 3-4).
 *
 * The status code is part of your API contract: clients, proxies, retry logic and monitoring all
 * act on it. Pick the right one for each outcome, and know which methods are safe and idempotent
 * (a retry or a crawler depends on it).
 */
public final class HttpSemantics {

	public enum Outcome {
		/** GET found the resource */
		FOUND,
		/** POST created a new resource (the response has a Location header) */
		CREATED,
		/** DELETE succeeded, nothing to return */
		DELETED,
		/** the request was accepted for processing later (e.g. a report is being generated) */
		ACCEPTED_FOR_LATER,
		/** conditional GET: the client's cached copy is still current */
		CLIENT_COPY_IS_CURRENT,
		/** the resource lives at a new URL for good */
		MOVED_FOREVER,
		/** the JSON body is malformed, or a field fails validation */
		INVALID_INPUT,
		/** no credentials, or the token expired */
		NOT_LOGGED_IN,
		/** logged in, but not allowed (a user calling an admin endpoint) */
		NOT_ALLOWED,
		/** no such resource */
		NO_SUCH_RESOURCE,
		/** the URL exists but not for this method (DELETE on a read-only resource) */
		WRONG_METHOD,
		/** state conflict: the email is already registered; the order is already cancelled */
		CONFLICT_WITH_CURRENT_STATE,
		/** If-Match did not match: someone else changed the resource first */
		STALE_VERSION,
		/** the client sent a format we do not accept (XML to a JSON-only API) */
		UNSUPPORTED_FORMAT,
		/** rate limit reached */
		TOO_MANY,
		/** our own bug: an unexpected exception */
		OUR_BUG,
		/** we are overloaded or in maintenance; try later */
		TEMPORARILY_UNAVAILABLE,
		/** a service we call did not answer in time */
		UPSTREAM_TIMEOUT
	}

	private HttpSemantics() {
	}

	public static int status(Outcome outcome) {
		return Kata.todo("tracks/05-web-http/L1-basics.md, section 3");
	}

	/** Safe = read-only by definition: no state change is requested. */
	public static boolean isSafe(String method) {
		return Kata.todo("tracks/05-web-http/L1-basics.md, section 4");
	}

	/** Idempotent = doing it twice has the same effect on the server as doing it once. */
	public static boolean isIdempotent(String method) {
		return Kata.todo("tracks/05-web-http/L1-basics.md, section 4");
	}

}
