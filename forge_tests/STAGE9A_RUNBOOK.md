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


## Information-boundary acceptance matrix (Stage 9A review)

| Situation | May inspect identity? | Candidate handling |
|---|---|---|
| Own hand | Yes | Ordinary typed candidate |
| Public battlefield card | Yes, except concealed underlying faces | Ordinary public-face candidate |
| Opponent face-down battlefield permanent | No underlying identity | Only public 2/2 face-down characteristics and referee-authorized actions |
| Own or opponent unseen library top | No, absent explicit look permission | Never enumerate actual underlying card or abilities |
| Face-up exile | Yes | Ordinary candidate, subject to actual play permission |
| Face-down exile with look permission | Yes | Candidate only if legally playable |
| Face-down exile with blind play permission | Not necessarily | Opaque referee-validated action; do not expose underlying identity to policy |
| Opponent hidden hand/deck | No | No identity-derived candidates or predictions |

This matrix is an **engineering acceptance proposal**, not a substitute for Magic's rules or a complete Forge legality implementation. Rule 406.3b prevents using restricted casting permissions to inspect otherwise unviewable face-down cards; rule 708.5 protects hidden identities of face-down permanents. The baseline visibility patch is conservative and must not be merged until blind-play capability is handled.

### Paired-test observability contract

For each paired game, assert equality of actor-visible state, game phase, public priority, public legal-action descriptors, own hand, own decklist multiset, and observation history before invoking the policy. Vary one inaccessible hidden zone at a time, with separate deterministic policy RNG streams initialized to the same seed. Assert identical selected *public action descriptor* and identical policy telemetry, excluding referee-private logs. Never compare raw Card IDs, global timestamps, or engine-only ordering as if they were observable.

### Build evidence gate

Only accept a run when actual Surefire XML contains exactly six distinct named Stage9A methods with no skipped/error methods; invoke `python3 forge_tests/verify_stage9a_surefire.py --reports forge-gui-desktop/target/surefire-reports --mode baseline` and then `--mode patched` for the separate patched run. Keep separate report directories to avoid stale XML contamination.
