package dojo.build.l1;

import dojo.Kata;

/**
 * KATA build L1 "MavenVersion" (tracks/03-build-tools/L1-basics.md, section 4).
 *
 * Maven compares versions to pick between candidates and to evaluate ranges, and its order is
 * NOT string order: "1.10" is newer than "1.9", and "2.0-SNAPSHOT" is OLDER than "2.0".
 * Implement this (simplified) version of Maven's rules:
 * - A version is dot-separated numbers, optionally followed by "-" and a qualifier:
 *   "1.2.3", "1.10", "2.0-SNAPSHOT", "1.0-alpha", "3.1-rc", "1.0-sp".
 * - Numbers compare numerically, segment by segment; missing segments count as 0, so
 *   "1" == "1.0" == "1.0.0" (equal, and equal hash codes).
 * - For equal numbers, the qualifier decides, case-insensitively, in this order:
 *     alpha < beta < milestone < rc < snapshot < (no qualifier: the release) < sp
 *   "a", "b", "m" and "cr" are short for alpha, beta, milestone and rc. A number glued to the
 *   qualifier ("rc1", "beta-2", "alpha3") orders versions with the same qualifier: rc1 < rc2.
 * - Any other qualifier is an IllegalArgumentException (real Maven sorts unknown ones lexically
 *   after the release; we keep it strict).
 * toString() returns the original text.
 */
public final class MavenVersion implements Comparable<MavenVersion> {

	public static MavenVersion parse(String text) {
		return Kata.todo("tracks/03-build-tools/L1-basics.md, section 4");
	}

	public boolean isSnapshot() {
		return Kata.todo("tracks/03-build-tools/L1-basics.md, section 4");
	}

	@Override
	public int compareTo(MavenVersion other) {
		return Kata.todo("tracks/03-build-tools/L1-basics.md, section 4");
	}

}
