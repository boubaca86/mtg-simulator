# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 111,500
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.63% | +5.91 pp | 93.82% | 64.89% |
| Eradicator S.T | 93.15% | 88.65% | +4.50 pp | 95.05% | 57.90% |
| Drop Pod S.T | 93.02% | 88.78% | +4.24 pp | 93.33% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 89.03% | +3.82 pp | 93.66% | 64.45% |
| Terminator S.T | 92.84% | 89.06% | +3.78 pp | 93.33% | 65.54% |
| Exterminatus S.T | 89.03% | 92.26% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.32% | -2.02 pp | 92.71% | 4.26% |
| Infernus S.T | 91.97% | 90.77% | +1.21 pp | 93.13% | 61.52% |
| Whirlwind S.T | 91.13% | 92.30% | -1.18 pp | 93.39% | 47.82% |
| Servo-Skull | 91.20% | 91.66% | -0.45 pp | 91.27% | 22.01% |
| Demolitionist S.T | 91.60% | 91.47% | +0.13 pp | 93.27% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
