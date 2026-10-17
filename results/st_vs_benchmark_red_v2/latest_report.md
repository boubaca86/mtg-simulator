# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 152,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.73% | +5.79 pp | 93.80% | 64.96% |
| Eradicator S.T | 93.12% | 88.75% | +4.37 pp | 95.03% | 57.86% |
| Drop Pod S.T | 93.02% | 88.83% | +4.19 pp | 93.28% | 57.44% |
| Terminator S.T | 92.85% | 89.10% | +3.75 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.82% | 89.14% | +3.69 pp | 93.63% | 64.43% |
| Exterminatus S.T | 88.95% | 92.30% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.37% | -2.09 pp | 92.82% | 4.22% |
| Infernus S.T | 92.00% | 90.77% | +1.23 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.50% | 47.83% |
| Servo-Skull | 91.19% | 91.69% | -0.50 pp | 91.24% | 22.00% |
| Demolitionist S.T | 91.67% | 91.39% | +0.28 pp | 93.32% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
