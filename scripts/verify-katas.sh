#!/usr/bin/env bash
# Run by CI. Proves two things about every kata, on a COPY of the repo (your files are untouched):
#   1. Without its solution, every kata test class has at least one failing test (it is not done yet).
#   2. With every reference solution from solutions/ applied, every test passes (it is solvable).
# Also validates the question banks and compiles every example program.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'code=$?; if [ "$code" -ne 0 ] && [ -d "$work/katas/target/surefire-reports" ]; then rm -rf "$root/katas/target/surefire-reports"; mkdir -p "$root/katas/target"; cp -r "$work/katas/target/surefire-reports" "$root/katas/target/"; fi; rm -rf "$work"' EXIT

fail() { echo "::error::$*"; echo "ERROR: $*" >&2; exit 1; }

tar -C "$root" --exclude=./.git --exclude=./katas/target --exclude=./.dojo -cf - . | tar -C "$work" -xf -
cd "$work"

echo "== Question banks"
java quiz/Quiz.java --check

echo "== Example programs compile"
for f in tracks/*/examples/*.java; do
  [ -e "$f" ] || continue
  javac -d "$work/examples-out" "$f" || fail "$f does not compile"
done

# Reads surefire's XML reports: one line per test class, "<class> <tests> <failures+errors> <skipped>".
summarize() {
  for f in katas/target/surefire-reports/TEST-*.xml; do
    [ -e "$f" ] || continue
    head -c 2000 "$f" | tr '\n' ' ' | sed -E 's/.*<testsuite[^>]* name="([^"]+)"[^>]* tests="([0-9]+)"[^>]* errors="([0-9]+)"[^>]* skipped="([0-9]+)"[^>]* failures="([0-9]+)".*/\1 \2 \3 \4 \5/' \
      | awk '{ print $1, $2, $3 + $5, $4 }'
  done
}

check() {
  local mode="$1" count=0
  while read -r cls tests bad skipped; do
    echo "  $cls: tests=$tests failing=$bad skipped=$skipped"
    [ "$tests" -gt 0 ] && [ "$skipped" -eq 0 ] || fail "$cls: tests did not actually run"
    if [ "$mode" = unsolved ] && [ "$bad" -eq 0 ]; then fail "$cls passes without its solution: the kata is already done"; fi
    if [ "$mode" = solved ] && [ "$bad" -ne 0 ]; then fail "$cls fails even with the reference solution"; fi
    count=$((count + 1))
  done < <(summarize)
  [ "$count" -ge "${MIN_KATAS:-1}" ] || fail "expected at least ${MIN_KATAS:-1} kata test classes, found $count"
}

echo "== 1. Without solutions every kata must fail"
(cd katas && ./mvnw -B -q test -Dsurefire.failIfNoSpecifiedTests=false > "$work/unsolved.log" 2>&1) || true
check unsolved

echo "== 2. Applying the reference solutions"
cp -r solutions/. ./

echo "== 3. With solutions every kata must pass"
(cd katas && ./mvnw -B -q clean test) || { tail -80 katas/target/surefire-reports/*.txt 2>/dev/null | grep -B2 -A10 "FAIL" | head -100; fail "katas fail with the solutions"; }
check solved

echo "All katas verified."
