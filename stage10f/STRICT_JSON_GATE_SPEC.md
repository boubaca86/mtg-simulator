# Stage 10F — strict actor JSON boundary: acceptance criteria (research-only)

## Scope and references

- Wizards of the Coast, *Magic Comprehensive Rules*, effective 2026-09-25: https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
- CR 400.2: library and hand remain hidden zones; face-down objects in otherwise public zones do not reveal their faces.
- CR 509.1 and 702.111: the authoritative referee declares legal blocks; menace requires at least two blockers.
- CR 510: combat damage assignment and resolution are authoritative game operations.
- Pinned Forge 2.0.15 commit 4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676, CombatUtil.canBlock / validateBlocks and PhaseHandler.declareBlockersTurnBasedAction.
- Cowling, Powley, Whitehouse (2012), *Information Set Monte Carlo Tree Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/ ; observations must not grant hidden information.

## Concrete finding

All four current test-only actors (stage9w/opening_hand_actor.py, stage10b/action_plan_actor.py, stage10d/multi_block_actor.py and stage10e/menace_defense_actor.py) currently call Python's default `json.loads(raw)`. By default, Python accepts duplicate object keys (last key wins) and the non-standard numeric literals NaN, Infinity and -Infinity. An actor may therefore accept an ambiguous wire message, even if its post-parse visible-field whitelist is exact.

This is a protocol ambiguity, **not evidence of an actual hidden-information leak**. The trusted Forge referee remains the sole authority for legal moves.

## Required Stage 10F change

1. Parse every inbound JSON observation with an `object_pairs_hook` that rejects duplicate keys at every nesting depth, and a `parse_constant` callback that rejects NaN/Infinity/-Infinity.
2. Preserve the exact existing allowed projection: objective, own_cards {hand, decklist_multiset, table}, opponent_table, life_totals {self, opponent}. No new fields, hidden identities, stack, turn or priority information.
3. Preserve the strict 16,384-byte per-message bound and fail closed with no JSON response to malformed requests.
4. Keep exactly two persistent independent actor processes per test and one authoritative Forge referee. Do not add a sidecar process or a second referee.
5. Reject duplicate keys in both normal action-plan and own_opening_hand request envelopes.
6. Preserve normal responses for valid input and all 51 Stage 10E genuine pinned Forge TestNG cases.

## Negative and positive protocol vectors

Reject: duplicate top-level objective; duplicate own_cards.hand; duplicate face-up card name; duplicate face-down appearance; duplicate life_totals.self; duplicate opening-envelope request; life_totals.self = NaN; life_totals.opponent = Infinity; an extra opponent_hand or phase key; any face-down card identity.

Accept: valid normal observation; valid opening-hand envelope; two distinct blockers proposed for a single visible attacking creature; two independent one-to-one blocks for two visible attackers.

## Release gate

A Stage 10F implementation is **not verified** until CI has run on the exact implementation commit and shows all actor protocol vectors PASS, the same pinned Forge SHA, exactly 51/51 TestNG methods in Surefire XML with zero failures/errors/skips, Maven exit 0, and an uploaded evidence artifact. Never interpret a prior Stage 10E success as proof of an untested Stage 10F change. Do not merge or deploy.

## Current status

Research and acceptance criteria only. The actual Stage 10F actor code and CI changes have not been committed or verified.
