# Expert MTG AI — Stage 7C semantic evaluator

## Status

Stage 7C offline semantic evaluation passed its **research** gate on the fixed Forge corpus, but is **not promoted into move selection**.

## Reproducible corpus

- Forge referee: pinned Forge `2.0.15`.
- Corpus seeds: `20261004`, `20261005`, `20261006`, `20261007`.
- Two games per seed in each deck orientation (`ST Forge Full` vs `Benchmark Red Forge`, and reversed).
- 16 complete games, 338 legal decision rows.
- Training: seeds `20261004`–`20261006`, 12 games / 248 rows.
- Holdout: seed `20261007`, 4 games / 90 rows.
- Split unit is the corpus seed, so related games and all decisions from a game stay together.
- Dataset SHA-256 observed in the passing workflow: `e944b572a0b1fdba6fa42a22befd5189608b826cece40084387638e91767c28c`.

## Information boundary

The evaluator may consume the acting player's legally known hand and public zone identities separated by perspective. It must not consume opponent hidden hand identities, either library's identities/order, or future draws. Forge remains authoritative for rules and terminal outcomes. Complete-action identity remains required by the Stage 6 regression.

No custom card gameplay rule is changed by Stage 7C.

## Fixed holdout result

Game-mean holdout log loss:

| Evaluator | Log loss |
| --- | ---: |
| Training prior | 0.611689 |
| Combined counts | 0.850289 |
| Perspective counts | 0.594673 |
| Visible card identity hash | **0.441409** |

Visible legal card identities therefore improved holdout outcome prediction by about `0.153264` log-loss versus perspective-aware counts on the exact same split. This is evidence that card identity carries useful signal; it is **not** evidence of expert playing strength.

The earlier historical `0.400665` result is invalid as a benchmark because it used incorrect winner labels and must not be cited as a target.

## Promotion decision

`promotion_allowed = false`.

Reasons:

1. Only 16 games are represented.
2. Card hashing identifies cards but does not encode their rules text, roles, interactions, mana efficiency, timing, or tactical context.
3. Outcome prediction is not action-selection strength.
4. Stage 6 complete-action replay/aggregation is still unfinished.
5. A live evaluator must prove strength on unseen games before influencing Forge choices.

## Next justified stage: 7D

Stage 7D should add **legal public card semantics** without touching gameplay rules: deterministic features derived from Forge-visible card characteristics (for example mana value, types, power/toughness where public, and stable ability/rules descriptors available to the acting player). Compare them against Stage 7C on the same seed-grouped protocol first.

Only after semantic features beat the Stage 7C evaluator reproducibly on a larger, multi-seed/multi-matchup holdout should the project wire a learned value estimate into search. That live integration must be shadow-tested first: record what the learned evaluator would choose while Forge continues to make the actual move. Promotion requires a predeclared win-rate/quality gate and the existing hidden-information audits to remain green.
