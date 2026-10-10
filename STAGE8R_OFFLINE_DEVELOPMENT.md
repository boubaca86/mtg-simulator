# Stage 8R — offline public target/effect outcome development

Status: development model and retrospective diagnostic. No gameplay, dispatch, or model promotion. Frozen Stage 8O model, Stage 8P protocol, card definitions and referee logic remain unchanged.

## Evidence and critical quarantine

Stage 8P passed gameplay safety checks and failed its one-shot outcome gate: learned side 3 vs Forge baseline 4 (paired delta -1). Run: https://github.com/boubaca86/mtg-simulator/actions/runs/38057158028 ; artifact 11671953705; postmortem issue #13.

**Seeds 20261032 and 20261033 have been consumed. Never rerun them or add their outcomes to training, cross-validation, model selection or future gameplay.**

Stage 8Q adds public target/effect interactions including own/enemy target relations. Stage 8R compares their observational predictive value to Stage 8O on prior development data. Observational game result is not a counterfactual action-value label for unexecuted moves.

## Predeclared historical training inputs and method

- Stage 8L artifact: run 37637623265, artifact 11491113213, digest sha256:4179dbe7bd26bcb745efd9d5da800342f13638a85dd6abd97f449bf3d29a1d7c. Seeds 20261028–20261031. 8 games, 115 observed actions.
- Stage 8N artifact: run 37646582768, artifact 11495733848, digest sha256:36037df0152a57fc6117f5cf91dcaa4cb3fd26288e4b07d6ce2028cffe93062e. Seeds 20261034–20261041. 16 games, 216 observed actions.
- Together: 24 games, 331 executed-action observations, 12 development seed families, zero reserved Stage 8P games.
- Candidate feature model: Stage 8O fixed public action features plus Stage 8Q effect/target interactions, no hidden information or outcome as input. Observed-action game-outcome logistic estimator only, no learned gameplay controller.
- Same regularized logistic training as Stage 8O: 160 epochs; learning rate 0.02; L2 penalty 0.05; bias unpenalized; each full game has equal training weight.
- Validation: 12 folds, each holding out a complete seed family and both orientations. Stage 8R model, Stage 8O model, and constant-prior comparator evaluated on the same independent family folds.
- Advance the representation development claim only if Stage 8R improves **both** game-weighted log loss and Brier score by more than 0.000001 versus each of Stage 8O and constant-prior baseline. If either condition fails, report the failure; do not move thresholds after observing losses. Passing is NOT playing-strength evidence.
- Pinned artifact digest checks, source-file hashes, checkpoint content hash and two hash-seed byte-for-byte training repetitions are required. All output explicitly prohibits promotion, broader model control, and strength claims. CI success means procedural integrity, not necessarily predictive improvement.

## Proposed fresh seed assignments — unverified, not permission to run

These families are only candidates. A later PR MUST first verify that all seeds are unconsumed and commit a one-shot/game-authorization guard, immutable Forge/referee build and patch hashes, exact exported deck hashes, training/model selection parameters, matchup and two orientations, intervention budget, seed allocation, stop conditions, audit fields, metrics and gate.

- Future iterative development candidate families: **20261042–20261049**, both deck orientations, one Forge-only baseline and controlled replay per orientation, 16 paired comparisons.
- Future one-shot held-out candidate families: **20261050–20261057**, same orientations, 16 paired comparisons; never train/tune using these outcomes.
- Proposed final game gate (must still be separately frozen and reviewed before gameplay): at least +3 paired game points total across the 16 held-out pairs and a positive 95% lower confidence bound from 10,000 fixed-seed bootstrap resamples of entire seed families (two orientations sampled together).
- Mandatory separate safety gate: no hidden-information cheating, action illegality, failed dispatches, early substitutions, terminal/lifecycle anomalies or pre-intervention state drift; at least eight valid applied interventions. Forge is sole rules referee.

No stage may reuse the spent 8P seeds. No card, deck or gameplay semantics may be silently rewritten to improve outcomes.

## Automation and review

Workflow Forge Expert AI Stage 8R — Offline Outcome Comparison runs only on code/PR changes and pulls the existing two development artifacts. It has no manual gameplay dispatch. Report artifact: stage8r-offline-grouped-outcome-diagnostic.

Merge only after passing tests, code review, and honest inspection of both the outcome-diagnostic flag and limitations.
