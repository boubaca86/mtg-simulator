# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 14,000
**S.T win rate:** 91.78%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.77% | 87.79% | +5.98 pp | 93.97% | 65.20% |
| Eradicator S.T | 93.63% | 88.42% | +5.21 pp | 95.22% | 58.34% |
| Drop Pod S.T | 93.13% | 89.21% | +3.91 pp | 93.43% | 57.54% |
| Pyroclast Squad S.T | 93.05% | 89.34% | +3.71 pp | 93.83% | 63.82% |
| Terminator S.T | 92.75% | 89.93% | +2.82 pp | 93.16% | 65.22% |
| Exterminatus S.T | 89.61% | 92.37% | -2.76 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.54% | 92.51% | -1.97 pp | 93.55% | 4.21% |
| Servo-Skull | 90.67% | 92.11% | -1.44 pp | 90.82% | 22.03% |
| Infernus S.T | 92.16% | 91.05% | +1.11 pp | 93.25% | 61.77% |
| Whirlwind S.T | 91.49% | 92.27% | -0.78 pp | 93.61% | 48.20% |
| Demolitionist S.T | 91.93% | 91.50% | +0.43 pp | 93.71% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
