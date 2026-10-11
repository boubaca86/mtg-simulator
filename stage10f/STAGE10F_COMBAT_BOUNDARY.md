# Stage 10F: bounded visible combat damage policy

Built from the live Stage 10D/10E development head
`a82389282f1cd3f42d2f309166356b3f95d41c38`. That head includes both Stage 10D's
49-case suite and Stage 10E's 51-case menace suite. This change is test-only and
must receive successful pinned Forge CI and independent PR review before merge.

## Why this step

Stage 10E demonstrated legal two-creature blocks but inherited
`PlayerControllerAi.assignCombatDamage`, which invokes
`ComputerUtilCombat.distributeAIDamage`. That evaluator can inspect the full
Forge game. The inherited ordering callbacks call `AiBlockController`, and both
`reveal` overloads populate `AiCardMemory` (the view overload also resolves views
back to referee-owned cards).

Sources audited at Forge `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`:
`PlayerControllerAi`, `ComputerUtilCombat`, `Combat`, and `GameRules`.

Current rules do not require the old combat damage assignment order. The
[official Foundations rules update](https://magic.wizards.com/en/news/announcements/foundations-update-bulletin)
removed it; current [Comprehensive Rules](https://magic.wizards.com/en/rules)
510.1c/d allow division among opposing combatants. Trample still has separate
requirements. Pinned Forge distinguishes these modes with `hasOrderCombatants`.
This stage supports modern ordering only and does not introduce an obsolete
lethal-before-next-blocker requirement.

## Implementation and scope

`Stage10FCombatBoundaryTest` inherits all 51 Stage 10E tests and substitutes its
controller through the existing attachment point. The parent is opened for test
inheritance using six visibility/finality declarations, avoiding another copy of
its 2,800-line harness.

The new damage callback accepts only face-up Grizzly Bears on their correct
players' current battlefields, actually opposing one another in the current
Forge combat damage step. The provided combatants must exactly match the native
combat relationship, and the damage budget must equal Forge's actual combat
damage. Menace, flying and reach are permitted evasion keywords; other keywords
and legacy-order games abort explicitly.

The bounded deterministic **test policy** puts all damage on the opposing
combatant with the lowest index in the visible battlefield order. It never
uses hidden zones, global card IDs, the caller's enumeration order, AI memory,
the remaining-attacker list, or built-in AI evaluators. Concentrating damage is
a legal policy choice for this supported subset, not a requirement of Magic.
It intentionally does not optimize kills or implement trample.

This policy runs in the Java test controller, not in the external Python
worker. It removes a privileged AI fallback; it does not establish that every
strategic choice has migrated into a hardened external actor. The referee
still owns legality checks, damage application, simultaneous combat and
state-based actions. Actor observations and Python workers are unchanged.

The three legacy ordering callbacks and both reveal overloads now throw
explicit unsupported-operation failures before inherited AI can run. Legitimate
reveal effects need an approved projection/remembering protocol in a later
stage; they are not silently ignored. An unsupported game aborts and is not
claimed to be resumable or completed.

## Regression integrity repair

The live Stage 10E copy still contained the three historical rejection loops
that caught their own `Assert.fail`. This PR ports the focused assertion repair
already developed in PR #23 into that active parent harness. The validator's
implementation and accepted payloads do not change. Each negative assertion now
fails after its catch block if validation returns successfully.

CI first requires all 57 real Forge cases to pass. It then disables only
`validateReply` in a disposable upstream test copy and runs exactly the three
repaired methods. Success requires exactly three `java.lang.AssertionError`
failures bearing `FORGED_PAYLOAD_ACCEPTED:`. A compilation failure, process
crash, skip, missing method, wrong assertion or surviving mutation cannot pass
that gate. The source is restored in `finally` and mutation reports are kept
separate from the passing baseline.

## Required evidence

- Java 21 and exact Forge SHA verification.
- **57 named TestNG methods:** all 51 inherited Stage 10E cases and six new
  combat-boundary methods, with zero failures/errors/skips.
- Native menace combat explicitly verifies that the new damage callback was
  entered for both sides, including an actual two-block assignment. Original
  life totals, casualties and inventory assertions remain in place.
- Direct-callback tests cover damage conservation, private-zone noninterference,
  reversed candidate enumeration, hidden/foreign/noncombat cards, wrong budgets,
  wrong phase, legacy rules, unsupported keywords and reveal/ordering denial.
- **13 Python evidence tests**, which are synthetic XML checks, not gameplay.
- Deliberately disabled-validator mutation detected by all three repaired tests.
- Artifacts retain commit and source hashes, Maven logs/exit, baseline XML,
  mutation source/XML/log, and both gate results, including on failure.

## Remaining limits

These are controlled integration fixtures, not unbiased full matches or expert
play. Unsupported keywords, arbitrary creatures, effects, legacy combat and
reveal choices still need their own actor protocols. Other inherited controller
callbacks and process-level filesystem/network isolation remain unaudited.
The user's negative-life/library-exhaustion scoring rules remain unimplemented.
No reserved seeds, training, promotion, production deployment or main merge are
part of this work; `20261032` and `20261033` remain permanently consumed.

Next: move a bounded visible damage preference into the external actor protocol
without sending phase, stack or hidden state, then expand supported combat
keywords with native Forge legality and outcome tests.
