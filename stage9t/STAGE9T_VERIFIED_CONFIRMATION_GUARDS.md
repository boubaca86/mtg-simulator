# Stage 9T — Verified guarded bid, static, payment and replacement confirmations

**Verified on October 11, 2026, with authentic pinned Forge 2.0.15.**

## CI evidence

- Pinned Forge: `Card-Forge/forge` tag `forge-2.0.15` commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Successful GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38139664903
- Tested source commit: `065895875e945a03c7d4db840c699c9df1a7c77d`.
- Maven **BUILD SUCCESS**, process exit `0`.
- **23 real TestNG methods executed and passed, zero failures, errors or skips.**
- Independent strict Surefire XML verification gate **PASS**, `executed_methods:23`, no missing or duplicate method.
- Archived run artifact `stage9t-guarded-confirmations-38139664903-1`, id `11702750374`, contains Maven output, real XML, pinned Forge/feature hashes, source hashes, verification JSON.
- Files: `forge_tests/Stage9TGuardedConfirmTest.java`, `forge_tests/verify_stage9t_surefire.py`, `.github/workflows/stage9t-guarded-confirmations.yml`. Reuses `stage9o/combat_actor.py`.

## Actual development

Prior Stages 9R–9S introduced ten overridden `PlayerControllerAi` non-priority decision entry points that reject unsupported calls before Forge's `AiController` can inspect privileged game state:

- `chooseCardsForEffect`, `chooseCardsToDiscardToMaximumHandSize`, `chooseStartingHand`, `confirmAction`;
- `chooseSingleEntityForEffect`, `chooseEntitiesForEffect`, `chooseSingleSpellForEffect`, `chooseSpellAbilitiesForEffect`, `choosePermanentsToSacrifice`, `choosePermanentsToDestroy`.

**Stage 9T adds four further `SecurityException`-guarded decision methods**, totaling **14**:

- `confirmBidAction` — no internal AI bidding.
- `confirmStaticApplication` — no internal AI static-effect choice.
- `confirmPayment` — no internal AI decision about an optional or otherwise interactive payment.
- `confirmReplacementEffect` — no internal AI choice about applying an effect that may alter a game event.

These requests are not processed or silently approved, and **the corresponding richer card interactions remain unavailable until a bounded actor-decision protocol with referee-only validation exists**.

The first new adversarial test calls all four callbacks and confirms that they throw, do not request actor priority/attacks, and do not change either actor's allowed observation, life totals or the stack. The second adversarial test repeats calls with changed opponent concealed hand/library and secret-bearing arguments, confirming no private-zone dependence or leaks. Previous **21 genuine Forge regression methods** still execute and pass, including natural `PhaseHandler.mainLoopStep` priority/turn/draw, legal land, mana-paid creature on the actual stack, combat damage, frozen true `RegisteredPlayer.getDeck().getMain()` multisets and earlier secret noninterference. Both actor processes are distinct persistent Python OS processes, without receiving Forge engine handles.

## Critical limitations

- These are test-only guards, **NOT** a comprehensive secure two-AI sandbox and **NOT** full Magic: The Gathering gameplay. Throwing unsupported decisions means some real card effects will fail until purpose-built choice workflows exist; it is safer than quietly using an omniscient AI to make those decisions.
- Many other inherited controller decision pathways remain, including `confirmTrigger` (optional triggers), `playSpellAbilityNoStack`, `confirmReplacementEffect` variants outside this signature, `chooseCardsToDiscardFrom`, `arrangeForScry`/`arrangeForSurveil`, `chooseCardsToDelve`, `assignCombatDamage`, `specifyManaCombo`, target selection and `sideboard`. A complete callback audit and actor protocol mapping is not yet done.
- Separate workers are **not hardened OS sandboxes** with proven filesystem/network/process confinement.
- Stage9E unresolved hidden-exile permission life-cycle, cross-pile alias, re-exile object identity/timestamps, concealed originals and hidden library top, plus complex mana/nonmana costs and rollback.
- Restricted test actor can play narrow allowlisted basic lands / creatures and unblocked vanilla attacks; no unrestricted target spells, stack interaction, blocking or strategic ISMCTS.
- User's Ultimatus custom negative-life/deck exhaustion/score rules have not been exercised by the pinned standard Forge canary. No protected evaluation seeds consumed. No main merge, deployment or policy promotion.

## Next Stage 9U

Do a *small research-first verification* of additional inherited full-state decisions. Prioritize optional triggers and no-stack triggered-ability execution and discard/scry callbacks: understand actual pinned Forge `confirmTrigger` semantics and which forced triggers must still resolve; fail closed only optional strategic decisions that currently inspect hidden/full state, then add positive mandatory-trigger checks and negative secret-world tests. Preserve all 23 prior regressions; require exact named method strict Surefire XML gate, zero skipped, pinned Forge SHA, artifacts, reviewed frozen hashes before any merging. Do **not** treat lack of callbacks as full sandbox isolation.

## Sources

- Official [Wizards of the Coast Comprehensive Rules](https://magic.wizards.com/en/rules) (effective September 25, 2026) particularly priority 117, costs 118/601, triggers 603, replacement effects 614/616 and secrecy in 401–402/406.
- [Pinned Forge source](https://github.com/Card-Forge/forge/tree/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676), especially `PlayerControllerAi.java`, `PlayerController.java`, `PhaseHandler.java` and `CombatUtil.java`.
- [Cowling, Powley, Whitehouse ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/) for eventual hidden-information expert policy.
