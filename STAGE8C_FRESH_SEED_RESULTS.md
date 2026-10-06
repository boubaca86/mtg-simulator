# Stage 8C — fresh-seed result

Status: **passed all three predeclared practical thresholds**. Learned gameplay
remains disabled. This qualifies the model for shadow instrumentation only.

The [successful run](https://github.com/boubaca86/mtg-simulator/actions/runs/37445803768)
at `92a03583cf7aafe12e824e799edceb9dc89b6e1c` used the four predeclared seed
families 20261012–20261015, four games in each deck orientation, and the frozen
Stage 8C implementation and development artifact.

| Predeclared criterion | Required | Observed |
| --- | ---: | ---: |
| Seed-macro game-macro normalized regret improvement | At least 0.01000 | 0.01113 |
| Families with lower regret | At least 3 of 4 | 4 of 4 |
| Corresponding top-choice accuracy change | At least -2.00 pp | +0.40 pp |

| Fresh family | Regret improvement | Top-choice change |
| --- | ---: | ---: |
| 20261012 | +0.02820 | +2.94 pp |
| 20261013 | +0.00987 | +0.99 pp |
| 20261014 | +0.00418 | -1.89 pp |
| 20261015 | +0.00227 | -0.45 pp |

Coverage is **534/820 captured proposals (65.12%)** across all 32 games, with
2,200 candidates, 5,101 strict pairs, 487 informative decisions and 47 all-label
ties. Thirty-seven decisions have terminal-scale labels. No game is missing.

| Representation | Decision accuracy | Pairwise accuracy | Decision regret | Game-macro accuracy | Game-macro regret |
| --- | ---: | ---: | ---: | ---: | ---: |
| Public semantics | 79.56% | 70.14% | 0.13147 | 78.88% | 0.13740 |
| Typed target semantics | 80.06% | 69.88% | 0.12210 | 79.27% | 0.12627 |

The primary gate passes narrowly. Pairwise accuracy decreased slightly; this is
not an across-metric victory or evidence of improved win rate. Only four new seed
families were tested. The previous Stage 8B primary replication remains failed;
this is a separate predeclared representation comparison.

## Retry and verification

The [original attempt](https://github.com/boubaca86/mtg-simulator/actions/runs/37401589465)
stopped with `OutOfMemoryError: Java heap space` in the ST-first 20261014 log.
The retry increased Java's maximum heap from 6 to 10 GiB. Seeds, decks, cutoff,
features, trainer and thresholds were unchanged; no model result was produced by
the failed run, and its incomplete logs were excluded.

The complete model report reproduced byte for byte locally. Both whole-log
parsers accepted every successful source log, and serializing those logs reproduced
the entire counterfactual corpus exactly. Detailed metrics, source hashes and
coverage are in `results/forge_expert_ai_stage8/typed-target-fresh-replication-v1.json`.

- Development SHA-256: `1173a323a329460d69abfd2c8cab71816430759830b26c927874cc389ff42903`.
- Confirmation SHA-256: `b4e595609f297d926bce7f7bcfb73823701258395f646db859da918444f71580`.
- Artifact: `11404017116`, SHA-256 `eb8c6c38de8a18a009bfec3f44d1875dcca4f3d6f8ca7183ad819e4fc8822d3a`.

The next implemented step is documented in `STAGE8D_SHADOW_POLICY.md`. These
confirmation seeds are now observed and must not be advertised as untouched
evidence for future feature changes.
