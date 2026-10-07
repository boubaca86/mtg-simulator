# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 30,500
**S.T win rate:** 91.63%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.60% | 87.70% | +5.90 pp | 93.86% | 65.19% |
| Eradicator S.T | 93.33% | 88.50% | +4.83 pp | 95.08% | 58.40% |
| Drop Pod S.T | 93.04% | 88.92% | +4.12 pp | 93.34% | 57.85% |
| Pyroclast Squad S.T | 92.95% | 89.10% | +3.85 pp | 93.68% | 64.07% |
| Terminator S.T | 92.67% | 89.63% | +3.04 pp | 93.10% | 65.41% |
| Exterminatus S.T | 89.27% | 92.27% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.46% | -2.20 pp | 92.41% | 4.14% |
| Infernus S.T | 91.94% | 91.04% | +0.91 pp | 93.08% | 61.51% |
| Whirlwind S.T | 91.40% | 92.03% | -0.64 pp | 93.62% | 47.68% |
| Demolitionist S.T | 91.79% | 91.34% | +0.45 pp | 93.48% | 47.39% |
| Servo-Skull | 91.33% | 91.72% | -0.39 pp | 91.38% | 22.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
