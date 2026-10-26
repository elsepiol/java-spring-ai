#!/usr/bin/env bash
# Run by CI. The web-katas (tracks 12-13) get the same two proofs as the Java katas, on a COPY:
#   1. Without its solution, every kata test file has at least one failing test.
#   2. With every reference solution from solutions/web-katas applied, every test passes.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

fail() { echo "::error::$*"; echo "ERROR: $*" >&2; exit 1; }

[ -d "$root/web-katas/node_modules" ] || (cd "$root/web-katas" && npm ci --no-audit --no-fund)

tar -C "$root" --exclude=./.git --exclude=./katas --exclude=./web-katas/node_modules --exclude=./.dojo -cf - . | tar -C "$work" -xf -
ln -s "$root/web-katas/node_modules" "$work/web-katas/node_modules"
cd "$work/web-katas"

echo "== Type check (the stubs and the tests must compile)"
npx tsc --noEmit

# Prints one line per test file: "<file> <tests> <failing>", from vitest's JSON report.
summarize() {
  node -e '
    const r = require(process.argv[1]);
    for (const f of r.testResults) {
      const tests = f.assertionResults.length;
      const failing = f.assertionResults.filter(a => a.status !== "passed").length + (tests === 0 && f.status === "failed" ? 1 : 0);
      console.log(require("path").relative(process.cwd(), f.name), tests, failing);
    }' "$1"
}

check() {
  local mode="$1" report="$2" count=0
  while read -r file tests bad; do
    echo "  $file: tests=$tests failing=$bad"
    if [ "$mode" = unsolved ] && [ "$bad" -eq 0 ]; then fail "$file passes without its solution: the kata is already done"; fi
    if [ "$mode" = solved ] && [ "$bad" -ne 0 ]; then fail "$file fails even with the reference solution"; fi
    count=$((count + 1))
  done < <(summarize "$report")
  [ "$count" -ge "${MIN_WEB_KATAS:-1}" ] || fail "expected at least ${MIN_WEB_KATAS:-1} web kata test files, found $count"
}

echo "== 1. Without solutions every kata must fail"
npx vitest run --reporter=json --outputFile="$work/unsolved.json" > "$work/unsolved.log" 2>&1 || true
check unsolved "$work/unsolved.json"

echo "== 2. Applying the reference solutions"
cp -r "$work/solutions/web-katas/." ./

echo "== 3. With solutions every kata must pass"
npx tsc --noEmit
npx vitest run --reporter=json --outputFile="$work/solved.json" > "$work/solved.log" 2>&1 || { cat "$work/solved.log" | tail -60; }
check solved "$work/solved.json"

echo "All web katas verified."
