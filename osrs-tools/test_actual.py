#!/usr/bin/env python3
"""Diff a failed SingleClassesTest's expected and actual output: test_actual.py BUILD_DIR TEST_NAME [--write REF.dec]

Reads the AssertionFailedError text of build/test-results; prints a unified diff; with --write, writes the actual
output to the given reference file (only after the diff has been reviewed).
"""
import difflib
import glob
import os
import sys
import xml.etree.ElementTree as ET

build, name = sys.argv[1], sys.argv[2]
for f in glob.glob(os.path.join(build, "test-results", "test", "*.xml")):
    for tc in ET.parse(f).getroot().iter("testcase"):
        if tc.get("name") != name or tc.find("failure") is None:
            continue
        t = tc.find("failure").text
        head = "expected: <"
        sep = "> but was: <"
        i = t.index(head) + len(head)
        j = t.index(sep, i)
        k = t.index(">\n\tat ", j) if ">\n\tat " in t[j:] else t.rindex(">")
        exp, act = t[i:j], t[j + len(sep):k]
        sys.stdout.writelines(difflib.unified_diff(exp.splitlines(True), act.splitlines(True), "expected", "actual", n=1))
        if "--write" in sys.argv:
            with open(sys.argv[sys.argv.index("--write") + 1], "w") as out:
                out.write(act)
        sys.exit(0)
print("no failure for", name)
