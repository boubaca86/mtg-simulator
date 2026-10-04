# Stage 7 — Expert Value Model

## Status

STARTED after the Stage 6 live complete-action regression gate passed on the reproducible 20-game corpus (20/20 games completed, 825 complete root candidates audited, 0 observed strategy-fusion divergences).

Stage 6 remains a permanent anti-cheating regression gate. Its pass is empirical for the exercised corpus, not a proof over every possible Magic position.

## Objective

Improve decision quality toward strong human play by learning a value function that estimates eventual match/game outcome from information legally available to the acting player, while Forge remains the sole rules referee and legal-action executor.

The learned model must not alter card scripts, custom-card rules, legality, priority, stack resolution, combat rules, triggers, replacement effects, or hidden zones.

## Stage 7A — data contract before training

Before fitting any model, record reproducible decision examples from information-set-clean Forge search. Each example must separate observations from labels and must contain no inaccessible opponent information.

Required observation fields:

- deterministic run seed and decision index;
- acting player and play/draw status;
- turn, phase, step, priority context;
- acting player's life and opponent life;
- acting player's visible hand summary;
- public battlefield, graveyard, exile, command/other public-zone summaries;
- public stack state;
- public/known opponent cards only;
- unknown opponent hand/library represented only by counts and legally inferred/public knowledge;
- mana availability and relevant public resources;
- matchup/deck identifier, without exposing shuffled hidden identities;
- complete legal root-action identity (spell/ability, modes, X, targets, relevant choices);
- Stage 6 information-set sample count.

Required labels/metadata:

- Forge search score/value for the action;
- chosen/not-chosen flag;
- eventual game result;
- terminal reason when available;
- search mode and evaluator version.

## Leakage invariants

1. No real unknown opponent hand identity may appear in a feature row.
2. No future library identity/order may appear in a feature row.
3. Known/revealed cards may appear only when Forge marks them as information available to the acting player.
4. Features derived from determinized hidden worlds must be aggregated before export; individual sampled hidden identities must never become model inputs.
5. The complete root action is fixed across hidden-world evaluation according to the Stage 6 contract.
6. Training and validation splits must be grouped by game/run seed so adjacent states from one game cannot leak across the split.
7. Custom card scripts are immutable during model training experiments unless a separate explicit card-rules change is requested and documented.

## Reproducibility requirements

- Pin Forge to the existing forge-2.0.15 source used by Stages 3–6 until a deliberate engine-upgrade experiment is performed.
- Store feature schema/version and evaluator version with every dataset.
- Store seeds and exact CLI settings for generated corpora.
- Establish a hand-written-evaluator/search baseline before enabling the learned evaluator.
- Compare learned-vs-baseline using paired or reverse-seat seeds where practical.
- Keep Stage 6 live fusion audit green for every Stage 7 integration workflow.

## Acceptance gates

Stage 7A passes only when a deterministic extractor produces the same legal-information feature rows for the same seed and CI verifies that forbidden hidden identities are absent.

Stage 7B may then train a simple transparent baseline value model. A complicated neural model is not justified until the data contract, leakage tests, and evaluation harness are stable.

Stage 7 is not an expert-strength claim. Advancement requires reproducible evidence that the learned evaluator improves held-out decision/game performance without weakening the Stage 6 information boundary.
