# Stage 10E — Menace Blocking, Narrow Native Forge Verification

**VERIFIED** 2026-10-11 for the stated limited behavior. Feature branch
`expert-stage9a-forge-decision-pairs` remains UNMERGED.

## Reproducible evidence

- GitHub Actions run: https://github.com/boubaca86/mtg-simulator/actions/runs/38170238899
- Tested feature commit: `90e75229a92b3908ff5f3017cac83aa8a329e187`
- Engine: Card-Forge/forge `forge-2.0.15` exact peeled commit
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
- Maven exit: `0`; `BUILD SUCCESS`
- Genuine native Forge TestNG: **51/51 passed; 0 failures; 0 errors; 0 skipped**
- Independently checked exact 51-method Surefire XML:
  `{"executed_methods":51,"full_magic_game":false,"gate":"PASS"}`
- Independent Stage10D and Stage10E actor subprocess/IPC canaries: **PASS**
- CI evidence artifact `stage10e-menace-blocking-38170238899-1`,
  GitHub artifact ID `11713087814` (logs, exact XML, source checksums,
  pinned engine SHA and gate result).

## Feature and constraints

- Exactly two independent persistent external Python actor OS processes;
  exactly one trusted Forge referee remains authoritative for every action,
  mana/cost, shuffle/draw, stack, priority, combat and life.
- Actor observations contain ONLY own objective, own hand, visible table,
  immutable actual registered starting deck card-name/count multiset,
  visible opposing table and both referee-maintained life totals.
- No actor observes phase/turn/priority/stack, hidden opponent cards/deck,
  library order/top, original face-down identity, or raw Forge Card/Game.
- Passive blocking proposals: if actor sees two eligible-looking own Bears
  and just one visible opponent Bear, it proposes both as blockers of that
  one target; only the referee determines whether the attacker currently
  attacks and actually has menace or another requirement.
- Real Forge tests include **solo-block rejection and consequent combat
  damage** and **two-blocker eligibility and consequent prevention of
  player damage**. Stage10E inherits all 49 Stage10D tests, and the
  two-blocker Stage10D regression was hardened to require both assignments
  in the SAME native declaration callback.
- Nested observations undergo strict allowlist shape checks for face-up
  and face-down table records, names/counts, life totals and hand shape.
  The Stage10E Python canary covers two independent processes, passivity,
  face-down concealment, malformed nested fields and two-target fallback.

## Research anchors

- Official WotC Comprehensive Rules effective September 25, 2026:
  https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
  Especially CR 509.1a-e, 510.1a-e, 510.2 and menace (702.111).
- Exact pinned Forge `CombatUtil.canBlock`, `CombatUtil.validateBlocks`,
  `CombatUtil.getBlockCost`, `PhaseHandler.declareBlockersTurnBasedAction`.
- Cowling, Powley and Whitehouse (2012), Information Set Monte Carlo
  Tree Search: https://eprints.whiterose.ac.uk/id/eprint/75048/

## Limitations (not validated)

This is **not** a complete playable or autonomous full Magic game, a
hardened OS sandbox, or a trained perfect-information-independent expert.
Complex targeting, inherited PlayerControllerAi privileged callback audit,
must-block and pay-to-block interactions, multi-creature combat damage
allocation, stacked effects, exile object identity/hidden top, production
deployment, end-to-end winner determination and the user's custom Ultimatus
negative-life/library-exhaustion format remain out of scope.

No merge, deployment, model training, competitive promotion, or protected
evaluation seed use was performed.
