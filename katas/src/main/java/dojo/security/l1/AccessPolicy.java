package dojo.security.l1;

import dojo.Kata;

/**
 * KATA security L1 "AccessPolicy" (tracks/09-security/L1-basics.md, sections 1-2).
 *
 * Decide access to documents. {@code caller} is null when nobody is logged in; {@code doc} is
 * null when no document has the requested id.
 *
 * Reading: nobody logged in -> UNAUTHENTICATED (401). The owner and ADMINs can read any document;
 * SUPPORT can read documents that are not confidential. Anyone else must not even learn that the
 * document exists: answer NOT_FOUND, exactly as for a missing id (otherwise ids can be probed:
 * insecure direct object references).
 *
 * Deleting: only the owner and ADMINs. Someone who may READ the document but not delete it gets
 * FORBIDDEN (403: they know it exists); someone who may not read it gets NOT_FOUND.
 */
public final class AccessPolicy {

	public Decision read(Caller caller, Document doc) {
		return Kata.todo("tracks/09-security/L1-basics.md sections 1-2");
	}

	public Decision delete(Caller caller, Document doc) {
		return Kata.todo("tracks/09-security/L1-basics.md sections 1-2");
	}

}
