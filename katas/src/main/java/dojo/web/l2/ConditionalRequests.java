package dojo.web.l2;

import dojo.Kata;
import java.util.Optional;

/**
 * KATA web L2 "ConditionalRequests" (tracks/05-web-http/L2-practical.md, section 3: ETags).
 *
 * An ETag is a version label for a representation: "v7" (strong) or W/"v7" (weak). Clients send
 * it back to (a) save bandwidth: GET with If-None-Match answers 304 Not Modified, empty body,
 * when nothing changed; (b) avoid lost updates: PUT with If-Match fails with 412 Precondition
 * Failed when someone else changed the resource first. Implement the server's decision
 * (RFC 9110, section 13, simplified):
 *
 * 1. If-Match present (any method): if the resource does not exist, or none of the listed tags
 *    matches the current ETag with STRONG comparison (weak tags never match), answer 412.
 *    "*" matches any existing resource.
 * 2. Else if If-None-Match present: if any listed tag matches with WEAK comparison (W/"x" equals
 *    "x"), or it is "*" and the resource exists: GET and HEAD answer 304, other methods 412.
 * 3. Otherwise PROCEED with the request normally.
 *
 * Header values are comma-separated lists of quoted tags, e.g.  "a", W/"b" , "c"
 * currentEtag is empty when the resource does not exist.
 */
public final class ConditionalRequests {

	public enum Decision { PROCEED, NOT_MODIFIED_304, PRECONDITION_FAILED_412 }

	private ConditionalRequests() {
	}

	public static Decision evaluate(String method, Optional<String> ifMatch, Optional<String> ifNoneMatch, Optional<String> currentEtag) {
		return Kata.todo("tracks/05-web-http/L2-practical.md, section 3");
	}

}
