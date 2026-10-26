package dojo.testing.l1;

/** Reference solution for the SignupService kata. */
public final class SignupService {

	public static final class EmailTakenException extends RuntimeException {

		public EmailTakenException(String email) {
			super("email taken: " + email);
		}

	}

	private final UserAccounts accounts;
	private final Mailer mailer;
	private final java.util.function.UnaryOperator<String> hasher;

	public SignupService(UserAccounts accounts, Mailer mailer, java.util.function.UnaryOperator<String> hasher) {
		this.accounts = accounts;
		this.mailer = mailer;
		this.hasher = hasher;
	}

	public long signUp(String email, String password) {
		String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
		if (normalized.isEmpty() || password == null || password.length() < 12) {
			throw new IllegalArgumentException("invalid email or password");
		}
		if (accounts.emailTaken(normalized)) {
			throw new EmailTakenException(normalized);
		}
		long id = accounts.create(normalized, hasher.apply(password));
		try {
			mailer.send(new Mailer.Email(normalized, "Welcome!", "Your account number is " + id + "."));
		}
		catch (RuntimeException e) {
			// a lost welcome mail must not undo the signup
		}
		return id;
	}

}
