# Git, Linux, networking: question bank

Drill with `java quiz/Quiz.java --track git-linux-networking`.

## L1

### OPS-L1-001 · recall
What are Git's working tree, index and repository?

<details><summary>Answer</summary>

The working tree is the files you edit. The index (staging area) is the snapshot the next commit will contain: `git add` copies the current content of a file into it. The repository (`.git`) stores all commits and objects. `git diff` compares working tree vs index, `git diff --staged` index vs last commit.

</details>

### OPS-L1-002 · recall
What is a Git commit, technically?

<details><summary>Answer</summary>

An object containing a tree id (the complete snapshot of the project), the parent commit id(s), author, committer, timestamps and the message. Its id is the hash of that content, so changing anything (including a parent) gives a different id. It is a snapshot, not a diff.

</details>

### OPS-L1-003 · recall
What is a branch? What is HEAD?

<details><summary>Answer</summary>

A branch is a movable named pointer to a commit (a file containing a commit id) that advances when you commit on it. HEAD points to the current branch (or directly to a commit in "detached HEAD" state) and decides what the next commit's parent is.

</details>

### OPS-L1-004 · why
Why are branches in Git cheap compared to older version control systems?

<details><summary>Answer</summary>

Creating a branch writes a tiny reference file with one commit id; no files are copied, because commits are immutable snapshots shared by all branches. Switching branches only rewrites the working files that differ.

</details>

### OPS-L1-005 · recall
How does Git compute a blob's id?

<details><summary>Answer</summary>

SHA-1 of the header "blob <size in bytes>\0" followed by the file content (`git hash-object`). Identical content anywhere in history has the same id and is stored once (content addressing).

</details>

### OPS-L1-006 · why
Why is Git history tamper-evident?

<details><summary>Answer</summary>

Each commit id hashes its tree and its parents' ids, forming a hash chain: changing an old commit, file or message changes its id and therefore the ids of every descendant. Anyone with the latest id can detect rewritten history (and signed commits/tags add authenticity).

</details>

### OPS-L1-007 · recall
What makes a good commit?

<details><summary>Answer</summary>

One logical change, small enough to review, that builds and passes tests on its own, with a message stating what and why (a short summary line, a body for context). That makes review, revert, cherry-pick and bisect work well.

</details>

### OPS-L1-008 · recall
What does a process's exit code mean, and what does `set -euo pipefail` do in a bash script?

<details><summary>Answer</summary>

0 means success; any other value (1-255) failure, by convention specific codes. `set -e` exits the script when a command fails, `-u` treats unset variables as errors, `-o pipefail` makes a pipeline fail if any command in it fails (not only the last). Together they stop scripts from continuing after errors silently.

</details>

### OPS-L1-009 · recall
`>`, `>>`, `2>&1` and `|`: what does each do?

<details><summary>Answer</summary>

`>` redirects stdout to a file (overwrite), `>>` appends, `2>&1` sends stderr to wherever stdout currently goes, and `|` pipes stdout into the next command's stdin. `cmd > out.log 2>&1` captures both streams (order matters).

</details>

### OPS-L1-010 · recall
Explain the permission string `rwxr-x---` and its octal form.

<details><summary>Answer</summary>

Owner: read, write, execute (4+2+1 = 7); group: read and execute (5); others: nothing (0): mode 750. The owner can run and change it, group members can run it, nobody else can access it.

</details>

### OPS-L1-011 · recall
What is a umask, and what file permissions does umask 022 give?

<details><summary>Answer</summary>

A mask of permission bits removed from newly created files and directories: files start from 0666 and directories from 0777, minus the masked bits. With 022: files 0644 (rw-r--r--), directories 0755 (rwxr-xr-x). With 077: 0600 and 0700.

</details>

### OPS-L1-012 · predict
A file has mode `0044` (`---r--r--`) and you are its owner. Can you read it?

<details><summary>Answer</summary>

No. Unix checks only one class: as the owner, the owner bits apply (none), even though group and others may read. You can still `chmod` it, since you own it.

