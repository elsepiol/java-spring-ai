import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Run: java tracks/01-java-core/examples/HashCodeContract.java  (L1, section 6) */
public class HashCodeContract {

	/** equals without hashCode: breaks every hash-based collection. */
	static final class EmailOnlyEquals {
		final String email;

		EmailOnlyEquals(String email) {
			this.email = email;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof EmailOnlyEquals e && email.equals(e.email);
		}
		// hashCode NOT overridden: Object.hashCode differs per object
	}

	/** Both, consistently. */
	static final class Email {
		final String value;

		Email(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Email e && value.equals(e.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	record EmailRecord(String value) { // a record generates both for you
	}

	public static void main(String[] args) {
		Set<EmailOnlyEquals> broken = new HashSet<>();
		broken.add(new EmailOnlyEquals("ann@example.com"));
		broken.add(new EmailOnlyEquals("ann@example.com"));
		System.out.println("equals only: size " + broken.size() + ", contains? "
				+ broken.contains(new EmailOnlyEquals("ann@example.com"))); // 2, false (almost always)

		Set<Email> ok = new HashSet<>();
		ok.add(new Email("ann@example.com"));
		ok.add(new Email("ann@example.com"));
		System.out.println("both:        size " + ok.size() + ", contains? " + ok.contains(new Email("ann@example.com")));

		Set<EmailRecord> rec = new HashSet<>(Set.of(new EmailRecord("ann@example.com")));
		System.out.println("record:      contains? " + rec.contains(new EmailRecord("ann@example.com")));
	}
}
