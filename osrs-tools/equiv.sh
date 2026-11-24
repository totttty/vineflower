#!/bin/bash
# Check the osrs/1.11.1 fork jar decompiles exactly like stock Maven Vineflower 1.11.1: decompile one revision's deob jar
# (read only; decompiling reads class files, it never runs them) with both, same options, then diff the outputs.
#   equiv.sh INPUT_JAR
set -u
H=/Users/jordantotty/Desktop/OSRS_NEW/vineflower
IN=${1:?}
STOCK=/Volumes/Ventura/oracle-deob-work/gradle-home/caches/modules-2/files-2.1/org.vineflower/vineflower/1.11.1/77767cdbc76d116c6d5e1565ec387e531085b9f4/vineflower-1.11.1.jar
FORK=$H/build/libs/vineflower-1.11.1+local.jar
J=$(/usr/libexec/java_home -v 21)/bin/java
T=$H/tmp-equiv
rm -rf "${T:?}"
mkdir -p "$T/stock" "$T/fork"
OPTS="-ind=	 -bsm=1 -dcl=1 -dgs=1 -din=1 -ren=1 -thr=1"
"$J" -Xmx4g -jar "$STOCK" $OPTS "$IN" "$T/stock" > "$T/stock.log" 2>&1
echo "stock exit $?"
"$J" -Xmx4g -jar "$FORK" $OPTS "$IN" "$T/fork" > "$T/fork.log" 2>&1
echo "fork exit $?"
echo "files: stock $(find "$T/stock" -type f | wc -l | tr -d ' '), fork $(find "$T/fork" -type f | wc -l | tr -d ' ')"
grep -c -i "couldn't be decompiled\|\$VF: Couldn" -r "$T/stock" | awk -F: '{s+=$2} END {print "stock VF failure markers:", s}'
if diff -rq "$T/stock" "$T/fork" > "$T/diff.txt"; then echo "IDENTICAL OUTPUT"; else echo "OUTPUT DIFFERS: $(wc -l < "$T/diff.txt") files"; head -5 "$T/diff.txt"; diff -r "$T/stock" "$T/fork" | head -40; fi
# determinism: stock a second time
mkdir -p "$T/stock2"
"$J" -Xmx4g -jar "$STOCK" $OPTS "$IN" "$T/stock2" > "$T/stock2.log" 2>&1
if diff -rq "$T/stock" "$T/stock2" > "$T/diff2.txt"; then echo "STOCK RUN-TO-RUN: identical"; else echo "STOCK RUN-TO-RUN: $(wc -l < "$T/diff2.txt") files differ"; head -5 "$T/diff2.txt"; fi
rm -rf "${T:?}"
