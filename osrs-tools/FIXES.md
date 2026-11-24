# Vineflower fixes for the OSRS deob corpus (branch osrs/1.11.1)

Base: tag 1.11.1 + `c83512a9` (osrs-tools/equiv.sh). Four fixes, one commit per root cause, each with an upstream-style
regression test. 2026-11-24.

## How the failures were reproduced

The input is the same one oracle-deob's `decompile` stage gives Vineflower:

- jar: the `02-openrs2` stage output (`gamepack_deob.jar`): 230-241 from
  `/Volumes/Ventura/oracle-deob-work/namecorr-sweep/work/jars.txt`, 242 from `runs/rev242-m5/n242-final`;
- the two pre-decompile bytecode passes (`neutralizeInvalidLambdaInvokedynamics`, `splitDisjointIntLocalSlots`), run by
  reflection on oracle-deob's own `DecompileStage` (`PrePass.java`, daemon lib classpath; ASM only, the jar is never run);
- the pinned Vineflower options of `DecompileOptions` (`decompile.sh`: `-ind=\t -rbr=0 -rsy=0 -dgs=1 -ump=0 -udv=0 -bto=0
  -ren=1 -jrt=current -thr=1`).

Check: stock 1.11.1 output for 242 is byte-identical to `n242-final/stages/03-decompile/out/sources` (241: identical to
`final-replay-m4` except `Interface23`, `@interface` vs `interface`, an older daemon). Stock output is deterministic with
`-thr=1` (two runs of 241 and 242 identical).

Compile: `javac_check.py` runs javac 21 (`-source 17 -target 17`, the revision's javac classpath from the oracle-deob
config or `dch3-20261124/data/cp`, plus `bytecode.jar` for the `@OriginalMember` annotations) on the raw decompile tree
through FLOW, no class files, and maps every error to its method by `@OriginalMember`. A target "fails" when javac reports
an error inside its body. (The raw tree also has errors the pipeline's normalize / repair stages fix by design, e.g. the
neutralized-lambda parse errors and checked exceptions; those are not targets.)

## Root causes and commits

