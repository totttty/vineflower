#!/usr/bin/env python3
"""Write SingleClassesTest.java with the registrations of the first N fixes only (for one commit per cause).

  stage_tests.py FINAL_COPY N OUT
"""
import sys

final, n, out = sys.argv[1], int(sys.argv[2]), sys.argv[3]
text = open(final).read()
# (fix number, exact text added for it); removed when the fix number > N
parts = [
    (1, '    register(JAVA_8, "TestRawIterableForeach");\n'),
    (2, '    register(JAVA_8_NODEBUG, "TestNarrowVarSlotReuse");\n'),
    (3, '    register(JAVA_8_NODEBUG, "TestNullOnlyVar");\n'),
    (4, '    registerSet("No Boolean As Int", this::registerNoBooleanAsInt,\n'
        '      IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",\n'
        '      IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1",\n'
        '      IFernflowerPreferences.DUMP_EXCEPTION_ON_ERROR, "0",\n'
        '      IFernflowerPreferences.IGNORE_INVALID_BYTECODE, "1",\n'
        '      IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES, "1",\n'
        '      IFernflowerPreferences.BOOLEAN_TRUE_ONE, "0"\n'
        '    );\n'),
    (4, '\n  private void registerNoBooleanAsInt() {\n    register(JAVA_8, "TestBooleanAndConstant");\n  }\n'),
]
for num, s in parts:
    assert text.count(s) == 1, s
    if num > n:
        text = text.replace(s, "")
open(out, "w").write(text)
