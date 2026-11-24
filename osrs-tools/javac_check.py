#!/usr/bin/env python3
"""javac-check a Vineflower output tree and map every error to its method (@OriginalMember key).

  javac_check.py CLASSPATH_FILE SRC_DIR OUT_JSON

CLASSPATH_FILE: one classpath entry per line. Only the default-package sources (the client) are compiled; the
library packages (com/, org/) come from the classpath, as in oracle-deob. javac runs through FLOW (so definite
assignment errors are reported next to attribution errors) and writes no class files; no annotation processing.
OUT_JSON: {"errors": n, "methods": {key: [ "Line: message", ...]}, "unmapped": [...]}.
"""
import json
import os
import re
import subprocess
import sys

JAVAC = os.path.join(subprocess.check_output(["/usr/libexec/java_home", "-v", "21"], text=True).strip(), "bin", "javac")
ANN = re.compile(r'@OriginalMember\(\s*owner = "([^"]*)",\s*name = "([^"]*)",\s*descriptor = "([^"]*)"\s*\)', re.S)
ERR = re.compile(r'^(.*\.java):(\d+): error: (.*)$')


def method_ranges(path):
    """[(key, start_line, end_line)] for every annotated method/constructor (1-based, inclusive)."""
    text = open(path, encoding="utf-8").read()
    lines = text.split("\n")
    out = []
    for m in ANN.finditer(text):
        key = "method %s.%s %s" % (m.group(1), m.group(2), m.group(3))
        if "(" not in m.group(3):
            continue  # field
        ann_line = text.count("\n", 0, m.start())  # 0-based
        indent = len(lines[ann_line]) - len(lines[ann_line].lstrip("\t"))
        # declaration: first line after the annotation block that is not an annotation line
        i = text.count("\n", 0, m.end()) + 1
        while i < len(lines) and lines[i].lstrip("\t").startswith("@"):
            i += 1
        if i >= len(lines):
            continue
        if lines[i].rstrip().endswith(";"):
            out.append((key, ann_line + 1, i + 1))
            continue
        close = "\t" * indent + "}"
        j = i
        while j < len(lines) and lines[j] != close and not (j == i and lines[j].rstrip().endswith("}")):
            j += 1
        out.append((key, ann_line + 1, j + 1))
    return out


def main():
    cp_file, src, out_json = sys.argv[1:4]
    cp = [l.strip() for l in open(cp_file) if l.strip()]
    files = sorted(os.path.join(src, f) for f in os.listdir(src) if f.endswith(".java"))
    argfile = out_json + ".args"
    with open(argfile, "w") as f:
        for p in files:
            f.write('"%s"\n' % p)
    cmd = [JAVAC, "-J-Xmx6g", "-source", "17", "-target", "17", "-encoding", "UTF-8", "-Xmaxerrs", "100000",
           "-nowarn", "-proc:none", "-implicit:none", "-XDshould-stop.ifError=FLOW", "-XDshould-stop.ifNoError=FLOW",
           "-cp", ":".join(cp), "@" + argfile]
    r = subprocess.run(cmd, capture_output=True, text=True)
    os.remove(argfile)
    ranges = {}
    methods = {}
    unmapped = []
    n = 0
    for line in (r.stdout + r.stderr).splitlines():
        m = ERR.match(line)
        if not m:
            continue
        n += 1
        path, ln, msg = m.group(1), int(m.group(2)), m.group(3)
        if path not in ranges:
            ranges[path] = method_ranges(path)
        hit = [k for (k, a, b) in ranges[path] if a <= ln <= b]
        if hit:
            # innermost: the last (latest-starting) range that contains the line
            methods.setdefault(hit[-1], []).append("%s:%d: %s" % (os.path.basename(path), ln, msg))
        else:
            unmapped.append("%s:%d: %s" % (os.path.basename(path), ln, msg))
    json.dump({"errors": n, "javacExit": r.returncode, "methods": dict(sorted(methods.items())), "unmapped": unmapped},
              open(out_json, "w"), indent=1)
    print("javac exit %d, %d errors, %d methods, %d unmapped" % (r.returncode, n, len(methods), len(unmapped)))


if __name__ == "__main__":
    main()
