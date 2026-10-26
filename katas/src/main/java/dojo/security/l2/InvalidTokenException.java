package dojo.security.l2;

/** The token must be rejected: answer 401. The message is for logs, never for the client. */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}

}
