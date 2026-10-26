package dojo.security.l1;

/** Reference solution for the AccessPolicy kata. */
public final class AccessPolicy {

	public Decision read(Caller caller, Document doc) {
		if (caller == null) {
			return Decision.UNAUTHENTICATED;
		}
		return doc != null && canRead(caller, doc) ? Decision.ALLOW : Decision.NOT_FOUND;
	}

	public Decision delete(Caller caller, Document doc) {
		Decision read = read(caller, doc);
		if (read != Decision.ALLOW) {
			return read;
		}
		return isOwner(caller, doc) || caller.has(Role.ADMIN) ? Decision.ALLOW : Decision.FORBIDDEN;
	}

	private static boolean canRead(Caller caller, Document doc) {
		return isOwner(caller, doc) || caller.has(Role.ADMIN) || (caller.has(Role.SUPPORT) && !doc.confidential());
	}

	private static boolean isOwner(Caller caller, Document doc) {
		return caller.name().equals(doc.owner());
	}

}
