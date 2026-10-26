package dojo.ops.l2;

import java.util.Optional;
import java.util.Set;

import dojo.Kata;

/**
 * KATA ops L2 "CommitGraph" (tracks/14-git-linux-networking/L2-practical.md, section 1).
 *
 * Git history is a directed acyclic graph: each commit points to its parents (0 for the root,
 * 2 for a merge). Implement what `git merge-base`, `git log a..b` and fast-forward checks do:
 * - add(id, parents...): parents must already exist (IllegalArgumentException otherwise);
 * - isAncestor(a, b): can b reach a by following parents? A commit is its own ancestor;
 * - range(a, b): the commits reachable from b but not from a (`git log a..b`: what merging b into
 *   a would bring in);
 * - canFastForward(target, source): target can simply move to source (no merge commit needed);
 * - mergeBase(a, b): the best common ancestor: a common ancestor that is not an ancestor of
 *   another common ancestor. If several exist (criss-cross merges), return the smallest id.
 *   Empty if the histories are unrelated.
 * Unknown ids -> IllegalArgumentException. Histories can be 200,000 commits long: no recursion.
 */
public final class CommitGraph {

	public void add(String id, String... parents) {
		Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 1");
	}

	public boolean isAncestor(String ancestor, String descendant) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 1");
	}

	public Set<String> range(String from, String to) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 1");
	}

	public boolean canFastForward(String target, String source) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 1");
	}

	public Optional<String> mergeBase(String a, String b) {
		return Kata.todo("tracks/14-git-linux-networking/L2-practical.md section 1");
	}

}
