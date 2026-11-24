#!/bin/bash
# Run the upstream test suite at each given commit in a separate worktree WT (detached), listing failures per commit.
#   per_commit_tests.sh WT COMMIT...
set -u
WT=${1:?}
shift
for c in "$@"; do
  git -C "$WT" checkout -q --force "$c" || exit 1
  git -C "$WT" clean -q -fd testData/results testData/src test
  (cd "$WT" && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --no-daemon --continue > "$WT/test-$c.log" 2>&1)
  echo "== $c $(git -C "$WT" log --oneline -1 "$c")"
  python3 /Users/jordantotty/Desktop/OSRS_NEW/vineflower/osrs-tools/test_failures.py "$WT/build" "$WT/plugins"
done
