#!/bin/bash
# Decompile a pre-passed jar (PrePass.java output) with oracle-deob's pinned Vineflower options
# (oracle-deob kotlin/decompile DecompileOptions: the options that differ from Vineflower's defaults).
#   decompile.sh IN_JAR OUT_DIR [VINEFLOWER_JAR]   (default: this fork's build/libs jar)
# Reads the jar as data; never runs it. OUT_DIR must not exist.
set -u
H=/Users/jordantotty/Desktop/OSRS_NEW/vineflower
IN=${1:?}
OUT=${2:?}
VF=${3:-$H/build/libs/vineflower-1.11.1+local.jar}
J=$(/usr/libexec/java_home -v 21)/bin/java
if [ -e "$OUT" ]; then echo "exists: $OUT"; exit 2; fi
mkdir -p "$OUT/src"
"$J" -Xmx6g -jar "$VF" "-ind=	" -rbr=0 -rsy=0 -dgs=1 -ump=0 -udv=0 -bto=0 -ren=1 -jrt=current -thr=1 \
  "$IN" "$OUT/src" > "$OUT/vf.log" 2>&1
echo "vineflower exit $? ($(find "$OUT/src" -name '*.java' | wc -l | tr -d ' ') java files)"
# Vineflower writes a jar input as OUT/src/<name>.jar of sources; unpack it so javac can read the tree.
for z in "$OUT"/src/*.jar; do
  [ -f "$z" ] || continue
  mkdir -p "$OUT/src/x"
  (cd "$OUT/src/x" && unzip -q -o "$z")
  rm -f "${z:?}"
done
