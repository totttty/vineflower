#!/usr/bin/env python3
"""Per-revision key lists of the H3 targets (dch3-20261124): the wrappers W whose published home is a stub.

  h3_keys.py WORK_DIR

Sources: data/held-rows.json rows "its published home is a stub" (jar name only: resolved to the @OriginalMember key
through the stock decompile WORK_DIR/stock<rev>/src/<Class>.java) and archive-b removal-*.json h3 rows with rule
"restored" (wrapperKey). Writes WORK_DIR/keysh3-<rev>.txt and prints the counts.
"""
import glob
import json
import os
import re
import sys

D = "/Users/jordantotty/Desktop/OSRS_NEW/output/dch3-20261124"
W = sys.argv[1]
ANN = re.compile(r'@OriginalMember\(\s*owner = "([^"]*)",\s*name = "([^"]*)",\s*descriptor = "([^"]*)"\s*\)\s*'
                 r'(?:@[^\n]*\n\s*)*[^\n(]*?\b([A-Za-z_$][\w$]*)\(', re.S)


def key_of(rev, wrapper):
    cls, rest = wrapper.split(".", 1)
    name = rest.split("(", 1)[0]
    path = os.path.join(W, "stock%d" % rev, "src", cls.split("$")[0] + ".java")
    hits = [m for m in ANN.finditer(open(path, encoding="utf-8").read()) if m.group(4) == name]
    if len(hits) != 1:
        return None
    m = hits[0]
    return "method %s.%s %s" % (m.group(1), m.group(2), m.group(3))


keys = {}
src = {}
for r in json.load(open(os.path.join(D, "data/held-rows.json")))["rows"]:
    if "home is a stub" in r["rule"]:
        k = key_of(r["revision"], r["wrapper"])
        if k is None:
            print("unresolved", r["revision"], r["wrapper"])
            continue
        keys.setdefault(r["revision"], set()).add(k)
        src[(r["revision"], k)] = "held"
for f in glob.glob(os.path.join(D, "archive-b/p-with-restorations/removal/removal-*.json")):
    rev = int(re.search(r"(\d+)\.json$", f).group(1))
    for h in json.load(open(f))["h3"]:
        if h.get("rule") == "restored":
            keys.setdefault(rev, set()).add(h["wrapperKey"])
            src[(rev, h["wrapperKey"])] = "restored"
total = 0
for rev in sorted(keys):
    with open(os.path.join(W, "keysh3-%d.txt" % rev), "w") as f:
        for k in sorted(keys[rev]):
            f.write(k + "\n")
    n_r = sum(1 for k in keys[rev] if src[(rev, k)] == "restored")
    print(rev, len(keys[rev]), "(restored %d)" % n_r)
    total += len(keys[rev])
print("total", total)