</details>

### OPS-L1-013 · recall
What do read, write and execute mean on a directory?

<details><summary>Answer</summary>

Read: list the names in it. Write: create, delete and rename entries (together with execute). Execute: enter it and access entries by name (traverse). Without execute you cannot open files inside even if you know their names.

</details>

### OPS-L1-014 · recall
SIGTERM vs SIGKILL vs SIGINT.

<details><summary>Answer</summary>

SIGTERM (15) asks a process to terminate; it can be handled (the JVM runs shutdown hooks; Spring shuts down gracefully). SIGKILL (9) kills immediately and cannot be caught: no cleanup. SIGINT (2) is Ctrl-C in a terminal, handled like SIGTERM by the JVM. Always send SIGTERM first.

</details>

### OPS-L1-015 · recall
What do `10.20.0.0/16` and `/24` mean, and how many addresses does each contain?

<details><summary>Answer</summary>

CIDR notation: the first 16 (or 24) bits are the network prefix; the rest identify hosts. /16 contains 2^16 = 65,536 addresses (10.20.0.0 to 10.20.255.255); /24 contains 256. Each additional prefix bit halves the block.

</details>

### OPS-L1-016 · recall
What are the RFC 1918 private IPv4 ranges?

<details><summary>Answer</summary>

10.0.0.0/8, 172.16.0.0/12 (172.16.0.0 to 172.31.255.255) and 192.168.0.0/16. They are not routed on the public internet; hosts reach the internet through NAT. 127.0.0.0/8 is loopback.

</details>

### OPS-L1-017 · recall
TCP vs UDP.

<details><summary>Answer</summary>

TCP: connection-oriented (3-way handshake), reliable, ordered byte stream with retransmission, flow and congestion control: HTTP/1.1 and 2, databases, SSH. UDP: connectionless datagrams without delivery or ordering guarantees, low overhead: DNS, video/voice, games, and QUIC (HTTP/3), which builds reliability itself.

</details>

### OPS-L1-018 · recall
What do the DNS record types A, AAAA, CNAME and TXT contain? What is a TTL?

<details><summary>Answer</summary>

A: an IPv4 address; AAAA: an IPv6 address; CNAME: an alias pointing to another name; TXT: arbitrary text (domain verification, SPF, DKIM). The TTL says how long resolvers may cache the answer; lower it ahead of a planned migration so changes propagate quickly.

</details>

### OPS-L1-019 · interview
"What happens when you type https://shop.example.com/cart into a browser and press Enter?"

<details><summary>Answer</summary>

The browser parses the URL and checks its caches; DNS resolution (browser/OS cache, then the resolver: root → .com → example.com name servers) returns an IP; a TCP connection to port 443 (3-way handshake); a TLS handshake (SNI, certificate verification, key exchange); an HTTP request (GET /cart with headers and cookies), possibly through a CDN and load balancer to an app server, which may call databases and caches; the HTTP response streams back; the browser parses HTML, fetches CSS/JS/images (often over the same HTTP/2 connection), runs JavaScript and renders the page.

</details>

### OPS-L1-020 · dsa
How do you test whether an IPv4 address lies inside a CIDR block with integer operations?

<details><summary>Answer</summary>

Convert both to 32-bit unsigned integers (a.b.c.d → a<<24 | b<<16 | c<<8 | d), build the mask from the prefix length (n leading ones: 0xFFFFFFFF << (32 - n), with /0 as a special case), and compare: (ip & mask) == (network & mask). O(1); a routing table does longest-prefix matching over many such blocks, typically with a trie.

</details>

## L2

### OPS-L2-001 · recall
Merge vs rebase?

<details><summary>Answer</summary>

Merge joins two histories with a merge commit (two parents), keeping what happened and not rewriting anything. Rebase replays your commits on top of another base, creating new commits with new ids, for a linear history. Both integrate the same changes; rebase rewrites the rebased commits.

</details>

### OPS-L2-002 · why
Why should you not rebase commits that others have already pulled?

<details><summary>Answer</summary>

