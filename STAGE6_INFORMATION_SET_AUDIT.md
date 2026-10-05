# Stage 6 — Information-set action-consistency audit

**Current implementation:** `STAGE6_FIXED_ROOT_AGGREGATION.md` describes the
replacement of per-world optimum averaging with fixed-root recipe aggregation,
the passing real-Forge and failed-seed regressions, and the remaining limits.

**2026-10-05: reopened.** The earlier Java audit serialized the root before merging
its child target/mode/choice nodes. The repaired snapshot and real-Forge regression
are described in `STAGE7_INTEGRITY_REPAIR.md`. Zero historical divergences do not
close the complete-action replay/aggregation requirement below.

## Why this audit exists

Stage 5 successfully resamples hidden opponent hand/library state and averages the score of each root spell/ability across three plausible worlds. The 80-game validation was stable and strongly directional.

However, inspection of `SpellAbilityPicker` found a stricter imperfect-information requirement that Stage 5 does not yet satisfy for every decision.

`evaluateSa(...)` creates a fresh `SpellAbilityChoicesIterator` for each hidden-world sample. That iterator can independently optimize modes, targets, and hidden-origin/card choices inside each sampled world. Therefore the aggregate score for a root spell/ability can become an average of *different concrete actions* chosen with knowledge of each determinization. The root spell identity is information-set consistent, but its target/mode/choice policy is not guaranteed to be.

This is a form of strategy fusion. It is not acceptable for an expert AI whose search must obey the same information boundary as a human player.

## Consequence for Stage 5 results

The Stage 5 validation remains useful as an engineering/stability result for multi-world search, but its strength numbers must **not** be treated as clean evidence of non-cheating expert strength until action consistency is fixed.

## Stage 6 requirement

A root action must be represented by a complete public decision key, not merely the spell/ability object. At minimum the key must include:

- spell/ability identity
- selected modes
- announced X value
- public targets
- public sacrifice/discard choices when they are part of the decision
- any other choice made before hidden information changes

For every candidate complete action key:

1. Enumerate the action from legal information without reading the real unknown opponent hand/library.
2. Evaluate **that same action key** in every determinized hidden world.
3. Reject worlds where the action is genuinely illegal under information the player is entitled to know; do not use hidden-world illegality to leak information.
4. Aggregate expected value plus an explicitly documented downside-risk term.
5. Execute the exact winning action key in the real game.

No sample may independently choose a different target/mode/choice and then contribute that optimized score to the same aggregate candidate.

## Safety rule

Until this is implemented and tested, Stage 5 multi-sample search is experimental. Forge remains the rules referee, card scripts are unchanged, and no learned evaluator should be trained from Stage 5 ensemble decisions as if they were clean expert labels.

## Next validation

Build an adversarial tactical fixture where two hidden-world determinizations prefer different public targets. The information-set AI must choose one target based on aggregate value and use that same target in all worlds. A test should fail if per-world target optimization changes the aggregate candidate score.
