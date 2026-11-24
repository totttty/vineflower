#!/usr/bin/env python3
"""List the failed / errored tests of a Gradle run: test_failures.py BUILD_DIR... (reads test-results/**/TEST-*.xml)."""
import glob
import os
import sys
import xml.etree.ElementTree as ET

total = 0
failed = []
for root in sys.argv[1:]:
    for f in sorted(glob.glob(os.path.join(root, "**", "test-results", "**", "TEST-*.xml"), recursive=True)):
        for tc in ET.parse(f).getroot().iter("testcase"):
            total += 1
            if tc.find("failure") is not None or tc.find("error") is not None:
                failed.append("%s :: %s" % (tc.get("classname"), tc.get("name")))
for t in failed:
    print(t)
print("%d tests, %d failed" % (total, len(failed)))
