# Git, Linux, networking, L1: the everyday foundations

**Goal:** use Git with a correct mental model (snapshots and pointers, not diffs), move around a Linux shell confidently (files, permissions, processes, pipes), and understand the networking every web request depends on: IP addresses, ports, DNS, TCP and HTTP.

Katas: `cd katas && ./mvnw test -Dgroups="ops & L1"`. Drill: `java quiz/Quiz.java --track git-linux-networking --level 1`.

---

## 1. Git: the daily workflow

```
git clone <url>                  git status / git diff / git diff --staged
git switch -c feature/cart       git add -p            (stage hunks, review as you go)
git commit -m "Add cart total"   git log --oneline --graph --all
git fetch / git pull --rebase    git push -u origin feature/cart
```

Three places: the **working tree** (your files), the **index/staging area** (what the next commit will contain), the **repository** (commits). `git restore <file>` discards working-tree changes; `git restore --staged` unstages; `git commit --amend` rewrites the last (unpushed) commit.

Good commits: one logical change, builds and passes tests, a message that says *why* ("Fix rounding of discounted totals" + a body), not "fix". `.gitignore` for build output, IDE files and `.env`.

---

## 2. Git's model: objects and pointers

Git is a **content-addressed object store**:
- **blob**: file contents (id = SHA-1 of `"blob <size>\0" + content`; identical files are stored once);
- **tree**: a directory listing (names → blob/tree ids, modes);
- **commit**: a tree id (the full snapshot), parent commit id(s), author, committer, message;
- **tag**: an annotated, named pointer to an object.

Because each commit's id depends on its tree and parents, **history is tamper-evident**: changing anything old changes every id after it (a hash chain). A commit is a **snapshot**, not a diff; diffs are computed when you ask.

**Refs** are just names for commit ids: a **branch** is a movable pointer (`.git/refs/heads/main` contains an id) that advances when you commit; **HEAD** points to the current branch (or directly to a commit: "detached HEAD"). Creating a branch is writing 41 bytes, which is why branches are cheap.

Ids are abbreviated to the shortest unique prefix (`git log --oneline`). Git is moving to SHA-256 repositories, but SHA-1 ids are still the default.

> **Kata `GitObjects`**: compute `git hash-object` ids and shortest unique prefixes.

---

## 3. The shell

```
pwd  ls -la  cd  mkdir -p  cp -r  mv  rm -r  touch  ln -s target link
cat  less  head -n 20  tail -f app.log  wc -l  grep -rn "ERROR" logs/  find . -name "*.java" -mtime -1
cmd > out.txt      (stdout to a file)       cmd 2>&1 | tee all.log   (stderr too, and to the screen)
cmd1 | cmd2        (pipe)                   cmd1 && cmd2  (only if cmd1 succeeded)   cmd1 || cmd2
export JAVA_HOME=/opt/jdk   echo $PATH   which java   man grep   history   Ctrl-R
```

- Everything is a file (including devices and `/proc`); text streams connect small tools.
- **Exit codes**: 0 is success, anything else failure (`echo $?`). Scripts start with `set -euo pipefail` so errors stop them.
- **Environment variables** are inherited by child processes (that is how containers and Spring Boot get configuration).
- Quoting: `"$var"` (expand, keep spaces together) vs `'literal'`.

---

## 4. Users and permissions

`ls -l` → `-rwxr-x--- 1 app staff 4096 app.sh`: type, then three permission triplets for **user (owner), group, others**: read (4), write (2), execute (1). `chmod 750 app.sh` = `rwxr-x---`; `chmod u+x`, `chown app:staff`.

- For directories: read = list names, write = create/delete entries, execute = enter/traverse.
- Only **one** class applies: the owner's bits if you are the owner (even if "others" has more), else the group's, else others'.
- New files get `0666 & ~umask` (directories `0777 & ~umask`); umask 022 gives 644/755.
- **root** (uid 0) bypasses read/write checks. `sudo` runs one command as root; least privilege means services run as their own unprivileged user (and containers as non-root, track 11).
- Private keys must be `600` (`ssh` refuses group/other-readable keys).

> **Kata `Permissions`**: symbolic ↔ octal, umask, and the owner/group/others decision.

---

## 5. Processes

```
ps aux | grep java      top / htop      pgrep -f shop.jar      kill <pid>  (SIGTERM)      kill -9 <pid> (SIGKILL)
jobs  bg  fg  Ctrl-Z    nohup cmd &     systemctl status shop  journalctl -u shop -f
```

- Every process has a PID, a parent, a user, an environment, open file descriptors (stdin 0, stdout 1, stderr 2).
- **Signals**: SIGTERM (15, "please stop": the JVM runs shutdown hooks), SIGKILL (9, cannot be caught), SIGINT (Ctrl-C), SIGHUP. Always try SIGTERM first.
- Services are managed by **systemd** on most servers (units, restart policies, logs in the journal), and by the container runtime/Kubernetes in containers.

---

## 6. Networking basics

- **IP address**: identifies a host interface. IPv4 is 32 bits (`192.168.1.20`); **CIDR** notation `10.20.0.0/16` describes a network: the first 16 bits are the network, the rest hosts (65,536 addresses). Private ranges (RFC 1918): `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`; `127.0.0.0/8` is loopback. IPv6 is 128 bits (`2001:db8::1`).
- **Port**: identifies a process's socket on a host (16 bits; < 1024 privileged). A TCP connection is the 4-tuple (src IP, src port, dst IP, dst port).
- **DNS** resolves names to addresses: A/AAAA (address), CNAME (alias), MX (mail), TXT, NS; answers are cached for their **TTL**. `dig shop.example.com`, `nslookup`, `/etc/hosts`.
- **TCP**: reliable, ordered byte stream; a 3-way handshake (SYN, SYN-ACK, ACK) before data; retransmissions, flow and congestion control. **UDP**: datagrams, no connection, no guarantees (DNS, video, QUIC).
- **TLS** on top of TCP encrypts and authenticates the server (certificates, L3). **HTTP** on top of that (Web track).
- **NAT**: private addresses share a public one (your home router, cloud NAT gateways).
- Tools: `ping` (ICMP reachability), `curl -v https://...` (the whole exchange), `ss -tlnp` (listening sockets), `nc -vz host 5432` (is the port open?), `traceroute`/`mtr` (path), `ip addr`.

> **Kata `Cidr`**: parse blocks, sizes, membership, overlaps, private ranges.

---

## 7. Katas for this level

| Kata | Practises |
|---|---|
| `GitObjects` | content addressing, SHA-1 blob ids, abbreviated ids |
| `Permissions` | rwx bits, octal, umask, which class applies, root |
| `Cidr` | IPv4 as 32-bit numbers, masks, subnets, RFC 1918 |

```
cd katas && ./mvnw test -Dgroups="ops & L1"
```

## 8. Interview checklist

1. Working tree, index, repository. What does `git add` really do?
2. What is a commit, technically? A branch? HEAD?
3. Why are Git branches cheap?
4. What is an exit code? What does `set -euo pipefail` do?
5. Explain `rwxr-x---`. What is a umask?
6. SIGTERM vs SIGKILL.
7. What does `/16` mean in `10.20.0.0/16`? How many addresses? What are the private ranges?
8. What happens when you type a URL and press Enter (DNS, TCP, TLS, HTTP)?
9. TCP vs UDP.
