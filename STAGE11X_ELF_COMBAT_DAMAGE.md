# Stage 11X: finish the Stage 11W Elf combat-damage path

Base: `09a415c39209a67b1a53b4bb349af3061ee730d3` on
`expert-stage9a-forge-decision-pairs` (Stage 11W).

## Problem and repair

The original Stage 11W run, [38337913586](https://github.com/boubaca86/mtg-simulator/actions/runs/38337913586),
executed 138 native methods. Both new Elf combat methods failed at
`ExternalCreatureController.assignCombatDamage`: the block selector admitted
Llanowar Elves, but the damage allocator rejected that same creature as a target.
The other 136 methods passed.

The subsequent whitelist repair in run [38338273277](https://github.com/boubaca86/mtg-simulator/actions/runs/38338273277)
left one fixture failing with zero accepted blocks. Its arranged board was
allowed to change through random opening-hand main-phase plays before the
asserted combat. The fixture now makes both controllers pass priority during the arranged
combat while continuing to ask the independent actors for native attacks and
blocks. Forge initializes and advances every phase itself. This fixture-only
control is absent from observations; natural games retain normal action
selection. An initial PR run exposed that jumping directly to combat skipped
Forge initialization; that approach was removed rather than bypassing Forge.

The latest Stage 11W repair now accepts Llanowar Elves as a damage source and
recipient alongside the existing five named vanilla creatures. Stage 11X builds
on that repair and adds boundary validation and regression evidence. Every recipient must be a current,
face-up creature controlled by the opponent and present on that opponent's
battlefield. Sources must likewise be current face-up creatures, with the
correct combat power (one for Llanowar Elves). Forge
supplies the actual combatants, applies simultaneous damage, destroys creatures,
and decides the outcome. This change adds no actor input fields or actor policy.

The pinned Forge source is still `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
(`forge-2.0.15`). Its `Combat.assignAttackersDamage` invokes the controller even
for a single blocker. [Official comprehensive rules](https://magic.wizards.com/en/rules),
effective 2026-09-25, describe these assignments in CR 510.1c-d and their
simultaneous application in CR 510.2. The referee retains its existing bounded
lethal-first allocation policy; this is not a general combat-policy expansion.

## Regression evidence

The suite must execute exactly **140 named native methods**, with no failures,
errors, skipped methods, duplicate methods, missing methods, or foreign classes.

- The two inherited Stage 11W fixtures now also require the surviving 2/2 Bear
  to have one damage marked by the blocking Elf and require the trusted damage
  allocator to have run. Native Forge must kill the original registered Elf
  while keeping its controller at two life.
- A new registered-card fixture checks a 3/3 allocating one damage to a 1/1 Elf
  and two to a 2/2 Bear in either target order. It also checks an Elf assigning exactly one damage back. Allocation alone must leave life,
  damage, observations and physical card inventory unchanged.
- A new boundary fixture has a positive control and eight rejected requests:
  own Elf, hidden opposing Elf, a noncreature Forest, invalid source damage,
  an Elf source with an incorrect two-damage request, a face-down Elf, and both references to a departed
  Elf. Rejection must leave state and allocation counters unchanged.

Arranged fixtures are explicitly separate from the inherited Stage 11W natural
cohort: two fresh full 60-card games for each registration order, four total.
Those games use native opening, shuffling, London mulligans and 20 starting life.
Their logged attacks, blocks and winner do not assert that an emergency Elf
block necessarily occurs naturally. No draw is curated or rerolled for an event.

Three inherited forged-reply tests previously caught the assertion that should
have failed them. This branch ports their repaired assertions and mutation
check from PR #30 without importing that PR's different combat policy. After a
passing baseline, CI disables only the reply validator in disposable Forge and
requires those three methods to fail with the precise detection assertion.
Thirteen Python evidence checks exercise the XML and mutation gates.

Local evidence checks pass; native and mutation CI results are recorded in the
pull request against its exact source commit. No merge is authorized by a green
test result alone: the pull request still needs independent review.

## Boundaries

This is still a bounded simulator with two separate persistent Python actors
and a trusted Forge referee, not full Magic support, hardened OS isolation,
training, or an expert-strength evaluation. Reserved evaluation seeds are not
run. Ultimatus scoring is not introduced. The separate own-hand cleanup
prototype remains separate from this repair.
