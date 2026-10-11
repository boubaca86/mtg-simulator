# Stage 9Q — Real Forge priority and combat with frozen registered main decks

**INDEPENDENTLY VERIFIED October 11, 2026.**

Two distinct, persistent process-backed Python actor instances consume only their own objective, own visible hand/table, a sorted immutable count of their **actual Forge registered main deck**, opponent's legally visible battlefield and referee-owned dual life totals. They do not receive any hand/library order, phase/turn/priority/stack, opponent private hand/decklist, game objects, hidden face-down identity, or arbitrary zone snapshot.

## Main integrated verification

- Pinned upstream `Card-Forge/forge` tag `forge-2.0.15`, exact SHA `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- GitHub CI: https://github.com/boubaca86/mtg-simulator/actions/runs/38137026093
- Source commit: `8bd59b51656c185c044288175355a2235d89c231`.
- Archived artifact `stage9q-registered-combat-38137026093-1`, ID `11701118361`.
- Actual Maven `BUILD SUCCESS`, exit `0`; **18 TestNG methods, 0 failures, 0 errors, 0 skips**; independent required-method Surefire XML gate **PASS**.
- Files: `forge_tests/Stage9QRegisteredCombatIntegrationTest.java`, `forge_tests/verify_stage9q_surefire.py`, `.github/workflows/stage9q-registered-combat.yml`, reused process worker `stage9o/combat_actor.py`.
- Referee freezes a per-player `TreeMap<String,Integer>` derived exclusively from `Player.getRegisteredPlayer().getDeck().getMain()` at game construction; subsequent actor observations use this frozen map, never reverse-engineer the initial deck from a mutable live library.
- Actual `PhaseHandler.mainLoopStep` still owns priority, turns, land actions, true mana payment, stack resolution, attack declaration, combat damage and both life totals; workers only suggest bounded intents. Includes deck-order noninterference and existing Stage9O attack/cast regressions.

## Additional independent registered-deck integration

- GitHub CI: https://github.com/boubaca86/mtg-simulator/actions/runs/38137212150
- Tested source commit: `b8c7bc90171f7fcde3c90ba546a20880a3ea9219`.
- Artifact `stage9q-forge-registered-priority-38137212150-1`, ID `11702300053`.
- **5/5 TestNG methods passed, Maven BUILD SUCCESS exit 0, independent XML gate PASS**.
- Additional tests build an actual `Game` with independently registered decks, seed all initial card objects from those registrations, verify true pool count conservation through Forge draws, real land drop, mana-paid creature spell stack and unblocked combat, and assert frozen decklist and opponent hidden-info invariance.

## Related Stage 9P checks, not equivalent to match registration

Stage9P authoritative stand-alone snapshot canary: https://github.com/boubaca86/mtg-simulator/actions/runs/38136570878 (6/6) reads true `RegisteredPlayer` initial pools. An **additional synthetic-zone snapshot** canary https://github.com/boubaca86/mtg-simulator/actions/runs/38137342547 passed 21/21 after correcting test-fixture count assertions. This 21-method suite reads test-fixture zones and **must not be treated as a substitute for actual registered deck ownership**; Stage9Q is the stronger integrated authority path.

## Open risks

- This is a narrow, test-only Forge integration, NOT a fully secure actor sandbox or an unrestricted Magic-playing AI.
- Actors are separate Python OS processes but not strongly confined with filesystem/network/process restrictions.
- `PlayerControllerAi` still owns an internal `AiController`. Multiple inherited non-priority choice/confirmation methods can inspect game state or make decisions through privileged engine routines; attack and priority entry points are overridden, but this is not a complete decision firewall.
- Face-down exile permission grant freshness, alias/cross-pile identity, re-exile new-object lifetime, timestamps, true costs/rollback, hidden-top leaks and other Stage9E threats remain unresolved.
- Test fixture cards, legal ability allowlists, attackers and available interactions are intentionally limited; no unrestricted triggers, choice/targeting system or general blocking, and no demonstrated strategically strong ISMCTS.
- User's Ultimatus life-below-zero, deck-out and custom scoring variant is not simulated by these ordinary Forge tests.
- No main merge, deployment, model promotion or reserved evaluation seed use.

## Next engineering target

Stage9R+: audit and replace inherited `PlayerControllerAi` non-priority strategic callbacks. Begin by failing closed for choices not yet supported by the restricted actor protocol, and test that known Forge gameplay still functions; replace one callback family at a time with explicit bounded actor requests validated by the referee. Keep end-to-end secret noninterference and OS sandboxing as explicit security acceptance conditions.

### Research

- Wizards of the Coast, official [Comprehensive Rules effective September 25, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt): 103.2a–103.3, 117, 305, 401–402, 406, 508–510 and 601.
- [Pinned Forge tag](https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676).
- [Cowling, Powley, Whitehouse, 2012 ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
