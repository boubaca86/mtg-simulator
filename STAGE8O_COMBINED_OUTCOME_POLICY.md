# Stage 8O — combined conservative outcome learner

Stage 8N passed its real-Forge corpus gate on run 37646582768: 16/16 interventions applied, including 8 targeted and 8 untargeted, with zero failed dispatches, invalid accepted requests, lifecycle anomalies, or pre-intervention drift. It produced 216 public-only trajectory rows. Reserved families 20261032-20261033 remained untouched.

Stage 8O combines Stage 8L (8 games / 115 rows / seeds 20261028-20261031) with Stage 8N (16 games / 216 rows / seeds 20261034-20261041). It replaces Stage 8M's high-dimensional representation with a small public-only feature set and stronger regularization.

The development gate is predeclared: grouped leave-one-complete-seed-family-out validation must beat the constant baseline on both game-weighted log loss and Brier score. Failure retires the model without touching reserved evaluation. Passing only authorizes a later one-shot reserved evaluation; it does not authorize gameplay promotion or a strength claim.

Forge remains the sole rules/referee and executor. No custom-card text or gameplay semantics are changed. Hidden identities, future outcomes, search scores, run seeds, Forge proposal identity, and audit metadata are excluded from learner input. Reserved evaluation seeds remain 20261032-20261033.
