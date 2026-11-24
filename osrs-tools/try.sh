#!/bin/bash
# Compile one scratch test source with javac 21 (no debug info, like the obfuscated client) and decompile it with the
# pipeline options.   try.sh SRC.java OUT_DIR [VINEFLOWER_JAR]
set -u
SRC=${1:?}
OUT=${2:?}
VF=${3:-/Users/jordantotty/Desktop/OSRS_NEW/vineflower/build/libs/vineflower-1.11.1+local.jar}
JH=$(/usr/libexec/java_home -v 21)
rm -rf "${OUT:?}"
mkdir -p "$OUT/cls" "$OUT/src"
"$JH/bin/javac" -g:none --release 8 -nowarn -d "$OUT/cls" "$SRC" || exit 1
"$JH/bin/java" -jar "$VF" "-ind=  " -rbr=0 -rsy=0 -dgs=1 -ump=0 -udv=0 -bto=0 -ren=1 -jrt=current -thr=1 \
  "$OUT/cls" "$OUT/src" > "$OUT/vf.log" 2>&1
cat "$OUT"/src/*.java
