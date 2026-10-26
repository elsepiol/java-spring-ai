package dojo;

/** Every unsolved kata calls this, so its tests fail with a pointer to the lesson. */
public final class Kata {

	private Kata() {
	}

	public static <T> T todo(String where) {
		throw new UnsupportedOperationException("TODO: this kata is not done yet. Read " + where);
	}

}
