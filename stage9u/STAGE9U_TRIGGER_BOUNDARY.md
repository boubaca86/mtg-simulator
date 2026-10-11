# Stage 9U: bounded mandatory and optional trigger handling

This is a test-only extension of Stage 9T, stacked on the rejection-test repair
in PR #23. It must pass pinned Forge CI and receive independent review before
merging. It is not a completed match engine, an OS sandbox, or a policy promotion.

## Problem and source audit

At Forge `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` (`forge-2.0.15`),
`PlayerControllerAi.confirmTrigger` calls `brains.doTrigger` for optional effects.
Its no-stack path also uses `ComputerUtil.playNoStack`, which checks costs and
constructs `AiCostDecision`. Trigger ordering, trigger preparation, static
trigger execution and target setup provide additional inherited AI entry points.
Guarding only `confirmTrigger` leaves these paths open.

`WrappedAbility.resolve` checks trigger requirements and resolution limits,
requests optional confirmation only when a decider exists, and then calls
`playSpellAbilityNoStack(sa, false)`. Mandatory triggers normally have no decider.
Rejecting every no-stack invocation would therefore break compulsory effects.

Rules consulted: [official Comprehensive Rules](https://magic.wizards.com/en/rules),
effective September 25, 2026, particularly 603.3, 603.3b, 603.4 and 603.5.
Optional “may” triggers still enter the stack; their choice occurs at resolution.
Ordering multiple triggers controlled by one player is a player decision.

## Bounded change

`Stage9UTriggerBoundaryTest` inherits the corrected Stage 9T harness and all 23
tests. Six visibility/finality changes in the parent allow the new controller to
be substituted for every inherited test. No parent test assertions, reply
validation, actor messages, registered decks, card definitions or Forge sources
are changed.

The new controller audits both the ability shape and pinned trigger parameters
for exactly two cards: Soul Warden and Soul's Attendant. Their shared effect is
the target-free, cost-free `DB$ GainLife | Defined$ You | LifeAmount$ 1`.
Optional flags and decider identity must agree with the real card's trigger.
Costs, additional abilities, chains, altered amounts, foreign controllers and
other effect shapes are rejected before execution.

| Callback | Stage 9U behavior |
| --- | --- |
| `confirmTrigger` | Accept audited mandatory confirmation; throw for optional choice |
| `playSpellAbilityNoStack` | Resolve audited mandatory effect with Forge `AbilityUtils.resolve`; reject target setup and optional/unaudited effects |
| `orderSimultaneousSa` | Validate zero or one audited trigger; reject multiple triggers |
| `orderAndPlaySimultaneousSa` | Add that single wrapper to Forge's real stack without AI preparation |
| `playTrigger` | Stop unsupported static/no-stack trigger entry explicitly |
| `chooseTargetsFor` | Stop unsupported target selection explicitly |

The normal Forge wrapper, stack, life-gain effect and priority loop still execute.
No direct life-total adjustment substitutes for the effect. Optional choices are
not silently treated as “no,” and mandatory effects are not silently dropped.
An unsupported callback aborts the disposable fixture; it is not resumable game
state or a successful completed match. In particular, Forge may already have
removed entries from its pending trigger list before the controller is invoked.

## Evidence required

The Stage 9U workflow requires Java 21, the exact upstream SHA, real TestNG
execution, Maven exit zero and exactly 31 fully qualified named XML cases with
zero failures/errors/skips: 23 inherited regressions plus eight trigger cases.
Eight Python tests exercise the evidence gate; these are synthetic XML checks,
not gameplay evidence. CI archives the source hashes, both commit SHAs, Maven
log/exit, XML reports and gate result, including on failure.

The new Java tests cover:

- Real paid creature ETB, mandatory trigger stacking, both process-backed actors'
  priority passes, and actual +1 life resolution.
- Real optional trigger stacking followed by an explicit stop at resolution.
- A mandatory trigger resolving after its source leaves the battlefield.
- Identical actor observation/reply across opponent secret-zone changes and
  unchanged mandatory/optional handling in direct callback fixtures.
- Optional flag/decider forgery, no-stack bypass, altered amounts, costs, chains,
  additional abilities and foreign-controller rejection.
- Multiple-trigger ordering rejection before this controller adds any stack
  entry, plus static-trigger and target-setup denial.

The Stage 9T workflow also reruns its 23-case baseline and disabled-validator
mutation proof because its harness visibility changed. Negative assertions use
an explicit failure after the exception handler, preserving PR #23's fix.

## Limits and next work

This supports one narrow mandatory effect, not arbitrary mandatory triggers.
Simultaneous ordering, optional decision protocols, triggered mana abilities,
targeted/modal/cost-bearing triggers, draw/discard/scry, general stack interaction
and replacement-effect composition remain unsupported or unaudited. Existing
inherited mana and other controller paths still require further boundary work.
Separate Python processes do not establish OS-level isolation. No full match,
strategic strength, Ultimatus end condition, or complete hidden-information
boundary is claimed.

Next: design an actor protocol for one optional public choice, using only the
authorized observation surface, then test it through the real trigger lifecycle.
Do not add phase, turn, stack details or private state to actor input casually.

Reserved Stage 8P seeds `20261032` and `20261033` remain consumed. This stage does
not execute, retrain on, rerun, or retune those games.
