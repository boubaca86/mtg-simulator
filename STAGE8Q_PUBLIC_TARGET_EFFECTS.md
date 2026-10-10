# Stage 8Q — public target/effect interactions (development only)

Status: feature extractor and synthetic tests only. **No new model training, no gameplay, no promotion, and no new playing-strength claim.** This is a successor proposal to the frozen Stage 8O / consumed Stage 8P evaluation; the frozen model and all Stage 8P files remain unchanged.

## Why this matters

Stage 8O has `target_self` and `target_opponent` as separate indicators from generic `action_damage`. Add explicit public interaction features for fixed-damage effects (self/opponent player and self/opponent creature), creature's publicly reported power/toughness, and other effects such as gain life, destroy, and +1/+1 counters. This makes distinct legal actions distinguishable **without** equating every self-target with a mistake: damage to one's own creatures can be a legitimate synergy, and the estimator still needs actual game evidence to know when.

All features come exclusively from Forge's validated public input envelope and Forge-emitted typed target descriptors. There is no hidden hand, unrevealed library content, transient object ID, search value, terminal outcome, seed, future game state or deck identifier in the features. Unknown and ambiguous effect magnitudes are not assigned made-up damage. Forge remains the only rule/referee engine, and no feature can authorize an action. Candidate order has no authority; action identity must be part of Forge's legal candidate set.

## Frozen evidence and boundaries

- Stage 8P reserved evidence: [run 38057158028](https://github.com/boubaca86/mtg-simulator/actions/runs/38057158028), artifact 11671953705, ZIP SHA256 `dd37d8dbf6dd6211009a713f96ea9d739e4c4b19720631e99947b23636bf4f9d`.
- Seeds **20261032 / 20261033 have been used**. Never rerun, replay, or use this held-out outcome for training or model-selection hyperparameters.
- Stage 8P passed its safety gate but failed its predeclared +1.0 game score gate; observed paired delta -1.0. The reported target-choice mistake is an *example motivating the feature family*, not a training sample or a new benchmark.
- Stage 8O model ID `90bbbd616e2509c3c85c318e5aded17cfe6e7592ad625bad2a1467136a596ba3` stays frozen.
- No edits to card text, Forge search, turn order, mana, targeting legality, or gameplay semantics.

## Explicit acceptance for **this** PR

1. Synthetic regressions: opponent-player burn versus self-creature burn, opponent-creature removal, positive self-buffs, public ownership consistency, strict hidden-field rejection, unlisted-candidate rejection, numeric boundedness, canonical object-ID invariance, and invariant vector keys.
2. Run the new Stage 8Q CI plus existing Stage 8O and Stage 8C/D safety tests. CI must pass before human review and merge.
3. No workflow_dispatch or automatic reserved gameplay is introduced by this PR. It cannot itself cause gameplay.

## Future training and evaluation — not authorized by this PR

Before any new games: independently establish that candidate development seeds are unused and commit a separate seed manifest with frozen decks, Forge version/patch hash, frozen baseline, both play orientations, data collection, game-count sample size, model architecture and objective, paired game-score metric, a predeclared acceptance threshold, confidence uncertainty, and non-overlapping held-out families. Group cross-validation by entire seed families and games, never train/evaluate on the same game or family. Use only newly authorized **nonreserved** seeds for iterative work. Freeze the candidate model before any one-shot new held-out evaluation.

A synthetic feature test passing does not establish game strength. Any broad learned-control enablement remains forbidden until a separate, precommitted Forge gameplay evaluation supports it.