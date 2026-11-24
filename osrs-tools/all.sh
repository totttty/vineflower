#!/bin/bash
# rev.sh for several revisions in a row: all.sh TAG VINEFLOWER_JAR REV...
set -u
TAG=${1:?}
VF=${2:?}
shift 2
for r in "$@"; do
  echo "== $r"
  /Users/jordantotty/Desktop/OSRS_NEW/vineflower/osrs-tools/rev.sh "$r" "$TAG" "$VF"
done
echo "all done"
