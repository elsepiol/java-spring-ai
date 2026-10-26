# Interview day

Mixed practice across every track, shaped like real interview loops. Each session takes 60-90 minutes and combines **questions asked back at you** (the quiz), **live coding** (a kata under time pressure) and **design** (talking through a system). Do one session a day in the week before an interview; repeat the ones where the quiz shows weak spots (`java quiz/Quiz.java --stats`).

Rules that make it work:
- Answer out loud (or in writing) **before** revealing the answer. Grade yourself honestly; the spaced repetition schedule depends on it.
- Time-box katas. In an interview you have 25-40 minutes per coding problem, with someone watching: talk through your approach first, then code, then test.
- For design questions, follow the method in [tracks/15-system-design/L1-basics.md](../tracks/15-system-design/L1-basics.md) and use a timer per phase.
- Reset a kata before redoing it: `git stash` or `git checkout -- <file>` (your solved version is in your history if you committed it).

---

## Session 1: Java backend screen (75 min)

1. Quiz, 15 questions (20 min): `java quiz/Quiz.java --track java-core,concurrency --kind interview,predict,bug --count 15`
2. Live coding (30 min): `LruCache` (track 01 L2), then `TopK` if time remains.
   `cd katas && ./mvnw test -Dtest=LruCacheTest`
3. Concurrency follow-up (25 min): explain, then implement `BoundedBuffer` (track 02 L2). Be ready for "what if two consumers wake up at once?"

## Session 2: Spring and data (90 min)

1. Quiz, 20 questions (25 min): `java quiz/Quiz.java --track spring-core,spring-boot-web,jpa-transactions --count 20`
2. Live coding (30 min): `RegistrationController` + `ApiErrors` (track 07 L2): validation and problem details, tested with MockMvc.
3. Debugging round (20 min): `OrderTotals` (N+1, track 08 L2) and `TransferService` (REQUIRES_NEW and self-invocation, track 08 L3). Explain the bug before fixing it.
4. Talk-through (15 min): the L3 interview talk tracks of tracks 06, 07 and 08, out loud, in your own words.

## Session 3: SQL and databases (75 min)

1. Quiz (20 min): `java quiz/Quiz.java --track sql --count 15`, then `--track jpa-transactions --kind scenario --count 5`
2. Live SQL (30 min): the window-function and query-pattern katas (track 04 L2): `cd katas && ./mvnw test -Dgroups="sql & L2"`
3. Production round (25 min): `StockReservations` (track 04 L3). Then explain how you would add a NOT NULL column to a 200 M-row table without downtime.

## Session 4: Full-stack and frontend (90 min)

1. Quiz (25 min): `java quiz/Quiz.java --track js-ts,react,web-http --kind interview,predict,bug --count 20`
2. Live coding (35 min): `useFetch` (track 13 L2), then `SignupForm`. `cd web-katas && npx vitest src/react/l2`
3. JavaScript fundamentals (15 min): `promises` (track 12 L2): `mapLimit` from scratch.
4. Talk-through (15 min): "What happens when you type a URL and press Enter?" all the way from DNS to React rendering (tracks 14 L1, 05 L1, 13 L1).

## Session 5: Production engineering (75 min)

1. Quiz (20 min): `java quiz/Quiz.java --track docker-deploy,git-linux-networking,testing,security --kind scenario,interview --count 15`
2. Incident drills (30 min), talked through with the tools you would type: "latency doubled after a deploy", "pods restart every few minutes", "PKIX path building failed", "Too many open files". Use the L3 lessons of tracks 11 and 14 as your checklist.
3. Live coding (25 min): `ShutdownCoordinator` (track 11 L3) or `LoginThrottle` (track 09 L3).

## Session 6: System design loop (90 min)

1. Warm-up quiz (15 min): `java quiz/Quiz.java --track system-design --count 10`
2. Design interview (45 min), timed per phase: pick one of the practice interviews in [tracks/15-system-design/L3-production.md](../tracks/15-system-design/L3-production.md) section 6. Draw it, estimate it, then deep-dive twice.
3. Building block (30 min): implement `HashRing` or `Saga` (track 15) and explain where you would use it in the design you just drew.

## Session 7: The mixed mock (90 min)

1. Quiz across everything (20 min): `java quiz/Quiz.java --kind interview --count 12`
2. One DSA-flavoured kata you have not done for a while (25 min): `KWayMerge` (track 01 L3), `CommitGraph` (track 14 L2), `Bisect` (track 14 L3) or `TokenBucket` (track 02 L2).
3. One "find the bug" kata (15 min): `FeatureFlags` (track 10 L3), `CatalogService` (track 08 L2) or `makeButtonHandlers` in `closures` (track 12 L1).
4. Behavioural + project deep-dive (30 min): explain the shop project end to end as if it were yours: architecture, one hard bug you fixed, one trade-off you made, what you would change. The lessons' "Seen in the shop project" sections are your notes.

---

## Quick drills (10 minutes, any day)

```
java quiz/Quiz.java                                  # whatever is due today, all tracks
java quiz/Quiz.java --kind predict --count 10        # "what does this print / return?"
java quiz/Quiz.java --kind bug --count 10            # spot the bug
java quiz/Quiz.java --kind dsa --count 5             # data structures and algorithms tied to the concepts
java quiz/Quiz.java --level 3 --kind design --count 5
java quiz/Quiz.java --stats                          # where you are weak, per track and level
```

## The night before

Re-read the "Interview checklist" or "Interview talk track" at the end of each L1-L3 lesson of the tracks the job description mentions, and say each talk track out loud once. Sleep.
