# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 391,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.68% | +5.75 pp | 93.72% | 64.99% |
| Eradicator S.T | 93.04% | 88.73% | +4.31 pp | 95.00% | 57.72% |
| Drop Pod S.T | 92.94% | 88.80% | +4.14 pp | 93.20% | 57.40% |
| Terminator S.T | 92.78% | 89.03% | +3.75 pp | 93.28% | 65.49% |
| Pyroclast Squad S.T | 92.77% | 89.05% | +3.72 pp | 93.58% | 64.27% |
| Exterminatus S.T | 88.93% | 92.22% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.34% | -2.21 pp | 92.38% | 4.25% |
| Infernus S.T | 91.93% | 90.71% | +1.23 pp | 93.08% | 61.40% |
| Whirlwind S.T | 91.15% | 92.13% | -0.98 pp | 93.55% | 47.80% |
| Servo-Skull | 91.04% | 91.65% | -0.60 pp | 91.06% | 22.05% |
| Demolitionist S.T | 91.56% | 91.41% | +0.14 pp | 93.25% | 47.04% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
