# Stage 9V — Guard inherited mulligan, targeting, face, splice, cost choices

**Verified with actual pinned Forge CI**, October 11, 2026:
https://github.com/boubaca86/mtg-simulator/actions/runs/38145866780

## Provenance and actual evidence

- Unmerged feature branch `expert-stage9a-forge-decision-pairs`, validated head
  `f837c2e5f0bd30d8edc78f0697cb32a5a5718576`.
- Pinned upstream `Card-Forge/forge` `forge-2.0.15`
  SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- **Maven BUILD SUCCESS**, exit 0. **29 TestNG methods executed,
  29 passed, 0 failures, 0 errors, 0 skipped**.
- Strict independent parsed Surefire gate: `PASS`,
  `full_magic_game:false`.
- GitHub artifact `stage9v-guarded-sensitive-choices-38145866780-1`,
  ID `11705220801` (source hashes, pinned SHA, test log,
  exact per-method XML, independent gate).
- Files: `forge_tests/Stage9VGuardedMulliganTargetsTest.java`,
  `forge_tests/verify_stage9v_surefire.py`,
  `.github/workflows/stage9v-guarded-sensitive-choices.yml`.
  Separate worker `stage9o/combat_actor.py` unchanged.

## New restricted-controller guards

The existing test-only Java referee extends `PlayerControllerAi`
and delegates only explicitly allowed real priority, land, restricted
creature-casting, and basic combat attacker decisions to TWO separate
persistent Python subprocess actors with the user's strict
observation allowlist.

These seven additional inherited strategic callbacks now
**immediately throw `SecurityException`**, with no fallback to
unrestricted `brains`, `ComputerUtil` or `SpellApiToAi`:

1. `mulliganKeepHand`: privileged AI mulligan judgment
2. `tuckCardsViaMulligan`: deciding which hand cards to return
3. `chooseTargetsFor`: arbitrary ability targeting
4. `chooseSingleCardFace` with explicit card-face list
5. `chooseSingleCardFace` with filtered card-face predicate
6. `chooseCardsForSplice`: modifying spells based on private cards
7. `chooseNumberForCostReduction`: unsupported optional reduction

Three newly executed TestNG methods verify (a) mulligan and
targeting guards reject choices and leave both player hands, life,
stack and IPC counters unchanged, (b) card-face/splice/reduction
choices refuse even when fed actual opponent concealed cards, and
(c) varying opponent hand and library secrets never changes
serialized actor observations or these denial outcomes.

**All 26 earlier Stage 9U TestNG cases also passed**, including
real Forge rules for turn/priority progression, land and creature
plays, limited attacking, fail-closed hidden pile, scry,
surveil, optional payment/replacement checks, and the exact
registered starting-deck-to-physical-library inventory
conservation after genuine Forge draws.

## Limitations

- These remain **test-only callback denial cases**. Unsupported
  cards triggering any guarded mandatory action can halt gameplay,
  rather than being fully supported via a new AI decision protocol.
- The inherited `PlayerControllerAi` has many **more** strategic
  callback families still to audit or replace. This run does
  **not** prove exhaustive hidden-information noninterference.
- The two Python processes are not yet in hardened OS memory,
  filesystem/network, or privilege sandboxes. `-I -S` alone is
  not an adversarial-security boundary.
- The current AI policy is a narrow heuristic, not expert, and no
  complete end-to-end match has been demonstrated.
- Production starting-shuffle/mulligan/game-over initialization
  and user's custom negative-life/library-empty win variant
  remain to be implemented.
- No merge, production deployment, training, reserved evaluation
  seeds, or promotion occurred.

### Audit of failed intermediate gate

Run `38145723762` had **29/29 Maven tests green** but a
strict XML verifier class-name typo. We corrected the class-name
matcher; final run `38145866780` had 29/29 Forge green **AND**
strict XML gate PASS.

## Stage 9W priorities

1. Replace direct inheritance of `PlayerControllerAi` with a
   least-privilege `PlayerController` adapter, or build an
   independent complete callback/override inventory and
   fail-closed policy for every remaining data-sensitive method.
2. Provide a **referee-approved mulligan/hand-selection protocol**
   with only own-hand and permitted own-deck information; no
   unsupported fallback to privileged Forge policy.
3. Harden independent actor processes (OS-level filesystem,
   network, memory and debugger isolation plus bounded
   authenticated IPC), then demonstrate adversarial secrecy.
4. Expand targeted spells/combat blocking only with explicit
   user-approved observations and real Forge legality.

Research: official rules
https://magic.wizards.com/en/rules ;
pinned Forge source
https://github.com/Card-Forge/forge/tree/forge-2.0.15 ;
Forge targeting specifications
https://github.com/Card-Forge/forge/wiki/Targeting .