Rebasing replaces them with new commits (new ids). Collaborators still have the old ones; when they pull, Git sees diverged histories, duplicates appear and conflicts repeat. Rebase only your own unshared work, or coordinate and use `--force-with-lease`.

</details>

### OPS-L2-003 · recall
What is a fast-forward merge?

<details><summary>Answer</summary>

When the target branch is an ancestor of the branch being merged (it has no commits of its own since they diverged), Git just moves the target pointer forward: no merge commit and no conflicts. `git merge --ff-only` refuses anything else.

</details>

### OPS-L2-004 · recall
What is the merge base, and why does a three-way merge need it?

<details><summary>Answer</summary>

The best common ancestor of the two branch tips (`git merge-base a b`). Comparing each tip with the base tells Git who changed what: lines changed on only one side are taken automatically; lines changed on both sides are conflicts. Without the base it could not tell an addition from a deletion.

</details>

### OPS-L2-005 · recall
What does `git log main..feature` show?

<details><summary>Answer</summary>

The commits reachable from feature but not from main: exactly what merging feature into main would bring in. `git log feature..main` shows the reverse, and `main...feature` the symmetric difference.

</details>

### OPS-L2-006 · scenario
A bad commit is already on the shared main branch. How do you undo it?

<details><summary>Answer</summary>

`git revert <id>`: a new commit that applies the inverse change, keeping history intact for everyone. Do not reset and force-push a shared branch. For a merge commit, `git revert -m 1 <merge>`.

</details>

### OPS-L2-007 · scenario
After a `git reset --hard` you lost three local commits. How do you recover them?

<details><summary>Answer</summary>

`git reflog` lists every position HEAD had, including the commit before the reset; `git reset --hard HEAD@{n}` or `git branch rescue <id>` brings it back. Unreachable commits survive until garbage collection (weeks by default). Uncommitted changes discarded by --hard are not in the reflog.

</details>

### OPS-L2-008 · recall
`git reset --soft`, `--mixed` and `--hard`?

<details><summary>Answer</summary>

All move the current branch to the given commit. `--soft` keeps the changes staged, `--mixed` (default) keeps them in the working tree unstaged, `--hard` discards them from index and working tree.

</details>

### OPS-L2-009 · recall
What does `--force-with-lease` protect against?

<details><summary>Answer</summary>

A plain `--force` push overwrites the remote branch even if someone pushed new commits meanwhile, deleting their work. `--force-with-lease` only overwrites if the remote is still where your last fetch saw it.

</details>

### OPS-L2-010 · recall
Trunk-based development vs Git flow.

<details><summary>Answer</summary>

Trunk-based: everyone integrates into main at least daily through short-lived branches, unfinished work hidden behind feature flags, main always deployable: suits continuous delivery. Git flow: long-lived develop and main branches plus feature, release and hotfix branches: suits scheduled, versioned releases but delays integration and causes big merges.

</details>

### OPS-L2-011 · recall
What is `git cherry-pick` used for?

<details><summary>Answer</summary>

Copying a specific commit (as a new commit) onto another branch, e.g. a hotfix from main onto a release branch. Overusing it duplicates changes and complicates later merges.

</details>

### OPS-L2-012 · design
What makes a pull request easy to review?

<details><summary>Answer</summary>

Small and focused (one concern, ideally under a few hundred lines), a description of why and how to test, tests included, CI green, refactorings separated from behaviour changes, self-reviewed first, and screenshots for UI changes. Reviewers focus on correctness, design, security and tests rather than style (linters handle style).

</details>

### OPS-L2-013 · scenario
Using shell tools, count the 10 most frequent paths of requests that returned 5xx in `access.log` (fields: time method path status latency).

<details><summary>Answer</summary>

`awk '$4 ~ /^5/ {print $3}' access.log | sort | uniq -c | sort -rn | head -10`. Normalise ids first if needed: `sed -E 's#/[0-9]+#/{id}#g'`. With JSON logs: `jq -r 'select(.status >= 500) | .path' | sort | uniq -c | sort -rn | head`.

