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


## GitHub Actions handoff (workflow-only registration)

- Stage9A feature branch has `.github/workflows/stage9a-forge-information-firewall.yml`.
- **Draft PR #22** (<https://github.com/boubaca86/mtg-simulator/pull/22>) contains ONLY that workflow file from a separate branch rooted at `main`, for independent review.
- GitHub requires a `workflow_dispatch` workflow file on the default branch to register a manually runnable workflow. After reviewing and merging **only PR #22**, visit **Actions → Stage 9A — Forge Hidden-Information Tests → Run workflow** and choose **`expert-stage9a-forge-decision-pairs`**, **not `main`**.
- This is a targeted Java test run, not an AI game simulation. Both Maven runs use `xvfb-run` for Forge desktop initialization.
- The upstream patch is applied only to the temporary runner checkout, never committed to Forge or merged into the simulator. Baseline and patched reports are separate artifacts.
- If the workflow fails, download its evidence artifact; retain the exact logs and method results before changing any patch or tests. **Do not merge Stage9A AI changes without independent review and passing proofs.**


## Stage 9B: independent face-down battlefield canary (unverified)

Stage 9A's six previously passing TestNG regressions remain unchanged and are still gated by `verify_stage9a_surefire.py`. The Stage9A feature branch adds a **separate** Java class, `forge_tests/FaceDownBattlefieldPrivacyTest.java`, with exactly two TestNG methods, followed by `verify_stage9b_surefire.py`. It runs **after** the patched Stage 9A six-test gate in the same manual workflow. New failure results in a red workflow; Maven XML and logs are saved under `stage9a-evidence/face-down-battlefield/`.

The new probes (i) swap the secret identity beneath an identical opponent-controlled face-down battlefield shell and require an unchanged actual Forge AI decision, and (ii) verify the opponent's hidden card does not give the actor a usable original-face turn-up action from `ComputerUtilAbility.getSpellAbilities`. The canary does **not** establish that other heuristic evaluators never consult hidden original attributes; it tests two specific observable consequences only.

**Known scope gap:** Magic Comprehensive Rules 406.3a, 406.3b and 406.4 distinguish playable face-down exile cards, quality-restricted casting with look permissions, and random pile selection. Before integrating the Stage9A visibility filter into production, create referee-validated opaque legal-action capabilities that preserve authorized blind pile actions without disclosing private cards. Audit `PlayerZone.getCardsPlayerCanActivate` and `Card.getAllPossibleAbilities`, including owner face-down turn-up permissions and legality of look-only/play-only library effects.

**Current evidence:** the previously recorded GitHub run #3 proved Stage9A six-test red/green (baseline 3 failures, patched 0 failures). Stage9B tests were committed **after** that run and must be compiled and executed separately before making any Stage9B success claim.

References: [official MTG Comprehensive Rules](https://magic.wizards.com/en/rules), [pinned Forge `Card.java`](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/Card.java), [pinned Forge `CardView.java`](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/CardView.java), [Information Set MCTS research](https://eprints.whiterose.ac.uk/id/eprint/75048/).
