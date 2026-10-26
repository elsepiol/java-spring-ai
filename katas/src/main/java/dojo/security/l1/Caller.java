package dojo.security.l1;

import java.util.Set;

/** The authenticated caller: who they are (authentication) and what roles they have. */
public record Caller(String name, Set<Role> roles) {

	public boolean has(Role role) {
		return roles.contains(role);
	}

}
