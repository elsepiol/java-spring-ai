package dojo.ops.l1;

import dojo.Kata;

/**
 * KATA ops L1 "Permissions" (tracks/14-git-linux-networking/L1-basics.md, section 4).
 *
 * Unix permissions: three groups (user, group, others) of three bits (read 4, write 2, execute 1).
 * - toOctal("rwxr-x---") = 0750 (an int; written in octal in Java as 0750). Exactly 9 characters
 *   of r/w/x/- in the right positions, otherwise IllegalArgumentException.
 * - toSymbolic(0640) = "rw-r-----"; values outside 0..0777 -> IllegalArgumentException.
 * - applyUmask: a new file is created with mode 0666 (directories 0777) minus the umask bits:
 *   applyUmask(0666, 0022) = 0644. (It is a bit mask, not a subtraction: 0666 with umask 0077 is
 *   0600, and 0666 with umask 0133 is 0644.)
 * - canAccess: may a user with the given uid/gids read/write/execute a file with this mode, owner
 *   uid and group gid? Unix checks ONE class: owner if uid matches, else group if any gid
 *   matches, else others. (So an owner WITHOUT read permission cannot read even if "others" can.)
 *   uid 0 (root) may read and write anything, and execute if ANY execute bit is set.
 */
public final class Permissions {

	public enum Access {
		READ, WRITE, EXECUTE
	}

	private Permissions() {
	}

	public static int toOctal(String symbolic) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 4");
	}

	public static String toSymbolic(int mode) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 4");
	}

	public static int applyUmask(int requested, int umask) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 4");
	}

	public static boolean canAccess(int mode, int ownerUid, int groupGid, int uid, java.util.Set<Integer> gids, Access access) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 4");
	}

}
