# Stage 8 — next fresh-seed replication

Status: **completed; positive-replication threshold not met**. Results and the
documented operational timeout extension are in `STAGE8_FRESH_SEED_RESULTS.md`.
The predeclared model comparison and thresholds below remain unchanged. This is
an offline research gate, not authorization for learned live move selection.

## Freeze before capture

Use the v3 feature/trainer implementation from
`71e6ab6b403cd889c60bb5743a31931e25366ff5`, including its recorded 20-epoch,
0.05-learning-rate configuration. Do not tune the representations or trainer on
the new outcomes. A correctness fix that changes predictions invalidates the
comparison and requires a documented protocol revision before rerunning it.

Train each representation once on **only** seeds 20261004–20261007 from the
development artifact pinned in `STAGE8_CONTEXTUAL_RANKING_PROTOCOL.md`. The
counterfactual JSONL hash must equal
`106912cfd8800cad8abe65231abeca03578c4185205a2e11b19e87000cc8ee7e`.
The fresh families must never enter a training fold; simply adding them to the
current leave-one-seed-out command would not implement this experiment.

Capture seeds **20261008, 20261009, 20261010, 20261011**, four games per seed in
each of the same two deck orientations: 32 complete games. Keep Forge 2.0.15,
fixed-root-v1, three information-set worlds, the current card/deck definitions
and capture settings. Record the exact capture commit, deck hashes, corpus
hashes and all timeout/replay/audit diagnostics. Reject incomplete or unsafe
source logs using the existing contract; report failures rather than replacing
an inconvenient seed after inspecting its result.

## Paired comparison

Use the same candidates/decisions for uniform choice, frozen legacy action hash,
canonical action only, public counts, visible identity and public semantics.
Forge's selected proposal remains a label-consistency reference, not an
independent opponent or a learned-policy comparator.

The **primary contrast** is public counts versus canonical action only. The
primary metric is normalized regret, averaged within each represented game,
then across games within each seed family, then equally across the four seed
families. Smaller is better. This prevents one long game or prolific family
from determining the result. Game and proposal coverage must accompany it.

Also report top-choice accuracy, strict pairwise accuracy, decision-weighted
and game-macro metrics, all per-family differences, model/label ties, and the
full secondary comparisons. Use the v3 label-tie and predicted-tie rules without
changes. Never replace the primary contrast with whichever secondary model wins.

Call this replication positive only if public counts reduces the primary mean
regret by at least **0.01**, has lower regret in at least **three of four** fresh
families, and loses no more than **two percentage points** of seed-macro
game-macro top-choice accuracy relative to action only. These are predeclared
practical research thresholds, not a claim of statistical significance. Require
at least one rankable game in every family and show every unrepresented game.

If the criteria fail, retain the result as a failed or inconclusive replication.
Any subsequent feature or parameter changes make these seeds development data;
another untouched set is then required. A positive result justifies discussing
shadow instrumentation. `promotion_allowed` stays false either way.

## Limits unchanged

Only 32 games and four fresh seed families still provide limited evidence. The
labels imitate Forge's current fixed-root evaluator. Passing, unrecorded priority
windows and actual execution are outside proposal coverage; same-name physical
targets may remain indistinguishable to the learner. This experiment cannot
establish improved win rate, independent continuation quality or master-level
play. Those require separate execution-aware and opponent-based experiments.
