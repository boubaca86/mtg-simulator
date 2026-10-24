# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 233,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.61% | +5.83 pp | 93.73% | 64.94% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.79% |
| Drop Pod S.T | 92.93% | 88.74% | +4.19 pp | 93.18% | 57.47% |
| Terminator S.T | 92.78% | 88.96% | +3.82 pp | 93.27% | 65.42% |
| Pyroclast Squad S.T | 92.74% | 89.03% | +3.71 pp | 93.55% | 64.30% |
| Exterminatus S.T | 88.81% | 92.22% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.29% | -2.14 pp | 92.50% | 4.24% |
| Infernus S.T | 91.92% | 90.66% | +1.25 pp | 93.05% | 61.42% |
| Whirlwind S.T | 91.11% | 92.13% | -1.03 pp | 93.48% | 47.82% |
| Servo-Skull | 91.03% | 91.61% | -0.58 pp | 91.06% | 22.04% |
| Demolitionist S.T | 91.58% | 91.30% | +0.28 pp | 93.24% | 47.10% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
