package dojo.ops.l3;

import java.util.List;
import java.util.function.Function;

import dojo.Kata;

/**
 * KATA ops L3 "Bisect" (tracks/14-git-linux-networking/L3-production.md, section 1).
 *
 * `git bisect` as code. {@code commits} is a linear history, oldest first; the first commit is
 * known GOOD and the last known BAD (do not test them). Find the first bad commit with as few
 * tests as possible: binary search, at most ceil(log2(n - 1)) tests when nothing is skipped.
 *
 * A test may answer SKIP (the commit does not build, `git bisect skip`). Then try another
 * untested commit between the current bounds, the one closest to the middle (ties: the older).
 * When only skipped commits remain between the last good and the first known bad commit, the
 * answer is ambiguous, as git says "the first bad commit could be any of": return the skipped
 * commits in that range plus the first known bad one, in history order. Otherwise return a
 * single index. Each commit is tested at most once.
 */
public final class Bisect {

	public enum Verdict {
		GOOD, BAD, SKIP
	}

	private Bisect() {
	}

	public static <T> List<Integer> firstBad(List<T> commits, Function<T, Verdict> test) {
		return Kata.todo("tracks/14-git-linux-networking/L3-production.md section 1");
	}

}
