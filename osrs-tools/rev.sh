#!/bin/bash
# One revision end to end: pre-pass (once, cached), decompile with VF jar into W/<tag><rev>, javac-check it.
#   rev.sh REV TAG [VINEFLOWER_JAR]
# W is the work dir (env W, default the session scratchpad work dir). Input jars: W/inputs.txt ("REV PATH" lines).
set -u
REV=${1:?}
TAG=${2:?}
VF=${3:-/Users/jordantotty/Desktop/OSRS_NEW/vineflower/build/libs/vineflower-1.11.1+local.jar}
W=${W:-/private/tmp/claude-501/-Users-jordantotty-Desktop-OSRS-NEW/5c39c902-4088-44b9-ac6d-0e901536045a/scratchpad/w}
T=/Users/jordantotty/Desktop/OSRS_NEW/vineflower/osrs-tools
J=$(/usr/libexec/java_home -v 21)/bin/java
LIB=/Users/jordantotty/Desktop/OSRS_NEW/oracle-deob/kotlin/daemon/build/install/daemon/lib
IN=$(awk -v r="$REV" '$1 == r {print $2}' "$W/inputs.txt")
[ -n "$IN" ] || { echo "no input for $REV"; exit 2; }
mkdir -p "$W/jars"
if [ ! -f "$W/jars/$REV.jar" ]; then
  "$J" -cp "$LIB/*" "$T/PrePass.java" "$IN" "$W/jars/$REV.jar" "$W/tmp-prepass-$REV" || exit 1
  rm -rf "${W:?}/tmp-prepass-${REV:?}"
fi
rm -rf "${W:?}/${TAG:?}${REV:?}"
"$T/decompile.sh" "$W/jars/$REV.jar" "$W/$TAG$REV" "$VF" || exit 1
CP="$W/cp$REV.txt"
[ -f "$CP" ] || CP="$W/cp-legacy.txt"
python3 "$T/javac_check.py" "$CP" "$W/$TAG$REV/src" "$W/$TAG$REV.json"
