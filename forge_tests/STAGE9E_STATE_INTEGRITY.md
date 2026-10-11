# Stage 9E — Forge-backed blind exile-pile state integrity (UNVERIFIED)

**Status on 2026-10-11:** Nine new TestNG test methods and an independent
Surefire XML verifier are committed to
`expert-stage9a-forge-decision-pairs`. **The nine Java tests have NOT yet
been run in CI.** An attempted new GitHub Actions workflow write was
blocked by the tool's safety checks. No workflow, green build or merge
should be inferred. This document intentionally separates source work
from verified execution.

## Why Stage 9E exists

Stage 9D's six passing Forge-backed tests demonstrated a test-only
opaque grant over real `Game`, `Card`, `CardView`, and `PlayerZone`.
But its private `Grant` held a snapshot of `Card` references and its
`redeem` accepted a revision **supplied by the caller**. It did not
recheck that the cards were still in the original exile pile, still
face down, and still unreadable by the actor. A card could leave exile,
turn face up, or become lookable after grant issuance, and a stale grant
could still be redeemed in that test-only model. That is a potential
time-of-check/time-of-use failure in a future production adaptation;
it is **not evidence of a currently exploitable production Forge bug**.

## Implemented, not yet executed

- `forge_tests/BlindPileStateIntegrityTest.java`: nine test-only
  TestNG cases backed by actual Forge objects.
- `forge_tests/verify_stage9e_surefire.py`: fail-closed parser
  requiring exactly nine named executed cases, with no failures,
  errors, skips, duplicates, or missing reports.
- Referee-owned public pile registration, a monotonically increasing
  public action sequence, private epoch, actual `Game.getTimestamp()`,
  immutable snapshot, live exile-zone membership and face-down/visibility
  rechecks before referee-only random selection.
- Tests: (1) public-action noninterference across concealed identities,
  (2) no unauthorized or 406.3b quality-restricted blind grant,
  (3) exile removal revokes, (4) face-up transition revokes,
  (5) newly granted look permission revokes **even if timestamp does
  not change**, (6) private pile replacement revokes without epoch
  or timestamp change, (7) costs confirmation and rollback marker,
  (8) actor binding, fresh action ID and single-use replay resistance,
  (9) separation of two registered exile pile groups.

## How to obtain actual evidence

Run on an isolated disposable checkout of
`Card-Forge/forge` tag `forge-2.0.15`, Java 21 and Xvfb.
Install the Java fixture in
`forge-gui-desktop/src/test/java/forge/ai/BlindPileStateIntegrityTest.java`.
From the Forge checkout:

```sh
xvfb-run -a mvn -B -ntp -pl forge-gui-desktop -am test \
  -Dtest=BlindPileStateIntegrityTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

From the simulator checkout, require Maven exit 0 and run:

```sh
python3 forge_tests/verify_stage9e_surefire.py \
  --reports /path/to/forge-src/forge-gui-desktop/target/surefire-reports
```

Preserve source SHAs, logs, XML and verifier output. Do not treat
annotation counts or Maven success without actual executed cases as
security evidence.

## Canon and implementation boundaries

Research basis: official September 25, 2026
[Magic Comprehensive Rules](https://magic.wizards.com/en/rules)
406.3a, 406.3b, 406.4, 708.5; pinned
[Forge Card](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/Card.java),
[CardView](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/CardView.java),
[PlayerZone](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/zone/PlayerZone.java),
[ComputerUtilAbility](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java);
[Cowling et al., ISMCTS (2012)](https://eprints.whiterose.ac.uk/id/eprint/75048/).

The actor sees only the five public fields, never a `Card` reference,
hidden card ID, secret identity, mana cost, or private pile membership.
The referee uses a trusted game object, not actor-supplied revisions.
Real integration must make issuance, cost payment, mutation detection
and redemption an **atomic referee-side transaction**; the current
`costsPaid` and `refereeAuthorized` booleans remain **fixtures**.

**Unsolved, production-blocking:**
1. A real named card/effect must authorize the blind pile choice
   independently of card identities, with linked-ability and
   exile-chronology provenance and rules 406.3b/406.4 respected.
2. Actual Forge spell/ability costs, illegal action rollback and
   priority/stack timing must be used, not trusted booleans.
3. A single authoritative mutation/version service must track all
   relevant zone, reveal, permission, shuffle and pile changes.
   `Game.getTimestamp()` alone is insufficient (the new look-grant
   test specifically checks this).
4. Prove the real production policy, action selection, and telemetry
   cannot access concealed `Card` internals or leak secret decisions.
5. Validate foretell, adventure, morph and special permissions
   separately; never suppress legitimate legal blind actions with a
   naive visibility-only filter.

No Stage9A merge, AI-vs-AI games, policy training, or reserved
evaluation seeds were used in this Stage9E source work.
