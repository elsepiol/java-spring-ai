package dojo.testing.l1;

/** A collaborator that talks to the outside world (SMTP). The tests replace it with a mock. */
public interface Mailer {

	void send(Email email);

	record Email(String to, String subject, String body) {
	}

}