</details>

### OPS-L2-014 · why
Why use percentiles rather than averages for latency?

<details><summary>Answer</summary>

Latency distributions are skewed: a few very slow requests (timeouts, GC pauses) barely move the mean but are what users notice, and users making many requests hit the tail often. p50 shows the typical case; p95/p99 the tail, which is what SLOs and alerts should use.

</details>

### OPS-L2-015 · recall
What do `grep`, `awk`, `sed`, `sort | uniq -c` and `jq` do?

<details><summary>Answer</summary>

grep filters lines matching a pattern; awk splits lines into fields and computes or prints them; sed edits streams (substitutions, line ranges); `sort | uniq -c` counts identical lines; jq queries and transforms JSON. Combined with pipes they answer most log questions during incidents.

</details>

### OPS-L2-016 · recall
"Connection refused" vs "connection timed out": what does each tell you?

<details><summary>Answer</summary>

Refused: the host answered with a reset (RST): it is reachable but nothing listens on that port (service down, wrong port, bound to localhost only). Timed out: no answer at all: the packets were dropped (firewall or security group, wrong IP or route, host down) or the server's backlog is overflowing.

</details>

### OPS-L2-017 · scenario
A service cannot connect to `db.internal:5432`. Debug it layer by layer.

<details><summary>Answer</summary>

DNS: `getent hosts db.internal` or `dig` (the right IP?). Routing and firewall: `nc -vz db.internal 5432`, and whether the result is refused or a timeout. On the database host: `ss -tlnp | grep 5432` (listening on the right interface, not only 127.0.0.1?), `pg_hba.conf`. Then TLS and credentials (application logs, `psql` from the client host), and connection limits (`max_connections`).

</details>

### OPS-L2-018 · recall
How do you inspect a server's TLS certificate from the command line?

<details><summary>Answer</summary>

`openssl s_client -connect host:443 -servername host` shows the presented chain and verification result; pipe into `openssl x509 -noout -dates -subject -ext subjectAltName` for validity dates and SANs. `curl -v https://host` also prints the certificate summary and errors.

</details>

### OPS-L2-019 · interview
"Your team's main branch is often broken. What would you change?"

<details><summary>Answer</summary>

Require pull requests with mandatory green CI before merge (branch protection), run the full test suite in CI and make it fast enough that people do not skip it, merge queues so merges are tested against the latest main, small and short-lived branches, feature flags instead of long-lived branches, revert broken commits immediately instead of fixing forward slowly, and fix or quarantine flaky tests so red means something.

</details>

### OPS-L2-020 · dsa
How do you compute the merge base of two commits in a DAG? What is the complexity?

<details><summary>Answer</summary>

Collect the ancestors of one commit (BFS/DFS over parent links), walk the other's ancestors to find common ones, and keep those that are not ancestors of another common ancestor (lowest common ancestors). Simple version: O(V + E) per traversal; Git speeds it up with commit-graph generation numbers so it can stop early. It is the LCA problem generalised from trees to DAGs, where several best answers can exist (criss-cross merges).

</details>

## L3

### OPS-L3-001 · recall
How does `git bisect` work, and how many steps does it need?

<details><summary>Answer</summary>

Mark a good and a bad commit; Git checks out the midpoint; you test and mark it good or bad; the range halves each time until the first bad commit remains: about log2(n) steps (10 for 1,000 commits). `git bisect run <script>` automates it: exit 0 = good, 125 = skip, other codes 1-127 = bad.

</details>

### OPS-L3-002 · scenario
A Java service suddenly uses 100% CPU. How do you find the code responsible?

<details><summary>Answer</summary>

`top -H -p <pid>` to see the hottest threads; convert the thread id to hex and find it in `jstack <pid>` / `jcmd <pid> Thread.print` (nid=0x...), taking several dumps a few seconds apart; or record with async-profiler or JFR for a flame graph. Also check GC (`jstat -gcutil`): a JVM close to running out of heap spends all its CPU in GC.

</details>

