# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 26,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.65% | 87.45% | +6.19 pp | 93.92% | 65.27% |
| Eradicator S.T | 93.25% | 88.53% | +4.72 pp | 95.00% | 58.33% |
| Drop Pod S.T | 93.04% | 88.81% | +4.23 pp | 93.36% | 57.51% |
| Pyroclast Squad S.T | 92.89% | 89.09% | +3.80 pp | 93.61% | 63.83% |
| Terminator S.T | 92.58% | 89.66% | +2.92 pp | 93.02% | 65.45% |
| Exterminatus S.T | 89.37% | 92.19% | -2.82 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.10% | 92.48% | -2.38 pp | 92.05% | 4.21% |
| Infernus S.T | 91.87% | 91.04% | +0.84 pp | 93.01% | 61.60% |
| Demolitionist S.T | 91.86% | 91.08% | +0.78 pp | 93.61% | 47.33% |
| Servo-Skull | 91.11% | 91.73% | -0.62 pp | 91.14% | 22.09% |
| Whirlwind S.T | 91.40% | 91.90% | -0.49 pp | 93.63% | 47.86% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
