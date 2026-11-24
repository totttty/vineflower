#!/usr/bin/env python3
"""Print the javac errors (javac_check.py output) of every target method.

  stub_errors.py CHECK_JSON KEYS_FILE [--summary]

KEYS_FILE: one method key per line ("method client!aa.gz (I)V"), '#' comments allowed.
"""
import json
import sys

check = json.load(open(sys.argv[1]))
keys = [l.strip() for l in open(sys.argv[2]) if l.strip() and not l.startswith("#")]
summary = "--summary" in sys.argv
bad = 0
for k in keys:
    errs = check["methods"].get(k, [])
    if errs:
        bad += 1
    if summary:
        print("%-4s %s %s" % ("FAIL" if errs else "ok", k, (errs[0] if errs else "")[:110]))
    elif errs:
        print(k)
        for e in errs:
            print("   ", e)
print("%d of %d target methods have javac errors" % (bad, len(keys)))
