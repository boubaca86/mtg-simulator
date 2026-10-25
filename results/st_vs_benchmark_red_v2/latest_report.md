# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 247,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.61% | +5.82 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.03% | 88.65% | +4.38 pp | 94.97% | 57.80% |
| Drop Pod S.T | 92.91% | 88.78% | +4.13 pp | 93.16% | 57.41% |
| Terminator S.T | 92.79% | 88.93% | +3.86 pp | 93.28% | 65.47% |
| Pyroclast Squad S.T | 92.73% | 89.05% | +3.68 pp | 93.54% | 64.27% |
| Exterminatus S.T | 88.83% | 92.21% | -3.38 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.31% | -2.18 pp | 92.41% | 4.23% |
| Infernus S.T | 91.92% | 90.66% | +1.26 pp | 93.04% | 61.38% |
| Whirlwind S.T | 91.10% | 92.14% | -1.04 pp | 93.48% | 47.84% |
| Servo-Skull | 91.07% | 91.60% | -0.53 pp | 91.10% | 22.07% |
| Demolitionist S.T | 91.57% | 91.32% | +0.25 pp | 93.23% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
