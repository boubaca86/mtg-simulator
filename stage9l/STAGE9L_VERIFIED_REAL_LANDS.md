# Stage 9L — Referee-validated land actions inside real Forge priority and natural turn progression

**Verified October 11, 2026.** GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38135135223 on isolated unmerged branch `expert-stage9a-forge-decision-pairs`, CI source SHA `eacbab23422531be72ed2b8a6ba0b59e99721d52`.

- Pinned upstream Forge `forge-2.0.15`, commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- `forge_tests/IsolatedForgeLandPriorityTest.java`: 6 actual Forge/TestNG methods executed, **6 passed, 0 failed, 0 errors, 0 skipped**, Maven `BUILD SUCCESS`, exit 0.
- `forge_tests/verify_stage9l_surefire.py`: independent strict positive XML gate **PASS**, 6 methods.
- `.github/workflows/stage9l-forge-land-priority.yml`: read-only branch-specific CI, pinned source assertions, source SHA-256 evidence, archived Maven and Surefire.
- Evidence artifact `stage9l-forge-land-priority-38135135223-1`, ID `11699464923`.
- Actor process: unmodified `stage9i/land_actor.py` launched as two independent, persistent `python3 -I -S -u` workers. Neither holds a Forge `Game`, `Player`, `Card`, internal ID or hidden referee API.

## Proven execution

Java referee subclass of `PlayerControllerAi` overrides the actual `chooseSpellAbilityToPlay` callback invoked by `PhaseHandler.mainLoopStep`. Each worker receives only objective, own hand/known decklist multiset, own table, opponent visible battlefield, and both current referee-controlled life totals; never phase/turn/priority/stack, opponent private hand, concealed original face, exact library order, unseen top, or other public zones. It replies `pass` or `play_land` with an index into its **own** hand.

The referee strictly validates the command schema (no extra fields; exact integer index), current Forge priority player, active turn, empty stack, valid own hand index and land face, and `Player.canPlayLand` / actual `SpellAbility.canPlay`. Only a genuine legal `LandAbility` is passed to Forge's normal `PlayerControllerAi.playChosenSpellAbility`, which resolves through `Player.playLand`.

Six executed methods demonstrated:
1. Pass/pass advances the real Forge phase, not after only one pass.
2. `PhaseHandler` naturally proceeds to the next player's turn with an automatic private draw, without calling `devModeSet` during progression.
3. Mutating an opponent's actual Forge hidden hand/library does not change the other player's restricted observation.
4. Separate workers: active player's legal Forest resolves as one land; opponent's early Island attempt is rejected as out-of-turn.
5. Two starting Forest cards: Forge accepts only one first turn, naturally progresses through a legal Island on the opponent's turn, returns for a future natural turn, resets land allowance, and accepts the second Forest.
6. Forged damage/life commands, unapproved extra fields, malformed/fractional/negative hand indices are rejected before the Forge state changes.

The initial `AITest.initAndCreateGame` fixture sets MAIN1 as in all prior Forge tests; **all later phase and turn progression in this test is genuine**.

## Limits and remaining tasks

- **NOT a completed AI-vs-AI Magic match.** No expert strategic policy, mana-producing abilities/cost payment, spell casting, targeting, instant-speed stack interaction, complete combat, or game-over resolution.
- The built-in `PlayerControllerAi` still supplies non-priority callbacks for some automatic choices and may need replacing with a comprehensively restricted controller; no global privacy proof is claimed.
- Process separation is tested but hardened operating-system filesystem/network/memory isolation is not.
- Stage9E security issues (re-exile object identity, stale permissions, cross-pile aliasing, real costs and rollback, hidden original representations) remain.
- No reserved evaluation seeds, no learning/training, no `main` merge, no production patch/deployment. The naive Stage9A patch remains unapproved.

**Next Stage9M:** add one carefully restricted **mana-costed, target-free creature spell** as an actor intent, with Forge-authoritative mana payment, legal timing, priority/stack resolution, counterexamples (insufficient mana, out-of-turn cast, no duplicated play) and visibility firewall. Test via same exact pinned Forge read-only CI before increasing to instant/target/combat behavior. Do not grant the AI general/public game-state access.

Research: [Wizards September 2026 Magic Rules](https://magic.wizards.com/en/rules) CR 117 and 305; [Forge pinned source](https://github.com/Card-Forge/forge/tree/forge-2.0.15); [Cowling et al. ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
