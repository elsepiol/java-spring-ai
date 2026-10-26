# Git, Linux, networking, L2: merging and rebasing, team workflows, text processing, HTTP on the wire

**Goal:** merge and rebase with a clear picture of the commit graph, recover from mistakes, work in a team flow with pull requests, slice logs with the shell during an incident, and debug connections with DNS, ports and HTTP tools.

Prerequisite: L1. Katas: `cd katas && ./mvnw test -Dgroups="ops & L2"`. Drill: `java quiz/Quiz.java --track git-linux-networking --level 2`.

---

## 1. Merge, rebase and the commit graph

History is a **DAG**: commits point to parents. Two branches that diverged share a **merge base** (their best common ancestor).

```
      C---D   main                   merge:   C---D---M      (M has two parents: D and F)
     /                                       /       /
A---B---E---F feature                A---B---E---F

rebase feature onto main:   A---B---C---D---E'---F'     (E and F re-created with new ids)
```

- **Fast-forward**: if main is an ancestor of feature, "merging" just moves the main pointer: no merge commit (`git merge --ff-only`).
- **Merge commit**: keeps history as it happened; a three-way merge uses the merge base, both tips, and only conflicts where both sides changed the same lines.
- **Rebase**: replays your commits on top of another base, creating **new commits** (new ids). Linear history, but rewriting: **never rebase commits others have already based work on** (shared branches); rebase your own feature branch freely before merging. After rebasing a pushed branch, push with `--force-with-lease` (refuses if someone else pushed meanwhile).
- **Squash merge**: the whole branch becomes one commit on main (clean main history; the branch's commits are lost from main).
- `git log main..feature` = commits in feature not in main (what a merge would bring); `git merge-base main feature`.
- **Conflicts**: edit the file (markers `<<<<<<< ======= >>>>>>>`), `git add`, `git merge --continue` / `git rebase --continue`, or `--abort`. Resolve by understanding both changes, then run the tests.
- `git cherry-pick <id>` copies one commit (hotfixes to release branches). `git revert <id>` creates a commit that undoes another: the safe way to undo on shared branches.

> **Kata `CommitGraph`**: ancestry, `a..b` ranges, fast-forward checks and merge bases on a DAG, iteratively.

---

## 2. Recovering and team workflows

- `git reflog`: every position HEAD had, so "lost" commits after a bad reset or rebase can be found and restored (`git reset --hard HEAD@{2}` or `git branch rescue <id>`).
- `git reset --soft/--mixed/--hard <commit>`: move the branch pointer and keep changes staged / unstaged / discard them. `--hard` destroys uncommitted work.
- `git stash` (park work in progress), `git worktree add` (a second checkout of another branch).
- **Trunk-based development**: short-lived branches (hours to a day or two) merged into main behind feature flags, main always releasable, CI on every PR. **Git flow** (develop/release/hotfix branches) suits versioned products with scheduled releases more than continuously deployed services.
- **Pull requests**: small, focused, with a description of why; CI green; review for correctness, design and tests; protected main branch with required checks. Conventional commit messages (`feat:`, `fix:`) can drive changelogs and versions.
- **Never commit secrets**: if it happened, rotate first (Security track L3).

---

## 3. Text processing for incidents

```
grep -c " 500 " access.log                                       # how many 500s
grep " 5[0-9][0-9] " access.log | awk '{print $3}' | sort | uniq -c | sort -rn | head     # top failing paths
awk '{ sub("ms","",$5); print $5 }' access.log | sort -n | awk '{a[NR]=$1} END {print a[int(NR*0.95)]}'   # rough p95
tail -f app.log | grep --line-buffered -i "exception"
jq -r 'select(.level=="ERROR") | .message' app.json.log | sort | uniq -c | sort -rn   # JSON logs
sed -n '100,120p' file    cut -d, -f2 data.csv    xargs -n1    sort -u    comm -12 a b
```

- `grep` filters lines (`-i`, `-v`, `-E`, `-c`, `-A3 -B3` context), `awk` splits fields and computes, `sed` edits streams, `sort | uniq -c | sort -rn` counts, `jq` queries JSON.
- Collapse ids before counting (`/orders/42` → `/orders/{id}`), or every URL is its own group.
- Percentiles beat averages: one 30-second request hides in an average of 100 ms.
- On large files, filter first (`grep`), then sort; `LC_ALL=C sort` is much faster.

> **Kata `AccessLogStats`**: the same pipeline as code: parse, skip garbage, count, rank, percentiles.

---

## 4. Debugging connections

When "the service cannot reach X", go layer by layer:

| Question | Tool |
|---|---|
| Does the name resolve, to what? | `dig +short db.internal`, `getent hosts db.internal` (uses the system resolver, like Java) |
| Is the host reachable? | `ping` (may be blocked), `traceroute`/`mtr` |
| Is the port open and listening? | on the server `ss -tlnp`; from the client `nc -vz db.internal 5432` or `curl -v telnet://host:port` |
| Is it a firewall/security group? | connection **timeout** (packets dropped) vs connection **refused** (nothing listening / RST) |
| Does TLS work? | `openssl s_client -connect host:443 -servername host` (certificate chain, expiry, SNI) |
| What does HTTP say? | `curl -v`, `curl -sS -o /dev/null -w '%{http_code} %{time_total}\n' url` |

- **Refused** = the host answered "no one listens here" (wrong port, service down). **Timeout** = no answer at all (firewall, wrong IP, routing, overloaded backlog).
- DNS caching: the JVM caches lookups (`networkaddress.cache.ttl`; 30 s by default without a security manager); stale DNS after a failover is a classic.
- `localhost` inside a container is the container (track 11).
- HTTP keep-alive and connection pools: a pool to a backend that silently drops idle connections (load balancer idle timeout) gives occasional "connection reset" errors: set the pool's idle timeout below the LB's.

---

## 5. Katas for this level

| Kata | Practises |
|---|---|
| `CommitGraph` | DAG traversal, ancestry, ranges, merge base, iterative search |
| `AccessLogStats` | parsing, grouping, top-k, nearest-rank percentiles |

```
cd katas && ./mvnw test -Dgroups="ops & L2"
```

## 6. Seen in this repository

- The dojo itself is developed on a feature branch with CI on every push (`.github/workflows/ci.yml`), and `scripts/verify-katas.sh` is a `set -euo pipefail` script that parses surefire XML with `sed`, `awk` and pipes.

## 7. Interview checklist

1. Merge vs rebase vs squash. When is rebasing dangerous?
2. What is a fast-forward merge? A merge base?
3. How do you undo a commit on a shared branch? On your local branch?
4. You ran `git reset --hard` and lost commits. How do you get them back?
5. Trunk-based development vs Git flow.
6. Count the top 10 failing endpoints in a log with shell tools.
7. Connection refused vs connection timed out: what does each tell you?
8. How do you check a TLS certificate from the command line?
