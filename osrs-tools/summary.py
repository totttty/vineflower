#!/usr/bin/env python3
"""Before/after target counts: summary.py WORK_DIR BEFORE_TAG AFTER_TAG

Target lists: W/keys242.txt (reviewed stubs 242), W/keys241.txt (241 allowedRepairStubs), W/keysh3-<rev>.txt (H3).
Prints, per source, how many targets have javac errors in the BEFORE and AFTER trees, and each remaining failure.
"""
import glob
import json
import os
import re
import sys

W, before, after = sys.argv[1:4]


def keys(path):
    return [l.strip() for l in open(path) if l.strip() and not l.startswith("#")]


def check(tag, rev):
    return json.load(open(os.path.join(W, "%s%d.json" % (tag, rev))))["methods"]


sources = [("242 reviewed stubs", [(242, keys(os.path.join(W, "keys242.txt")))]),
           ("241 repair stubs", [(241, keys(os.path.join(W, "keys241.txt")))])]
h3 = []
for f in sorted(glob.glob(os.path.join(W, "keysh3-*.txt"))):
    h3.append((int(re.search(r"(\d+)\.txt$", f).group(1)), keys(f)))
sources.append(("H3 stub homes 230-241", h3))
for name, revs in sources:
    n = b = a = 0
    remaining = []
    for rev, ks in revs:
        cb, ca = check(before, rev), check(after, rev)
        for k in ks:
            n += 1
            b += bool(cb.get(k))
            a += bool(ca.get(k))
            if ca.get(k):
                remaining.append("%d %s | %s" % (rev, k, ca[k][0]))
    print("%s: %d targets; javac errors before %d, after %d" % (name, n, b, a))
    for r in remaining:
        print("   still failing:", r)
