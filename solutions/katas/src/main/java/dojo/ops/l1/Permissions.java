package dojo.ops.l1;

/** Reference solution for the Permissions kata. */
public final class Permissions {

	public enum Access {
		READ, WRITE, EXECUTE
	}

	private static final String LETTERS = "rwx";

	private Permissions() {
	}

	public static int toOctal(String symbolic) {
		if (symbolic == null || symbolic.length() != 9) {
			throw new IllegalArgumentException("need 9 characters: " + symbolic);
		}
		int mode = 0;
		for (int i = 0; i < 9; i++) {
			char c = symbolic.charAt(i);
			char letter = LETTERS.charAt(i % 3);
			mode <<= 1;
			if (c == letter) {
				mode |= 1;
			}
			else if (c != '-') {
				throw new IllegalArgumentException("unexpected '" + c + "' at " + i);
			}
		}
		return mode;
	}

	public static String toSymbolic(int mode) {
		if (mode < 0 || mode > 0777) {
			throw new IllegalArgumentException("mode out of range");
		}
		StringBuilder out = new StringBuilder(9);
		for (int i = 8; i >= 0; i--) {
			out.append((mode & (1 << i)) != 0 ? LETTERS.charAt((8 - i) % 3) : '-');
		}
		return out.toString();
	}

	public static int applyUmask(int requested, int umask) {
		return requested & ~umask & 0777;
	}

	public static boolean canAccess(int mode, int ownerUid, int groupGid, int uid, java.util.Set<Integer> gids, Access access) {
		int bit = switch (access) {
			case READ -> 4;
			case WRITE -> 2;
			case EXECUTE -> 1;
		};
		if (uid == 0) {
			return access != Access.EXECUTE || (mode & 0111) != 0;
		}
		int shift = uid == ownerUid ? 6 : gids.contains(groupGid) ? 3 : 0;
		return ((mode >> shift) & bit) != 0;
	}

}