| # | Commit | Cause (Vineflower) | Test |
|---|---|---|---|
| 1 | `0bd99295` Cast the iterable of a for-each whose element type is Object | `DoStatement` printed `for (T x : it)` with `T` from the checkcast on `next()` even when the iterable's static element type is Object: a raw type (no Signature attributes: `Class22 implements Iterable`, raw `ArrayList`/`HashSet`) or a `List<Object>` field. Now prints `(Iterable<T>)` (raw) or `(Iterable<T>)(Iterable)` (other concrete element type), only when the printed expression's static type is certain (class without type parameters, field, local of a method without generic signature, printed cast, invocation with declared / erased / parameterized return type). | `TestRawIterableForeach` (Java Runtime set); `TestItrLoop.dec` updated (its expected output did not compile) |
| 2 | `a93339ce` Don't widen a narrow variable when merging it breaks its uses | `VarDefinitionHelper.mergeVars` merged a `byte` variable into a later `int` one reusing the slot, so `pos[v] = v++` (byte[] store, implicit-narrowing `++`) became int code that does not compile. The merge is refused when the merged type exceeds a max type of either variable (an object max type is ignored for primitives), and the later definition is kept (it used to be removed before the merge was tried). Also: `populateTypeBounds` recorded a primitive return type as the object max type `LI;`. | `TestNarrowVarSlotReuse` (Java 8 no-debug) |
| 3 | `e2d75a4a` Type a variable only ever assigned null from its uses | A local whose only value is `null` became `Object` (`Object f = null; f.getPath()`). `VarTypeProcessor` now records the object types its uses require (instance of an invocation, argument, return); `eliminateNonJavaTypes` uses the most specific one, Object on conflict. | `TestNullOnlyVar` (Java 8 no-debug); `TestArrayNull2.dec` (its TODO "Object should be int[]") and `TestTryWithResourcesReturnJ16.dec` updated: precise type, cast gone |
| 4 | `163b5fb6` Keep a constant boolean next to a fixed boolean operand | With `boolean-as-int` off (`-bto=0`, the pipeline's setting) `1` has no boolean value, so `FunctionExprent` forced `true & m()` to int: `int b = 1 & m()`. Not when the other operand is a method result, field, array element or boolean operator. | `TestBooleanAndConstant` (new set "No Boolean As Int", `bto=0`) |

Fails before / passes after: the 4 new tests and the 3 updated references were copied onto stock 1.11.1
(`copy_tests.sh`): all 7 fail there. On every one of the 4 commits (`per_commit_tests.sh`) and on HEAD the suite has
exactly the 9 pre-existing failures.

## Upstream tests (`./gradlew test`)

Stock 1.11.1 (`c83512a9`) on this machine: 1338 tests, 9 fail: `TestJava9StringConcat`, `TestJava11StringConcat`,
`TestSwitchPatternMatching13/14/15/16/22`, `TestSwitchPatternMatchingInstanceof4`, `TestSwitchPatternMatchingFuzz1`
(toolchain/javac version differences; not touched). HEAD: 1342 tests, the same 9 fail, nothing else.
The suite needs JDK 8/11/16 toolchains: Gradle provisioned them (foojay) into `~/.gradle/jdks`, which is a symlink to
`/Volumes/Ventura/tmp/obsidian-local-cache-offload/gradle-user-home-global` (about 950 MB there; the downloaded archives
were deleted).

## Before / after (raw decompile, javac errors inside the target body)

| Source | Targets | Fail with stock 1.11.1 | Fail with the fork |
|---|---:|---:|---:|
| 242 reviewed stubs (`config/m5/reviewed-stubs-242.json`) | 32 | 31 | 1 |
| 241 repair stubs (`config/rev241-m4.json` allowedRepairStubs) | 35 | 34 | 1 |
| H3 stub homes 230-241 (`dch3-20261124`: 67 wrappers; 23 certified restorations among them) | 67 | 27 (14 of the 23) | 2 (0 of the 23) |

Root causes of the 65 stock failures among the 241/242 stubs: cause 1 for 60 (58 fixed, incl. the raw `HashSet`
unboxing `gh.xa`; 2 over a RuneLite type, below), cause 2 for `yb.aq`/`yu.ai` (bzip2 MTF `byte` loop), cause 3 for `ty.an`/`rc.ax` (`Object var59 = null`),
cause 4 for `ge.pn` (`1 & bool`). Whole trees (methods with any javac error, stock -> fork):
230 163->75, 231 175->85, 232 173->72, 233 168->73, 234 169->75, 235 173->79, 236 185->82, 237 169->76, 238 169->74,
239 167->75, 240 182->88, 241 189->90, 242 190->94. No method gets an error it did not have.

Note: the current 242 run (`n242-final`, also `n242-rc`/`rb3`) already has 0 repair stubs (oracle-deob's repair now
casts these for-each loops); the 32 are the reviewed list. The fork removes the need for that repair in Vineflower.

### Still failing

- `241 rl24.ap`, `242 rl24.bq`: `for (WorldEntity x : var0.worldEntities())`, the iterable is RuneLite's
  `IndexedObjectSet` (raw). Vineflower is not given the RuneLite API, so it cannot know the type is an `Iterable`
  (the fix stays off for unknown classes). With the API as a library (`-e=<snapshots/241-real/000/main>`) the 241 target
  compiles too (0 of 35), but that run also adds 11 `@Override` errors elsewhere, so it is not recommended as is.
- `233 client.uk`: `netscape.javascript.JSObject` is not on the 233 compile classpath (environment, not Vineflower).
- `240 qo.ej`: `Object var5 = new ArrayList(); ... var5 = var5.subList(0, 2)`: a variable assigned an `ArrayList` and a
  `List` is typed Object. Not fixed (a first attempt in `VarTypeProcessor` did not change it; the merge happens elsewhere).
- Not reproducible as Vineflower failures (the method compiles in today's raw output with stock 1.11.1): `242 rk.ay`,
  `241 jo.ah`, and 40 of the 67 H3 wrappers. Their stubs came from later source passes / older pipelines
  (repair, stub stages), not from the decompiler. The fork output of all 40 still compiles.

Unrelated pre-existing issue seen while testing (not a target, not fixed): `return true | a > 3;` decompiles to
`(boolean)(1 | (a > 3 ? 1 : 0))` with `-bto=0`.

## Changed output (stock vs fork, every revision 230-242; `method_diff.py`)

1,265 changed methods in total, none outside method bodies (no class header / field changes):
- fixed (stock had javac errors, fork has none): 1,234;
- improved (fewer errors; the rest are other causes, e.g. `Object[] cannot be converted to MenuEntry[]`): 27;
- changed-ok (both compile): 4:
  - `232 dp.bq`: `int var8 = readByte(); ... (byte)var8 ... var8 = readInt()` -> `byte var8` (no cast) and a separate
    `int var18` (fix 2: the byte local is no longer widened);
  - `237 om.fo`, `239 ds.fh`, `242 be.fq`: `Object varN = null; ... (String)varN` -> `String varN = null; ... varN`
    (fix 3);
- worse: 0.
Per-method lists: `md<rev>.json` in the work dir (not kept).

## What oracle-deob needs to pin the fork

- Build: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew allJar --no-daemon` at `163b5fb6` ->
  `build/libs/vineflower-1.11.1+local.jar`, sha256 `96eb806297a55475e2f5d7bf2a8dbf21820ff265bc1f29f1ba0d6035bffc0a96`
  (rebuilds are byte-identical). Same version string and API as 1.11.1.
- Replace `org.vineflower:vineflower:1.11.1` in `kotlin/decompile/build.gradle.kts` (and the daemon lib) by that jar
  (file dependency or a local Maven repo), pinned by sha256 and the fork commit; keep the options and `-thr=1`.
- Expect the decompile stage output to change exactly as listed above; repair-stage counts drop (object-to-concrete
  casts, int-to-boolean, byte-narrowing families) and stage hashes change, so reference runs / gates must be re-baselined.

## Tools (this directory)

`PrePass.java` (pipeline pre-passes), `decompile.sh` (pinned options), `rev.sh` / `all.sh` (pre-pass + decompile +
check per revision), `javac_check.py`, `stub_errors.py`, `h3_keys.py`, `summary.py`, `cmp_errors.py`, `method_diff.py`,
`try.sh` (one scratch class), `test_failures.py`, `test_actual.py`, `copy_tests.sh`, `per_commit_tests.sh`,
`stage_tests.py`, `clean.sh`.
