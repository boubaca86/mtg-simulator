# Stage 8G — one-way live shadow sidecar

Status: **implemented; CI validation required**.

Stage 8F proved that exact Stage 8E returned actions reach
`PlayerControllerAi.playChosenSpellAbility` and dispatch successfully. Stage 8G
moves the frozen Stage 8D model from post-game replay into a genuinely live,
read-only sidecar.

## Architecture

Forge 2.0.15 remains the only rules referee and the only process allowed to
choose or execute actions.

During validation, Forge stdout is connected through a one-way shell pipeline:

`Forge -> tee(full log) -> frozen Stage 8D shadow observer -> recommendations`

The shadow observer receives the same public `EXPERT_STAGE8_CAPTURE` records
already validated in Stages 8A-8F. It emits a recommendation immediately for
each capture and has **no command channel back into Forge**.

The pipeline may impose ordinary stdout backpressure. It cannot alter a legal
action, target, mode, X value, cost, card choice, priority pass, controller
dispatch, or card rule.

## Frozen model and information boundary

The model remains the Stage 8D frozen checkpoint:

`6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288`

Its allowlist remains public-only. Opponent hidden hand/library identities,
counterfactual labels, Forge search scores, future draws and game outcomes are
not model inputs.

`promotion_allowed=false`.

## Validation protocol

Stage 8G deliberately reuses observed seed `20261012`, four games in each deck
orientation. This is integration validation, not fresh playing-strength evidence.

A clean pass requires:

1. all Stage 8F controller-acceptance checks continue to pass;
2. Forge 2.0.15 runs normally with the same decks and three information-set worlds;
3. the live sidecar emits exactly one non-rejected recommendation for every Stage 8 capture;
4. live recommendation order exactly matches capture order;
5. every live recommendation is reproduced exactly by an offline replay of the
   frozen model over the saved Forge log;
6. all returned expert actions still reach the controller and dispatch successfully;
7. zero live/offline drift, zero sidecar rejections and zero Forge dispatch failures.

The offline replay is independent of the live recommendation file. It recomputes
every recommendation from the saved public capture record and frozen checkpoint.

## What a pass means

A pass proves the model can operate alongside an actual running Forge game,
using only the approved public observation stream, without controlling gameplay
and without changing its outputs relative to deterministic offline replay.

It is **not** permission to control Forge and it is not a new win-rate result.

After Stage 8G, the next justified step is to instrument the accepted action
through stack/no-stack completion and eventual resolution/zone outcome, then
design a bounded policy-control experiment only after those execution semantics
are fully audited.
