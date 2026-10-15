# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 128,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.87 pp | 93.83% | 64.98% |
| Eradicator S.T | 93.17% | 88.70% | +4.48 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.01% | 88.89% | +4.12 pp | 93.29% | 57.52% |
| Pyroclast Squad S.T | 92.84% | 89.13% | +3.71 pp | 93.64% | 64.43% |
| Terminator S.T | 92.84% | 89.14% | +3.70 pp | 93.34% | 65.52% |
| Exterminatus S.T | 89.02% | 92.30% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.03 pp | 92.70% | 4.23% |
| Infernus S.T | 92.03% | 90.76% | +1.27 pp | 93.15% | 61.60% |
| Whirlwind S.T | 91.21% | 92.25% | -1.04 pp | 93.46% | 47.88% |
| Servo-Skull | 91.27% | 91.68% | -0.41 pp | 91.32% | 21.93% |
| Demolitionist S.T | 91.64% | 91.49% | +0.15 pp | 93.27% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
