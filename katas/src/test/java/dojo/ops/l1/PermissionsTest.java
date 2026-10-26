package dojo.ops.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dojo.ops.l1.Permissions.Access;

@Tag("ops")
@Tag("L1")
class PermissionsTest {

	@ParameterizedTest(name = "{0} = {1}")
	@CsvSource({ "rwxr-x---, 750", "rw-r--r--, 644", "rwxrwxrwx, 777", "---------, 000", "rw-------, 600", "r-x--x--x, 511" })
	void convertsBothWays(String symbolic, String octal) {
		int mode = Integer.parseInt(octal, 8);
		assertThat(Permissions.toOctal(symbolic)).isEqualTo(mode);
		assertThat(Permissions.toSymbolic(mode)).isEqualTo(symbolic);
	}

	@Test
	void rejectsMalformedInput() {
		assertThatIllegalArgumentException().isThrownBy(() -> Permissions.toOctal("rwx"));
		assertThatIllegalArgumentException().isThrownBy(() -> Permissions.toOctal("wrxr-x---"));
		assertThatIllegalArgumentException().isThrownBy(() -> Permissions.toOctal("rwxr-x--z"));
		assertThatIllegalArgumentException().isThrownBy(() -> Permissions.toSymbolic(01000));
		assertThatIllegalArgumentException().isThrownBy(() -> Permissions.toSymbolic(-1));
	}

	@Test
	void umaskClearsBitsItDoesNotSubtract() {
		assertThat(Permissions.applyUmask(0666, 0022)).isEqualTo(0644);
		assertThat(Permissions.applyUmask(0777, 0022)).isEqualTo(0755);
		assertThat(Permissions.applyUmask(0666, 0077)).isEqualTo(0600);
		assertThat(Permissions.applyUmask(0666, 0133)).isEqualTo(0644);
	}

	@Test
	void checksExactlyOneClass() {
		int mode = 0640; // rw-r-----, owner 1000, group 50
		assertThat(Permissions.canAccess(mode, 1000, 50, 1000, Set.of(1000), Access.WRITE)).isTrue();
		assertThat(Permissions.canAccess(mode, 1000, 50, 2000, Set.of(50), Access.READ)).isTrue();
		assertThat(Permissions.canAccess(mode, 1000, 50, 2000, Set.of(50), Access.WRITE)).isFalse();
		assertThat(Permissions.canAccess(mode, 1000, 50, 3000, Set.of(3000), Access.READ)).isFalse();
	}

	@Test
	void theOwnerClassWinsEvenWhenItIsMoreRestrictive() {
		int mode = 0044; // ---r--r--: everybody but the owner may read
		assertThat(Permissions.canAccess(mode, 1000, 50, 1000, Set.of(50), Access.READ)).isFalse();
		assertThat(Permissions.canAccess(mode, 1000, 50, 3000, Set.of(), Access.READ)).isTrue();
	}

	@Test
	void rootReadsAndWritesEverythingButExecutesOnlyExecutables() {
		assertThat(Permissions.canAccess(0000, 1000, 50, 0, Set.of(0), Access.READ)).isTrue();
		assertThat(Permissions.canAccess(0000, 1000, 50, 0, Set.of(0), Access.WRITE)).isTrue();
		assertThat(Permissions.canAccess(0644, 1000, 50, 0, Set.of(0), Access.EXECUTE)).isFalse();
		assertThat(Permissions.canAccess(0744, 1000, 50, 0, Set.of(0), Access.EXECUTE)).isTrue();
	}

}
