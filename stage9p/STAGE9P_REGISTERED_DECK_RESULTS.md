# Stage 9P — Verified referee-frozen registered Forge decklists and hidden-order noninterference

**REAL PINNED FORGE CI SUCCESS:**
https://github.com/boubaca86/mtg-simulator/actions/runs/38136570878

At branch head `b0c284e2383c2ec7de9711b0ac30e8b0889a5c85`, pinned
`Card-Forge/forge` `forge-2.0.15` commit
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`,
Maven **BUILD SUCCESS**, exit 0. **6/6 actual TestNG methods executed,
0 failures, 0 errors, 0 skips**; strict independently parsed
Surefire XML gate **PASS**. Archived artifact
`stage9p-forge-registered-decks-38136570878-1`
ID `11701375526`, with source hashes, pinned upstream hash,
per-method XML, test runner output and gate JSON.

## What was implemented and tested

Source: `forge_tests/Stage9ORegisteredDeckSnapshotTest.java`;
worker: `stage9h/ai_actor.py`; strict XML verifier:
`forge_tests/verify_stage9p_registered_surefire.py`; read-only CI:
`.github/workflows/stage9p-forge-registered-decks.yml`.

A test-only, referee-side immutable `Snapshot` reads actual
`Player.getRegisteredPlayer().getDeck().getMain()` for exactly two
independently registered starting decks, sums `PaperCard` names
into a sorted multiplicity map and immediately freezes the data.
Each subprocess receives only its **own** starting decklist multiset,
own legally identifiable hand and table, opponent's visible table
cards, its objective and referee-owned life totals. No draw order,
hidden top, turn, phase, priority, stack, opponent deck or hand,
card IDs, incidental public zones or engine objects are serialized.

Six genuine Forge test methods passed:

1. **Exact own counts and ownership:** actor A's Forest×4, Shock×2,
   Llanowar Elves×1 versus B's Island×3, Counterspell×2 and
   Grizzly Bears×2, all backed by separate registered `Deck` main
   card pools. Neither actor receives the other's list.
2. **Immutable once frozen:** the snapshot rejects local map writes
   and is unaffected if the underlying registered deck's
   mutable current pool later changes.
3. **Real Forge card draw:** drawing from a fully seeded registered
   seven-card library changes only the owner's hand and the live
   library size. The starting deck count does NOT shrink; the
   opponent's observation remains byte-identical.
4. **Draw-order noninterference:** two actual games use different
   library insertion orders but identical registrations and visible
   state. Both per-actor observation bytes match exactly.
5. **Distinct subprocesses:** two persistent Python actor workers
   have distinct PIDs and receive different allowed observation
   hashes, with only their own registered deck counts.
6. **Hidden opponent state and face-down protections:** opponent
   hand and library changes cause zero change to the other
   observation; face-down battlefield card names remain omitted.

This also establishes a safer ownership and frozen-list architecture
than Stages 9H–9O fixtures that injected simplified decklist literals.

## Explicit limits

- This is a verified **standalone referee-side integration canary**:
  its registered-deck snapshot code has not yet been substituted into
  the land/creature/spell/combat controller in a complete match.
- Full registered deck reconciliation at authoritative game startup
  (sideboarding, commander, replacement, registration-specific
  modifications) remains to be integrated and threat-reviewed.
- Actor subprocesses are distinct but OS sandboxing and non-priority
  Forge `PlayerControllerAi` callback security remain unresolved.
- Snapshot identity and life are permitted; opponent's decklist,
  exact library order, hidden card positions and arbitrary other
  public data remain explicitly prohibited.
- No full game, general combat policy, training, reserved evaluation
  seed, merge, deployment or new production permission is claimed.
- Original Stage9O separately verified **16/16 pinned Forge combat
  tests**: https://github.com/boubaca86/mtg-simulator/actions/runs/38136373646,
  artifact `stage9o-forge-combat-attack-38136373646-1` (11700964167).
  That system is still a narrow test-only attack policy.

## Next engineering priority

Stage 9Q: integrate the registered-deck frozen snapshot into the
**same actual process-backed Forge `PhaseHandler.mainLoopStep`
controller** used for real actions. Assert no hand-created/proxy
decklist arguments can reach the actor, then audit inherited
non-priority Forge AI callbacks and replace or explicitly gate any
that might inspect hidden cards. Add adversarial hidden-world
tests and OS filesystem/IPC isolation before calling the
three-process architecture secure.

Official MTG comprehensive rules (effective 2026-09-25):
https://magic.wizards.com/en/rules
Research on imperfect-information game AI:
https://eprints.whiterose.ac.uk/id/eprint/75048/
