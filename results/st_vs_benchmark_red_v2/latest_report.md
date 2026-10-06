# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 17,000
**S.T win rate:** 91.68%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.73% | 87.57% | +6.16 pp | 93.98% | 65.29% |
| Eradicator S.T | 93.48% | 88.40% | +5.07 pp | 95.13% | 58.37% |
| Drop Pod S.T | 93.00% | 89.18% | +3.82 pp | 93.33% | 57.62% |
| Pyroclast Squad S.T | 92.93% | 89.29% | +3.64 pp | 93.68% | 63.91% |
| Terminator S.T | 92.67% | 89.79% | +2.88 pp | 93.05% | 65.25% |
| Exterminatus S.T | 89.52% | 92.27% | -2.75 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.37% | 92.47% | -2.10 pp | 92.88% | 4.13% |
| Infernus S.T | 92.04% | 91.00% | +1.04 pp | 93.20% | 61.62% |
| Servo-Skull | 90.91% | 91.91% | -1.00 pp | 91.00% | 22.08% |
| Whirlwind S.T | 91.38% | 92.20% | -0.82 pp | 93.56% | 48.24% |
| Demolitionist S.T | 91.86% | 91.36% | +0.50 pp | 93.55% | 47.53% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
