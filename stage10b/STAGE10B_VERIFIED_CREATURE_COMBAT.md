# Stage 10B — Legal creature casting and real attack damage through one phase-agnostic actor plan

**VERIFIED October 11, 2026:** https://github.com/boubaca86/mtg-simulator/actions/runs/38150119754

- Unmerged feature branch `expert-stage9a-forge-decision-pairs`; pinned tested source commit
  `4c4f837fb5161b9edb6e88a74b8c14efe1f68cb9`.
- Pinned upstream Forge `forge-2.0.15` commit
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven **BUILD SUCCESS**, exit 0; **44 actual TestNG methods executed,
  44 passed, 0 failures/errors/skips**. Strict independently parsed
  Surefire XML gate **PASS**; `full_magic_game:false`.
- CI artifact `stage10b-real-creature-combat-38150119754-1`
  ID `11706627714`, including source SHA256, pinned upstream
  SHA, actual per-method XML, complete Maven output and gate result.
- Source `stage10b/action_plan_actor.py`,
  `forge_tests/Stage10BPriorityCombatPolicyTest.java`,
  `forge_tests/verify_stage10b_surefire.py`,
  `.github/workflows/stage10b-real-creature-combat.yml`.

## Resolved policy defect

Previous Stage10A actor `stage9w/opening_hand_actor.py` always proposed
playing the first basic land in its **own** hand, even after it
had already made a legal land play that turn. Forge correctly
rejected repeated land proposals, but the AI could miss affordable
creature spells or legal combat opportunities.

Stage10B adds a second, process-independent test actor policy that
generates a **passive action plan** using exclusively permitted
observable cards, containing *candidate* indices for lands in
own hand, creatures in own hand and face-up Grizzly Bears on
own battlefield. Its exact bounded reply schema is:

`{"intent":"action_plan","land_hand_indices":[...],"creature_hand_indices":[...],"attack_table_indices":[...]}`

It is a **list of preferences**, not instructions to the engine.
No turn number, phase, priority, stack, mana-pool internals,
opponent concealed hand, card IDs, library top/order or any
new game-state observation fields are passed to the actor.

A **trusted referee** validates exact keys and canonical
nonnegative, unique, size-bounded indices, checks the current
Forge `Player.getCardsIn(Hand)` or owned visible battlefield
and obtains a genuine playable land `SpellAbility` or a
target-free permitted creature `SpellAbility` only after
native `canPlayLand`, `canCastSorcery`,
`SpellAbility.canPlay` and `ComputerUtilCost.canPayCost`
checks. After a legal land is played, another land candidate
is denied and the same referee proceeds to the first affordable
creature candidate, returning the legal ability to Forge's
normal priority/stack/cost-paying pathway. In the real
`declareAttackers` callback the referee filters
the same generic preferences for **legally attack-capable**
ready Grizzly Bears using `CombatUtil.canAttack` and native
attack-cost validation, then declares actual attackers.

The earlier single-action legacy worker remains unchanged for
the **42 previous regression methods**, preserving behavior and
test results. The new actor is launched separately with
`STAGE10B_PLAN_WORKER` for Stage10B-specific tests.

## New real Forge integration results

Two additional TestNG methods passed:

1. **Unphased action plan casts a paid Bear and attacks in natural turns.**
   Forge actually executes `Match.startGame` with independently
   registered **60-card decks**, private shuffle,
   `MulliganService`, referee-selected start, then a
   test-only first-turn sentinel to stop an unlimited game loop.
   To make an affordable Bear scenario **deterministic**,
   the trusted test referee rearranges physical cards already in
   the caster's registered deck after genuine startup, ensuring
   two Forest cards and one Grizzly Bears in **its own hand**,
   keeping hand size at most seven and conserving the full
   original deck inventory. These are normal Forge card
   moves, but **not natural random draws**; this is a controlled
   integration scenario, not a sampled matchup.
   Subsequent `PhaseHandler.mainLoopStep()` calls make actual
   legal land plays, pay mana to cast Grizzly Bears,
   resolve the stack, and on a later turn execute
   a legal combat attack that damages the opponent.
   No manual turn switch, dev-mode change, fake combat or actor-side
   life modification occurs after controlled setup.
2. **Illegal/forged plans are rejected.**
   Duplicate, negative or fractional indices, extra
   `phase` or `set_life` fields are rejected by
   the referee before game mutation. The separate actor
   receives only its allowed four-field game observation
   and returns its bounded four-field preference plan.

All previous 42 actual Forge TestNG methods passed, including
real 5/7 natural turn progression, native opening mulligans,
registered-card inventory conservation, manual and native
first-turn skip, limited mana/stack/combat, actor secret
noninterference, and inherited callback fail-closed protections.

## Limitations

**Not a complete AI-versus-AI match or general MTG expert.**
Only two named target-free creatures are currently explicitly
referee-authorized. The policy has no strategic opponent model,
future search, targeting, legal block selection, varied mana
abilities, arbitrary card text or victory-condition reasoning.
The native match loop is deliberately bounded by a test sentinel;
only later real turns are executed.

The new combat scenario uses **post-start hand arrangement by
the trusted referee**, so do not represent its win/damage
claims as emerging from unbiased shuffled openings.
All actor processes are separate but still not verified
hardened OS file/network/process sandboxes. The Java
controller inherits many privileged Forge AI callbacks
despite progressive fail-closed guards. Stage9E concealed
identity and authorization, re-exile object identity,
cost rollback/randomness and side-channel threats remain.
The user's custom Ultimatus negative-life/highest-life-
at-library-exhaustion endgame is not implemented.

No main merge, production deployment, training or
reserved evaluation seeds were used.

## Next: Stage 10C

Prioritize **bidirectional combat** with legal blocker
decisions from own visible battlefield versus visible
opponent attacking creatures, while preserving user
strict restrictions about not adding phase/priority or
non-table information to either actor. Audit OS sandboxing
and inherited PlayerControllerAi callback exposure before
claiming security. Then test a full deterministic bounded
game-end under an explicitly approved custom-format
scoring model instead of implying stock Forge has custom
negative-life behavior.

Sources: [Wizards official 2026 rules](https://magic.wizards.com/en/rules),
[pinned Forge source](https://github.com/Card-Forge/forge/tree/forge-2.0.15),
[Information-Set MCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
