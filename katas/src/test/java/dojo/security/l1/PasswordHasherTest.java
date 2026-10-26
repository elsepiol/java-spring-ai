package dojo.security.l1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("security")
@Tag("L1")
class PasswordHasherTest {

	// Fewer iterations keep the tests fast; the production default is checked separately.
	private final PasswordHasher hasher = new PasswordHasher(10_000);

	@Test
	void usesTheSelfDescribingFormat() {
		String stored = hasher.hash("correct horse".toCharArray());
		String[] parts = stored.split("\\$");
		assertThat(parts).hasSize(4);
		assertThat(parts[0]).isEqualTo("pbkdf2-sha256");
		assertThat(parts[1]).isEqualTo("10000");
		assertThat(Base64.getDecoder().decode(parts[2])).hasSize(16);
		assertThat(Base64.getDecoder().decode(parts[3])).hasSize(32);
	}

	@Test
	void neverStoresThePassword() {
		assertThat(hasher.hash("correct horse".toCharArray())).doesNotContain("correct horse");
	}

	@Test
	void theSamePasswordHashesDifferentlyEveryTime() {
		String a = hasher.hash("hunter2".toCharArray());
		String b = hasher.hash("hunter2".toCharArray());
		assertThat(a).isNotEqualTo(b); // a random salt each time: no rainbow tables, no "same hash = same password"
	}

	@Test
	void verifiesTheRightPasswordOnly() {
		String stored = hasher.hash("hunter2".toCharArray());
		assertThat(hasher.verify("hunter2".toCharArray(), stored)).isTrue();
		assertThat(hasher.verify("hunter3".toCharArray(), stored)).isFalse();
		assertThat(hasher.verify("".toCharArray(), stored)).isFalse();
	}

	@Test
	void verifiesWithTheParametersStoredInTheHash() {
		String old = new PasswordHasher(1_000).hash("hunter2".toCharArray());
		assertThat(hasher.verify("hunter2".toCharArray(), old)).isTrue();
	}

	@Test
	void malformedValuesNeverVerify() {
		for (String bad : new String[] { null, "", "hunter2", "md5$1$abc$def", "pbkdf2-sha256$x$AAAA$AAAA",
				"pbkdf2-sha256$1000$!!!$AAAA", "pbkdf2-sha256$1000$AAAA" }) {
			assertThat(hasher.verify("hunter2".toCharArray(), bad)).as(String.valueOf(bad)).isFalse();
		}
	}

	@Test
	void oldHashesNeedRehashing() {
		String old = new PasswordHasher(1_000).hash("hunter2".toCharArray());
		String current = hasher.hash("hunter2".toCharArray());
		assertThat(hasher.needsRehash(old)).isTrue();
		assertThat(hasher.needsRehash(current)).isFalse();
	}

	@Test
	void theDefaultWorkFactorIsProductionGrade() {
		PasswordHasher production = new PasswordHasher();
		String stored = production.hash("hunter2".toCharArray());
		assertThat(stored).startsWith("pbkdf2-sha256$600000$"); // OWASP's 2023 minimum for PBKDF2-HMAC-SHA256
		assertThat(production.verify("hunter2".toCharArray(), stored)).isTrue();
	}

}
