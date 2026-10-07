# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 29,500
**S.T win rate:** 91.62%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.70% | +5.88 pp | 93.84% | 65.27% |
| Eradicator S.T | 93.32% | 88.49% | +4.83 pp | 95.07% | 58.47% |
| Drop Pod S.T | 93.03% | 88.91% | +4.13 pp | 93.34% | 57.79% |
| Pyroclast Squad S.T | 92.97% | 89.05% | +3.92 pp | 93.70% | 63.96% |
| Terminator S.T | 92.68% | 89.60% | +3.08 pp | 93.12% | 65.32% |
| Exterminatus S.T | 89.38% | 92.24% | -2.86 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.43% | -2.14 pp | 92.33% | 4.15% |
| Infernus S.T | 91.90% | 91.11% | +0.79 pp | 93.03% | 61.57% |
| Whirlwind S.T | 91.42% | 91.97% | -0.55 pp | 93.69% | 47.72% |
| Demolitionist S.T | 91.80% | 91.30% | +0.50 pp | 93.52% | 47.37% |
| Servo-Skull | 91.25% | 91.73% | -0.48 pp | 91.27% | 22.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