### OPS-L3-003 · scenario
A service fails with "Too many open files". Causes and investigation?

<details><summary>Answer</summary>

The process hit its file-descriptor limit (`ulimit -n`, systemd `LimitNOFILE`, container limits). Count with `ls /proc/<pid>/fd | wc -l` and group with `lsof -p <pid>`: usually leaked sockets (HTTP clients created per request, responses not closed), unclosed streams, or a connection storm. Fix the leak (try-with-resources, shared clients); raise the limit only if the usage is legitimate.

</details>

### OPS-L3-004 · scenario
`df` says the disk is full, but `du` finds far less data. Why?

<details><summary>Answer</summary>

Deleted files still held open by a process (typically a log file deleted by hand or rotation without reopening) keep using space until the process closes them. Find them with `lsof +L1` (or `/proc/<pid>/fd` entries marked deleted), then restart the process or truncate through the fd. Use proper log rotation (copytruncate or reopen signals).

</details>

### OPS-L3-005 · recall
What is JFR, and why is it suitable for production?

<details><summary>Answer</summary>

Java Flight Recorder, built into the JVM: records events (CPU samples, allocations, locks and contention, GC, I/O, exceptions) into a file with typically about 1% overhead. Start with `jcmd <pid> JFR.start duration=60s filename=rec.jfr` and analyse with JDK Mission Control. Low overhead and no agent to install make it safe in production.

</details>

### OPS-L3-006 · recall
Outline a TLS 1.3 handshake.

<details><summary>Answer</summary>

ClientHello (TLS versions, cipher suites, a key share, SNI with the hostname) → ServerHello (chosen suite, its key share) → both derive keys via ephemeral (EC)DHE → the server sends its certificate chain and a signature proving it owns the private key, encrypted → Finished messages → application data. One round trip; ephemeral keys give forward secrecy.

</details>

### OPS-L3-007 · recall
What is SNI, and why does it matter?

<details><summary>Answer</summary>

Server Name Indication: the client sends the requested hostname in the ClientHello, so a server or load balancer hosting many domains on one IP can choose the right certificate. Clients that do not send it (old ones, or connecting by IP) get a default certificate and hostname errors; `openssl s_client` needs `-servername`.

</details>

### OPS-L3-008 · recall
What three things does a TLS client verify about the server's certificate?

<details><summary>Answer</summary>

The chain of trust (leaf signed by intermediates up to a root in the trust store), the validity period (not before/not after), and that the hostname matches a Subject Alternative Name. Revocation (OCSP/CRL) is checked inconsistently. Any failure aborts the handshake.

</details>

### OPS-L3-009 · predict
Which hosts does a certificate with SAN `*.example.com` cover: `example.com`, `api.example.com`, `v2.api.example.com`?

<details><summary>Answer</summary>

Only `api.example.com`. A wildcard matches exactly one leftmost label: not the bare domain (add `example.com` as its own SAN) and not deeper subdomains (`*.api.example.com` would be needed).

</details>

### OPS-L3-010 · scenario
A Java client fails with `PKIX path building failed: unable to find valid certification path`. Likely causes?

<details><summary>Answer</summary>

The JVM cannot build a chain to a trusted root: the server does not send its intermediate certificate (browsers often cope, Java does not), the CA is private or corporate (a TLS-intercepting proxy) and not in the JVM trust store, or the JVM's cacerts is outdated. Check with `openssl s_client -showcerts`, fix the server chain or import the CA into the trust store. Never disable verification.

</details>

### OPS-L3-011 · design
How do you prevent certificate-expiry outages?

<details><summary>Answer</summary>

