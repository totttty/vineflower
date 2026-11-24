#!/bin/bash
# Delete decompile output trees of the given tags under the work dir W: clean.sh TAG...  (W/<TAG><rev> dirs only)
set -u
W=${W:-/private/tmp/claude-501/-Users-jordantotty-Desktop-OSRS-NEW/5c39c902-4088-44b9-ac6d-0e901536045a/scratchpad/w}
for t in "$@"; do
  for d in "$W/$t"2[0-9][0-9]; do
    [ -d "$d" ] || continue
    rm -rf "${d:?}"
  done
done
