package dojo.testing.l1;

import dojo.Kata;

/**
 * KATA testing L1 "SignupService" (tracks/10-testing/L1-basics.md, section 5).
 *
 * The tests are written with Mockito: read them to learn the behaviour, then implement it.
 * signUp(email, password):
 * - trims and lower-cases the email; blank email or a password shorter than 12 characters ->
 *   IllegalArgumentException, and NOTHING is called on the collaborators;
 * - email already taken -> EmailTakenException, no account created, no mail sent;
 * - otherwise creates the account with hasher.apply(password) (never the raw password) and sends
 *   ONE welcome mail to the normalised address, subject "Welcome!", the body containing the new id;
 * - if sending the mail fails, the signup still succeeds (the mail is not worth losing a customer);
 * - returns the new id.
 */
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
		return Kata.todo("tracks/10-testing/L1-basics.md section 5");
	}

}
