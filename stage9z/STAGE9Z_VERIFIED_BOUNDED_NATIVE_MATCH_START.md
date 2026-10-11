# Stage 9Z — Bounded actual Forge Match.startGame with two isolated AI processes

**Verified October 11, 2026 — GitHub Actions SUCCESS**
https://github.com/boubaca86/mtg-simulator/actions/runs/38148750609

- Feature branch `expert-stage9a-forge-decision-pairs` **UNMERGED**.
- Verified execution SHA `a2be1731359a35a202fa038b0ce7ef649d265fe1`.
- Upstream pinned `Card-Forge/forge` tag `forge-2.0.15`,
  exact checked source commit
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Actual Forge Maven **BUILD SUCCESS**, exit 0; **40 TestNG methods
  executed, 40 passed, zero failures/errors/skips**.
- Independent Surefire XML verifier `PASS`, explicitly records
  `full_magic_game: false`.
- Archived evidence
  `stage9z-native-match-start-38148750609-1`,
  artifact ID `11706161104` (68052 bytes), containing pinned hashes,
  Maven log, exact per-method XML and gate JSON.
- Test code `forge_tests/Stage9ZNativeMatchStartBoundedTest.java`;
  strict verifier `forge_tests/verify_stage9z_surefire.py`;
  workflow `.github/workflows/stage9z-native-match-start.yml`;
  unchanged two-worker `stage9w/opening_hand_actor.py`.

## Real Forge initialization path proved

Two separate persistent process-backed AI players are attached to
**actual Forge** `Player` objects. Each player owns a **distinct
legal-size, 60-card registered Constructed deck** consisting of
basic lands and a narrowly tested set of four-of creatures/spells.
The trusted Java referee freezes each player's original own-deck
name/count multiset from registration.

Unlike stages 9X and 9Y, the two new Stage9Z methods actually invoke
`game.getMatch().startGame(game, startGameHook)`. That native Forge path:

1. Calls `Match.prepareAllZones` to populate genuine private physical
   libraries from registered decks, and shuffles via Forge
   `Player.shuffle`. The controller explicitly overrides
   `cheatShuffle` to prevent inherited AI reordering the private
   library.
2. Calls `GameAction.startGame`: selects the game’s starting
   player through Forge's first-game election. The restricted
   controller supplies an explicit constant own-player choice for
   `chooseStartingPlayer` (no hidden-card reasoning or widened actor
   observations), and disables inherited privileged
   `complainCardsCantPlayWell` deck analysis.
3. Runs native Forge `MulliganService` with London mulligan rules.
   Each independent Python process receives only its own permitted
   observation, proposes keep/mulligan and ranks only own-hand positions;
   referee maps this to legal current `Card` objects and
   controls true card-zone movement and counts.
4. Completes the actual opening-hand action and new-game state setup,
   sets the starting player and enters
   `PhaseHandler.setupFirstTurn` at `UNTAP`.
5. Invokes a deliberately bounded first-turn hook that throws a
   **test-only sentinel**, preventing
   `PhaseHandler.mainGameLoop` from consuming uncontrolled
   time. The sentinel is caught solely by the test; this is an
   **intentional partial match-start lifecycle**, NOT proof of
   a completed game.
6. In a separate test, after that same genuine native setup,
   manual bounded calls to actual
   `PhaseHandler.mainLoopStep` progress normally.
   The starting player **skips its first draw** and the second
   player draws once on their first turn under 2026 CR 103.8a.
   Both independent process-backed priority controllers execute
   and real registered-deck inventory is conserved.

Two new TestNG methods passed:
- `realMatchStartPerformsSixtyCardPrepMulliganAndNativeFirstTurn`
- `realMatchStartThenNormalTurnsRespectFirstPlayerDrawAndPrivacy`

All 38 Stage9Y and older methods also passed, including authentic
mulligans, earlier land/creature/attack canaries and fail-closed
hidden-information callbacks.

## Actor permissions, unchanged

Each actor receives only game objective, own current hand and
legitimately known cards, own immutable registered starting-deck
multiset **without library order**, opponent cards legally visible on
the table, and current dual life totals originating from referee
`Player.getLife()`. No generic public state, phase, turn, priority,
stack, private opponent cards/deck, unknown library top/order,
`Card` refs, arbitrary logs or hidden original representations.
Actor cannot directly change life.

## Scope and open security/functional gaps

- This is **not a complete AI-versus-AI match** or a complete
  `Match.startGame` to match-end execution. The sentinel stops
  native initialization at the first-turn hook and
  skips endgame/match cleanup; it is test-only.
- The starting-player chooser's permitted action is a constant
  own-player choice decided by the trusted test controller,
  not yet a general isolated actor choice protocol.
- The test-only player controller still subclasses
  `PlayerControllerAi` and does **not** establish complete
  fail-closed coverage for every inherited privileged callback.
  Earlier denial guards mitigate only enumerated families.
- Distinct Python process IDs do not prove OS-level network,
  filesystem, debugger, process memory or side-channel isolation.
- Policy is narrow heuristic, not expert AI; no general targeted
  spells, complete blocks/combat strategy, expanded arbitrary card
  support or complete game-end conditions were tested.
- Stock Forge zero-life loss differs from user’s custom
  **negative life permitted / higher life wins after libraries are
  exhausted** format. It remains unimplemented.
- Stage9E concealed-object aliases, stale authorization, re-exile
  406.7 new object identities, cost rollback and RNG remain open.
- No main merge, unsafe production patch, deployment, training,
  reserved evaluation seeds or policy promotion.

### Failed intermediate compile and correction

First run `38148636931` failed Java compilation because the
Stage9Z test redundantly declared an inherited guarded
`chooseStartingHand` override already present from Stage9R.
No actual tests ran in that attempt. We removed the duplicate
without weakening the prior guard. Full rerun
`38148750609` passed all 40 real tests and independent evidence
check at the verified SHA above.

## Next engineering milestone — Stage 10A

Safely run a **bounded, coherent actual match from real
`Match.startGame` through two or more genuine turns**, including
real spells and combat, with referee-controlled termination and
strict tests for inventory, win conditions and no hidden leaks.
Do not mistake the test sentinel for finished match support.
Parallel priorities: OS-level actor sandboxing and complete
least-privilege Forge controller replacement (avoid inherited
privileged non-priority callbacks); then real policy search over
information sets without global card knowledge, and user-format
endgame implementation with explicit independent tests.

References:
- Magic rules effective 2026-09-25, CR 103 and 117:
  https://magic.wizards.com/en/rules
- Pinned Forge source:
  https://github.com/Card-Forge/forge/tree/forge-2.0.15
- Cowling, Powley, Whitehouse, ISMCTS:
  https://eprints.whiterose.ac.uk/id/eprint/75048/