Automate issuance and renewal (ACME/Let's Encrypt, cert-manager in Kubernetes, cloud certificate managers) with short-lived certificates, monitor the expiry date of every endpoint (including internal and client certificates) with alerts weeks ahead, keep an inventory, and test the renewal path regularly.

</details>

### OPS-L3-012 · recall
What is mutual TLS (mTLS)?

<details><summary>Answer</summary>

Both sides present and verify certificates: the server authenticates the client too. Used for service-to-service authentication and zero-trust networks; service meshes (Istio, Linkerd) issue and rotate the certificates automatically.

</details>

### OPS-L3-013 · scenario
Clients see sporadic "Connection reset by peer" errors on requests through a load balancer, mostly after quiet periods. Why?

<details><summary>Answer</summary>

The client's connection pool keeps idle keep-alive connections longer than the load balancer's or NAT's idle timeout (e.g. ALB 60 s); the LB has already dropped them, and the next request on a stale connection is reset. Set the client's idle timeout below the LB's, validate or evict idle connections, and retry idempotent requests once.

</details>

### OPS-L3-014 · recall
What is TIME_WAIT, and when does it cause trouble?

<details><summary>Answer</summary>

The state a TCP socket stays in on the side that closed first, for 2×MSL (about 60 s on Linux), so delayed packets do not confuse a new connection with the same 4-tuple. A client that opens and closes a new connection per request to one destination can run out of ephemeral ports (~28,000 by default) at high rates. Fix with connection pooling and keep-alive.

</details>

### OPS-L3-015 · recall
L4 vs L7 load balancers?

<details><summary>Answer</summary>

L4 (transport) balances TCP/UDP connections by IP and port without understanding the protocol: fast, protocol-agnostic (AWS NLB). L7 (application) understands HTTP: routes by host and path, terminates TLS, adds headers, retries, does health checks and sticky sessions (ALB, Nginx, Envoy, Kubernetes ingress controllers).

</details>

### OPS-L3-016 · bug
An app reads the client's IP from `X-Forwarded-For` for rate limiting, trusting the header from any source. Problem?

<details><summary>Answer</summary>

Clients can send their own `X-Forwarded-For` and spoof any IP, bypassing rate limits or IP allowlists. Only trust forwarded headers added by your own proxies (take the address added by the last trusted hop; Spring's `server.forward-headers-strategy` with proxy configuration, Tomcat's RemoteIpValve with internal proxies).

</details>

### OPS-L3-017 · recall
What do HTTP/2 and HTTP/3 change compared to HTTP/1.1?

<details><summary>Answer</summary>

HTTP/2: binary framing, many concurrent streams over one TCP connection (multiplexing), header compression (HPACK), but packet loss still blocks all streams (TCP head-of-line blocking). HTTP/3: HTTP over QUIC on UDP, with per-stream loss recovery, faster handshakes (TLS 1.3 built in) and connection migration across networks.

</details>

### OPS-L3-018 · scenario
After a database failover, the application keeps trying to connect to the old primary's IP for minutes. Why?

<details><summary>Answer</summary>

DNS caching: the JVM (networkaddress.cache.ttl), the OS resolver, or the connection pool holding long-lived connections keep the old address. Use short DNS TTLs on failover records, configure the JVM DNS TTL, set a maximum connection lifetime in the pool so connections are re-established, and use drivers or proxies that understand failover.

</details>

### OPS-L3-019 · interview
"Tell me how you would approach a production incident where the API latency suddenly doubled."

<details><summary>Answer</summary>

Mitigate first: check recent deploys or config changes and roll back if correlated; scale out if saturated. Then narrow down: which endpoints and percentiles (dashboards), when it started, which dependency spans grew (traces), resource saturation (CPU, GC, pool waits, thread pools), database (slow queries, locks, `pg_stat_activity`), and network (DNS, TLS, retries). Form one hypothesis at a time, verify, fix, then write a blameless postmortem with follow-ups such as alerts and tests.

</details>

### OPS-L3-020 · dsa
Bisect over 1,000 commits where 10% of commits are untestable (skip). Roughly how many tests do you need, and why does skipping not ruin the logarithmic bound?

<details><summary>Answer</summary>

About log2(1000) ≈ 10 successful tests plus the skipped attempts: each skip just tries a neighbour near the middle, so the split stays close to half and each successful test still halves the range. With 10% skips, expect about 1-2 extra tests. Only when skipped commits sit at the boundary does the answer become a small set of candidates.

</details>
