# Stage 8P — precommitted reserved pilot

Reserved families 20261032 and 20261033 were still unopened in the run-history
inspection on 2026-10-10 described below. Each family runs S.T vs Benchmark Red
and the reverse: four Forge-only baselines and four paired controlled games.

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

## One-shot seed guard

The two manual attempts on 2026-10-10,
[38016800081](https://github.com/boubaca86/mtg-simulator/actions/runs/38016800081)
and [38016854914](https://github.com/boubaca86/mtg-simulator/actions/runs/38016854914),
both failed while retrieving/validating the frozen models. Their reserved
gameplay steps were explicitly `completed` / `skipped`; neither attempt opened
the reserved games. The subsequent checkpoint preflight correction on `main`
is retained unchanged by this guard work.

`stage8p_one_shot_guard.py` now runs as a required, separate step immediately
before gameplay. The manual-only, main-branch pilot uses one fixed concurrency
group for all runs and refs, with `cancel-in-progress: false`. A failed guard
stops gameplay, and its log is included in the always-uploaded evidence artifact.

The guard reads every workflow-run page and every job page with `filter=all`
so an earlier attempt cannot be hidden by a newer preflight failure. It checks
pagination totals, unique record IDs, contiguous workflow run numbers from 1,
and exactly one `reserved-pilot` job for every attempt. Empty job lists,
unrelated jobs, missing attempts or steps, inconsistent state, and incomplete
history all fail closed. Only the exact currently running attempt may have a
queued or omitted future gameplay step. Every historical attempt must show an
explicitly skipped gameplay step. Started gameplay locks the seeds even if
that step later fails, times out or is cancelled.

This intentionally conservative guard also stops if another run is still
active/pending or a run was cancelled before usable job history was recorded.
Investigate such a failure before considering any further run; there is no
force or reset option. Preserve workflow history and do not rerun historical
workflow revisions without the guard: concurrency applies to revisions that
declare the group, and this history check cannot enforce behavior in old or
edited workflows that omit it.

The branch `expert-stage8p-one-shot-seed-guard` adds 35 synthetic guard, CLI and
workflow integration tests. **108 focused tests passed locally**, covering
these tests and the existing Stage 8P/O/K/H/F/E/D contracts. The integration
test executes the actual guard shell command with a fake `gh` executable,
verifying both success and failure propagation through `tee`, including API
failure, empty history, and gameplay in an earlier attempt. A separate read-only
replay of both real job-history responses also passed. No reserved gameplay was
run for this validation.

Run the same contract locally with:

```bash
python -m pip install PyYAML==6.0.3
python -m unittest -v test_stage8p_one_shot_guard test_stage8p_workflow_contract test_stage8p_reserved test_stage8o_outcome_policy test_stage8k_return_boundary_intervention test_stage8h_lifecycle_audit test_stage8f_acceptance_audit test_stage8e_returned_action_audit test_stage8d_shadow_policy
```

The contract workflow runs these checks on the guard branch and pull requests.
Successful CI and a reviewed pull request are required before merging. No
reserved pilot may be launched as part of testing this change. The frozen
models, reserved seeds, selection rule and evaluation gates are unchanged.
