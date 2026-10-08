# Stage 8P — precommitted reserved pilot

Reserved families 20261032 and 20261033 remain unopened. Each family runs S.T vs Benchmark Red and the reverse: four Forge-only baselines and four paired controlled games.

The frozen Stage 8O model chooses at most one action per game, only at an actual returned-action boundary. Choose the first unique public-only recommendation that exceeds Forge's score by more than 1e-6. Ties within 1e-12 abstain. The action must be Forge-legal and valid in every sampled world.

Safety requires four complete pairs, at least two applied interventions, no failed dispatches, pre-intervention drift or terminal anomalies. The primary result gate requires at least +1.0 total paired game point versus the Forge-only baselines. Both gates must pass. Neither outcome permits broader control or human-expert claims. Do not change card rules or Forge semantics.

## Controlled-game integration preflight

The initial Stage 8P comparison called the Forge-only baseline auditor on the
controlled game before reaching Stage 8K's intervention-aware auditor. That
baseline auditor requires every returned action to equal Forge's original
proposal, so it rejected the very learned substitution the pilot requires.
This would prevent any valid pilot with at least two interventions from passing.

The failure was reproduced before opening reserved games, using the already
verified `stage8k-controlled-st-first-20261024.log` from run `37557979992`,
artifact `11456102553`. Stage 8K verified the applied intervention, complete
terminal coverage, zero failed dispatches and zero lifecycle anomalies; the
old Stage 8P pre-audit rejected it with
`returned action differs from Forge captured proposal`.

Controlled logs now enter the existing Stage 8K auditor after recomputing and
verifying the frozen plan and exact request bytes. It permits exactly the
planned substitution and still validates the complete public capture,
return/acceptance/terminal chain and unchanged pre-intervention history.
Forge-only baselines continue to require Forge's original action.

Eleven new integrated regressions exercise synthetic full-game records through
the actual planner, pair auditor and aggregate gate. They cover targeted and
untargeted substitutions, ties and abstention, a passing four-pair result,
safe interventions without game-result improvement, failed dispatches,
contradictory resolution, missing terminals, identity drift, unplanned moves,
pre-intervention drift, and altered plans/requests. The passing four-pair test
was first run against the old implementation and reproduced the failure.

Validation: **73 focused tests passed locally and in
[CI run 37706982998](https://github.com/boubaca86/mtg-simulator/actions/runs/37706982998)**
at commit `bb9ba874e7f5d25e386589d9d0acf6d19ec8c7c2`, including all Stage 8P
tests and the inherited Stage 8O/K/H/F/E/D contracts. The Stage 8P contract
workflow now runs that full set on every change. All new game records are
synthetic; the archived Stage 8K log is development evidence only.

The reserved pilot remains manual-only. Its frozen model, two reserved seed
families, selection rule and precommitted gates are unchanged. No reserved
trial or new playing-strength result is reported by this preflight.
