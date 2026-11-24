#!/usr/bin/env python3
"""Method-level diff of two decompile trees (stock vs fork) of one revision, joined with their javac checks.

  method_diff.py STOCK_SRC FORK_SRC STOCK_CHECK.json FORK_CHECK.json OUT.json

Every top-level .java file is compared; changed files are split into @OriginalMember-annotated members (methods and
constructors; a class header / field change is reported per file). Each changed method is classified:
  fixed      stock had javac errors in it, the fork has none
  improved   both have errors, the fork fewer
  changed-ok both compile (text change only)
  worse      the fork has errors the stock did not (or more)
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from javac_check import method_ranges  # noqa: E402


def members(path):
    lines = open(path, encoding="utf-8").read().split("\n")
    out = {}
    covered = set()
    for key, a, b in method_ranges(path):
        out[key] = "\n".join(lines[a - 1:b])
        covered.update(range(a - 1, b))
    rest = "\n".join(l for i, l in enumerate(lines) if i not in covered)
    return out, rest


stock, fork, cs, cf, out_path = sys.argv[1:6]
es = json.load(open(cs))["methods"]
ef = json.load(open(cf))["methods"]
files = sorted(f for f in set(os.listdir(stock)) | set(os.listdir(fork)) if f.endswith(".java"))
changed_files = []
methods = []
for f in files:
    a, b = os.path.join(stock, f), os.path.join(fork, f)
    if not (os.path.exists(a) and os.path.exists(b)):
        changed_files.append({"file": f, "only": "stock" if os.path.exists(a) else "fork"})
        continue
    if open(a, "rb").read() == open(b, "rb").read():
        continue
    ma, ra = members(a)
    mb, rb = members(b)
    entry = {"file": f, "outsideMembers": ra != rb, "methods": []}
    for k in sorted(set(ma) | set(mb)):
        if ma.get(k) == mb.get(k):
            continue
        na, nb = len(es.get(k, [])), len(ef.get(k, []))
        kind = ("fixed" if na and not nb else "improved" if nb < na else
                "changed-ok" if not na and not nb else "worse")
        methods.append({"key": k, "file": f, "kind": kind, "stockErrors": na, "forkErrors": nb})
        entry["methods"].append(k)
    changed_files.append(entry)
summary = {}
for m in methods:
    summary[m["kind"]] = summary.get(m["kind"], 0) + 1
json.dump({"changedFiles": len(changed_files), "summary": summary, "files": changed_files, "methods": methods},
          open(out_path, "w"), indent=1)
print("changed files %d, changed methods %d: %s; files with changes outside members: %d" % (
    len(changed_files), len(methods), summary, sum(1 for e in changed_files if e.get("outsideMembers"))))
