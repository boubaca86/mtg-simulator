# Stage 9A Forge integration test runbook

This runbook is a test procedure, not a claim that tests passed. Target: Card-Forge/forge tag `forge-2.0.15`.

## Prerequisites

- Java 17+ and Maven, plus a clean Forge checkout at the pinned tag.
- Copy `forge_tests/ObservedOnlyNoninterferenceTest.java` to `forge-gui-desktop/src/test/java/forge/ai/ObservedOnlyNoninterferenceTest.java`.
- Keep the AI source patch in `forge_patches/stage9a_computer_util_ability_visibility.patch` separate from the baseline run.
- The test class contains six TestNG methods; **zero tests executed means failure**, regardless of Maven exit status.

## Baseline (unpatched Forge)

From the Forge root:

```sh
mvn -pl forge-gui-desktop -am test -Dtest=ObservedOnlyNoninterferenceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Collect the Surefire XML for `forge.ai.ObservedOnlyNoninterferenceTest`. Check the **executed** test count, not only the discovered methods. Expected outcome: three candidate-enumeration security tests expose the existing leak and fail; one public-card sanity test should pass. The two live-decision paired tests must be assessed from actual results, not assumed to pass.

## Patched run

Apply the proposed patch to the exact pinned source, then rerun the same Maven command. Require **six tests executed**, no compilation errors, no skipped methods, and no failures. If a test fails, retain its log and do not merge.

## Mandatory review before merge

1. Check legitimate casting/playing from hidden zones (including cases with legal blind actions). The current visibility patch may reject legal actions and is not production-ready.
2. Audit face-down battlefield cards: their public face-down shell is visible, but original identity and abilities are not automatically available to the actor.
3. Confirm paired-decision observations include every relevant public fact, not just the scenario-specific subset.
4. Confirm the Java fixture uses independent RNG and never consumes reserved gameplay-evaluation seeds.
5. Have a reviewer inspect patch and test logs. No training, match dispatch, PR merge, or policy promotion based solely on this runbook.

Forge's own testing docs use `mvn -pl forge-gui-desktop -am verify -Dtest=...`; its issue #11183 documents green builds that execute zero tests. For this reason, actual TestNG/Surefire XML execution counts are a hard gate.
