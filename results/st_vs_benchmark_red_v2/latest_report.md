# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 47,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.81% | +5.62 pp | 93.72% | 65.08% |
| Eradicator S.T | 93.24% | 88.48% | +4.76 pp | 95.05% | 58.01% |
| Drop Pod S.T | 93.11% | 88.59% | +4.52 pp | 93.42% | 57.46% |
| Pyroclast Squad S.T | 92.89% | 88.96% | +3.93 pp | 93.68% | 64.27% |
| Terminator S.T | 92.73% | 89.27% | +3.46 pp | 93.24% | 65.50% |
| Exterminatus S.T | 89.16% | 92.22% | -3.06 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.38% | -2.19 pp | 92.93% | 4.12% |
| Infernus S.T | 91.86% | 90.98% | +0.88 pp | 93.03% | 61.47% |
| Whirlwind S.T | 91.27% | 92.06% | -0.79 pp | 93.54% | 47.74% |
| Demolitionist S.T | 91.73% | 91.24% | +0.49 pp | 93.49% | 47.30% |
| Servo-Skull | 91.19% | 91.66% | -0.47 pp | 91.27% | 21.72% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
