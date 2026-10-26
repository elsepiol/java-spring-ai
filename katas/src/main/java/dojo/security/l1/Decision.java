package dojo.security.l1;

/** What the HTTP layer will answer: 200, 401, 403 or 404. */
public enum Decision {
	ALLOW, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND
}
