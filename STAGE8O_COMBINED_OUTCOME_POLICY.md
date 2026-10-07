# Stage 8O — combined conservative outcome learner

Stage 8N passed its real-Forge corpus gate on run 37646582768: 16/16 interventions applied, including 8 targeted and 8 untargeted, with zero failed dispatches, invalid accepted requests, lifecycle anomalies, or pre-intervention drift. It produced 216 public-only trajectory rows. Reserved families 20261032-20261033 remained untouched.

Stage 8O combines Stage 8L (8 games / 115 rows / seeds 20261028-20261031) with Stage 8N (16 games / 216 rows / seeds 20261034-20261041). It replaces Stage 8M's high-dimensional representation with a small public-only feature set and stronger regularization.

The development gate is predeclared: grouped leave-one-complete-seed-family-out validation must beat the constant baseline on both game-weighted log loss and Brier score. Failure retires the model without touching reserved evaluation. Passing only authorizes a later one-shot reserved evaluation; it does not authorize gameplay promotion or a strength claim.

Forge remains the sole rules/referee and executor. No custom-card text or gameplay semantics are changed. Hidden identities, future outcomes, search scores, run seeds, Forge proposal identity, and audit metadata are excluded from learner input. Reserved evaluation seeds remain 20261032-20261033.


## Frozen implementation: Stage 8O

The implementation joins the two complete passing development artifacts,
115 + 216 = **331 rows across 24 games and twelve seed families**.
The Stage 8L and 8N artifact digests are checked by GitHub Actions before
training. No reserved evaluation game is run or inspected.

The learner is a fixed-vocabulary, low-dimensional logistic outcome estimator,
trained exclusively on actually executed actions. Alternative legal actions
without observed outcomes are **not** assigned imaginary counterfactual labels.

Predeclared settings:

- objective: observed-behavior-game-outcome-logistic
- feature family: fixed-small-public-action-v1
- epochs: 160; learning rate: 0.02; L2: 0.05 (bias unpenalized)
- each full source game contributes equal total training weight
- hold out both full games for each seed family during 12-fold grouped validation
- constant baseline: game-weighted training mean, never learned from held-out labels
- fixed vocabulary of phase, life, hand/zone counts, legal candidate count,
  action-word flags and public target roles/counts
- no hidden card identifiers, raw transient object IDs, future knowledge,
  deck/actor identities, outcome labels, Forge search scores or run/corpus seeds
  in prediction features

This is an observational predictor, not a causal action-value estimator: game
results also depend on later decisions and opponent play. Its action ranking is
experimental and cannot be promoted from correlation alone.

CI verifies source files, checkpoint content hash, locked source-code and
hyperparameter provenance, byte-identical results under different Python hash
seeds, and repeatable whole-family validation.

**Predeclared development gate:** the grouped out-of-family model must show
strictly lower game-weighted log loss AND Brier score than the constant baseline,
with an improvement of more than 1e-6 for each. Both conditions are required.
The final workflow exits nonzero on failure, retains its report artifact, and
does not inspect reserved seeds.

Passing this diagnostic only authorizes drafting a distinct, predeclared,
one-shot evaluation on 20261032 and 20261033. It does **not** prove stronger
play, authorize model control, or permit changes to Forge/card semantics.
