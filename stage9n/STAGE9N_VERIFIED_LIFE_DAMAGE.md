# Stage 9N — Genuine Forge target-free damage spells and shared life totals

**VERIFIED in independent pinned Forge CI on October 11, 2026.**

GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38135863492
Feature head `7e0b0e93d19b695a7d923cfbec5bf13e9763eb38`.
Pinned Forge tag `forge-2.0.15`, exact commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.

**Maven BUILD SUCCESS, exit 0; 15 TestNG methods executed, 15 passed,
0 failures, 0 errors, 0 skips; independent Surefire XML gate PASS.**
Evidence artifact: `stage9n-forge-life-spell-38135863492-1`
(ID `11700259488`, roughly 60 KB, Maven log, per-method XML, SHA and gate).

## Genuine Forge functionality exercised

- Two separate persistent Python worker processes (`stage9n/stack_actor.py`),
  with only objective, own hand/decklist multiset, own visible table,
  opponent cards visibly placed on table, and **referee-owned dual life totals**.
  No opponent hidden hand, library order, unrevealed top, concealed originals,
  stack, turn/phase, priority or generic public information is transmitted.
- The trusted Java referee
  (`forge_tests/IsolatedForgeLifeSpellPriorityTest.java`) receives
  only bounded `pass`, `play_land`, `cast_creature` and
  `cast_sorcery` intents, with canonical nonnegative hand indexes.
  The only approved sorcery is `Flame Rift`, an actual pinned Forge
  card script: `{1}{R}` Sorcery, deals 4 damage to **each player**.
  Referee checks actual turn and priority, current own hand,
  `canCastSorcery`, empty stack, exact name, non-targeted plain mana
  cost, `SpellAbility.canPlay` and
  `ComputerUtilCost.canPayCost`. Only a valid Forge ability is
  returned to the actual `PhaseHandler.mainLoopStep` pathway.
- Authentic Forge cost payment taps Mountain and Island, queues the
  spell on the stack, permits the other AI to pass priority, then
  resolves damage. Both life totals go **20 → 16**. Each actor's
  next observation receives the new life numbers from real
  `Player.getLife()`, never an actor-supplied life claim.
- Genuine two-player progression demonstrates an AI on its turn
  casting one Flame Rift, then the other AI on its natural turn casting
  its own Flame Rift, leaving each at **12 life**.
- Negative cases: two Islands without red mana leave hand, mana and
  both life totals unchanged; the nonactive player cannot cast
  a sorcery; forged commands and unsupported card types remain
  rejected. All 11 previous Stage9M creature/land tests remain green
  under the stricter Stage9N observer.

## Limitations

**This is NOT a full self-contained Magic match.** It is a test-only
narrowly allowlisted land/creature/sorcery policy. It has no
unrestricted card abilities, general target selection, complete combat
AI, full termination/evaluation, or expert strategy.

- Actor processes are independent but not hardened from each other
  at OS filesystem, memory/debugging or network levels.
- The inherited Forge AI still supplies non-priority callbacks that
  may inspect privileged engine state. Those require replacement
  before claiming a robust three-party hidden-information boundary.
- The test fixture uses simplified *known decklist multiset* inputs,
  which do not exhaustively correspond to the actual 12-card
  synthetic libraries or all card fixtures in a test. **Next work
  should derive the actual starting decklist from trusted registered
  Forge deck data**, preserve counts without disclosing card order,
  and verify it against genuine draws, transformations and copies.
- The referee's implementation uses normal Forge life-loss/game-over
  semantics. The user's custom negative-life, winner-by-life,
  library-exhaustion game variant is a separate unimplemented rule
  module; avoid misrepresenting this demonstration as custom-format
  completion.
- Stage9E hidden-state permissions, cross-pile alias, re-exile
  identity, stale grants, cost/rollback and random-selection threats
  remain unresolved for arbitrary cards.

## Research

- [Official Magic rules (effective September 25, 2026)](https://magic.wizards.com/en/rules)
  notably CR 117, 305, 601 and 608.
- [Pinned upstream Forge source](https://github.com/Card-Forge/forge/tree/forge-2.0.15),
  exact commit above; `forge-gui/res/cardsfolder/f/flame_rift.txt`.
- [Cowling, Powley and Whitehouse ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/),
  for eventual hidden-information planning.

## Next work: Stage 9O

Prioritize verified **real starting decklist ownership/count projection**
and test that per-actor snapshots cannot reveal draw order while game
cards change location (draw, cast, graveyard, shuffled library). Also
audit inherited `PlayerControllerAi` for privileged non-priority
decision callbacks. Next bounded gameplay expansion can introduce
explicitly player-observable target options and referee-only validation,
then combat; do not widen actor view by assuming all zones are approved.

No main merge, production deployment, evaluation seeds or training was
performed by these stages.
