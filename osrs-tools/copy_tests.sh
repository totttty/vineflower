#!/bin/bash
# Copy this fork's regression tests (sources, expected outputs, registrations) into another checkout DEST, to show
# they fail on stock 1.11.1.   copy_tests.sh DEST
set -u
H=/Users/jordantotty/Desktop/OSRS_NEW/vineflower
D=${1:?}
cp "$H/test/org/jetbrains/java/decompiler/SingleClassesTest.java" "$D/test/org/jetbrains/java/decompiler/"
for f in java8/pkg/TestRawIterableForeach java8/pkg/TestBooleanAndConstant java8nodebug/pkg/TestNarrowVarSlotReuse java8nodebug/pkg/TestNullOnlyVar; do
  cp "$H/testData/src/$f.java" "$D/testData/src/$f.java"
done
for t in TestRawIterableForeach TestBooleanAndConstant TestNarrowVarSlotReuse TestNullOnlyVar TestItrLoop TestArrayNull2 TestTryWithResourcesReturnJ16; do
  cp "$H/testData/results/pkg/$t.dec" "$D/testData/results/pkg/$t.dec"
done
