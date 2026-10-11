# Stage 9Q — Real two-AI Forge combat with immutable owned registered-deck projections

**VERIFIED on October 11, 2026.** GitHub Actions:
https://github.com/boubaca86/mtg-simulator/actions/runs/38137026093

- Development branch `expert-stage9a-forge-decision-pairs`, run source commit
  `8bd59b51656c185c044288175355a2235d89c231`. **UNMERGED.**
- Upstream pinned `Card-Forge/forge` tag `forge-2.0.15` resolves to exact
  commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Real Forge Maven **BUILD SUCCESS**, exit **0**; **18 TestNG methods executed,
  18 passed, 0 failures, 0 errors, 0 skipped**. Strict independent XML
  validator returned `PASS` / `full_magic_game:false`.
- Archived evidence: `stage9q-registered-combat-38137026093-1`,
  artifact ID `11701118361` (source hashes, runner log, XML, gate JSON).
- Test code `forge_tests/Stage9QRegisteredCombatIntegrationTest.java`;
  independent gate `forge_tests/verify_stage9q_surefire.py`; pinned
  read-only CI `.github/workflows/stage9q-registered-combat.yml`.
  Actor policy `stage9o/combat_actor.py` is unchanged.

## Verified architecture advance

In contrast to Stage9O's hard-coded `DECK_A`/`DECK_B` lists, the Stage9Q
process-backed Forge controller receives **no decklist parameter**.
The referee creates two independently registered Forge `Deck` objects
and *immediately freezes* each player's own sorted card-count multiset
from `Player.getRegisteredPlayer().getDeck().getMain()`. All actual
priority and combat callbacks use the same referee-owned immutable
snapshot. The Python AI is given only: objective; own hand; frozen own
registered deck counts; own visible table; legally visible opponent
table cards; referee-owned **current dual life totals**.

The 18 real pinned Forge tests include all 16 Stage9O priority/turn,
automatic draw, basic-land, restricted creature spell, mana cost, stack,
unblocked combat and illegal attack checks, plus two new integration
guards:

1. The active process plays a real Forest while its own registered
   counts remain `Forest:12` and `Llanowar Elves:3`, and mutating
   the underlying Forge registered deck after freezing cannot add
   `Plains` to the actor's frozen snapshot.
2. Reordering actual hidden Forge libraries across two independent
   games does not change serialized actor observations or the two
   distinct process workers' decisions, and a hidden
   opponent `Counterspell` is excluded.

No library order, unknown top, opponent hand/registered deck, face-down
original, raw Card/Game refs, phase, priority, turn, stack or general
public-zone snapshot is sent. No AI may change life directly.

## Security audit / remaining critical blockers

The test controller still **inherits `PlayerControllerAi`**. Review of
pinned `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`
revealed 118 public methods, including non-priority callbacks
`chooseStartingHand` (uses `ComputerUtil.scoreHand` on candidate
hands), `chooseCardsToDiscardFrom` (may consult
`ComputerUtil.getCardsToDiscardFromOpponent`),
`chooseSingleCardForZoneChange` (delegates to
`brains.chooseCardToHiddenOriginChangeZone`),
`arrangeForScry`, `chooseTargetsFor` and numerous additional
choices. Stage9Q specifically overrides priority and declare
attackers/blocks but **does not guarantee inherited callbacks cannot
observe Forge private state or influence decisions through privileged
knowledge**. Therefore this is a successful test-only **view
projection**, **not a proof of comprehensive hidden-information
security or hardened three-process isolation**.

Furthermore, the AITest fixture initializes MAIN1 and populates
synthetic library cards independently from deck registration. Thus
all 18 tests prove the immutable registered multiset projected through
the real callback with genuine action resolution, **not** that every
initial fixture library is a perfectly reconciled implementation of
the registered starting deck. That remains to be verified at authentic
match initialization/sideboarding.

## Next engineering milestone

**Stage9R**: remove inherited full-information policy decisions from
the actor-facing controller, or explicitly gate and test all
non-priority callbacks (starting hand/mulligan, discards, selection
from hidden zones, targeting, modal/card choices, combat defense);
add independent adversarial hidden-world checks. Integrate a full
registered-deck-to-physical-zone inventory test with authentic
draws/shuffling so the frozen multiset is demonstrably consistent
with the actual starting library and initial hand. Keep actor
observation field set unchanged until user authorization.

No unreviewed Stage9A production patch or main merge, no training,
reserved evaluation seeds, promotion, or deployment.
Research: [official rules](https://magic.wizards.com/en/rules);
[Forge source](https://github.com/Card-Forge/forge/tree/forge-2.0.15);
[ISMCTS for partial observations](https://eprints.whiterose.ac.uk/id/eprint/75048/).
