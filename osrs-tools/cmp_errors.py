#!/usr/bin/env python3
"""Compare two javac_check.py results: per-category counts and per-method appear / disappear lists.

  cmp_errors.py BEFORE.json AFTER.json [--methods]
"""
import collections
import json
import re
import sys


def cats(d):
    c = collections.Counter()
    for v in d["methods"].values():
        for e in v:
            m = re.sub(r"^\S+:\d+: ", "", e)
            m = re.sub(r"\b(Class|Interface)\w+", "C", m)
            c[m[:70]] += 1
    return c


a = json.load(open(sys.argv[1]))
b = json.load(open(sys.argv[2]))
ca, cb = cats(a), cats(b)
print("errors %d -> %d, methods %d -> %d, unmapped %d -> %d" % (
    a["errors"], b["errors"], len(a["methods"]), len(b["methods"]), len(a["unmapped"]), len(b["unmapped"])))
for k in sorted(set(ca) | set(cb), key=lambda k: -(ca[k] + cb[k])):
    if ca[k] != cb[k]:
        print("  %5d -> %5d  %s" % (ca[k], cb[k], k))
if "--methods" in sys.argv:
    fixed = sorted(set(a["methods"]) - set(b["methods"]))
    new = sorted(set(b["methods"]) - set(a["methods"]))
    print("methods now compiling: %d" % len(fixed))
    for k in fixed:
        print("  +", k, "|", a["methods"][k][0][:100])
    print("methods newly failing: %d" % len(new))
    for k in new:
        print("  -", k, "|", b["methods"][k][0][:100])
