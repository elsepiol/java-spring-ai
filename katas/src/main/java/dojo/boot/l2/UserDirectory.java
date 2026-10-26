package dojo.boot.l2;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Given. */
@Component
public class UserDirectory {

	public static class DuplicateEmailException extends RuntimeException {
		public DuplicateEmailException(String email) {
			super("Email already registered: " + email);
		}
	}

	private final Set<String> emails = ConcurrentHashMap.newKeySet();

	public long register(String email) {
		if (!emails.add(email.toLowerCase(Locale.ROOT))) {
			throw new DuplicateEmailException(email);
		}
		return emails.size();
	}

}
