# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 110,500
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.62% | +5.94 pp | 93.83% | 64.91% |
| Eradicator S.T | 93.15% | 88.66% | +4.49 pp | 95.05% | 57.89% |
| Drop Pod S.T | 93.02% | 88.80% | +4.21 pp | 93.32% | 57.49% |
| Pyroclast Squad S.T | 92.85% | 89.03% | +3.82 pp | 93.66% | 64.44% |
| Terminator S.T | 92.85% | 89.05% | +3.80 pp | 93.34% | 65.54% |
| Exterminatus S.T | 89.07% | 92.25% | -3.18 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.32% | 92.32% | -2.00 pp | 92.70% | 4.25% |
| Infernus S.T | 91.97% | 90.78% | +1.19 pp | 93.12% | 61.52% |
| Whirlwind S.T | 91.13% | 92.30% | -1.17 pp | 93.38% | 47.83% |
| Servo-Skull | 91.20% | 91.67% | -0.47 pp | 91.26% | 22.01% |
| Demolitionist S.T | 91.61% | 91.47% | +0.13 pp | 93.27% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
