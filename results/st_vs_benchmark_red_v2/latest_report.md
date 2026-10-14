# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 120,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.88 pp | 93.84% | 64.97% |
| Eradicator S.T | 93.19% | 88.68% | +4.51 pp | 95.07% | 57.83% |
| Drop Pod S.T | 93.02% | 88.86% | +4.16 pp | 93.31% | 57.47% |
| Pyroclast Squad S.T | 92.85% | 89.11% | +3.75 pp | 93.64% | 64.45% |
| Terminator S.T | 92.85% | 89.12% | +3.73 pp | 93.34% | 65.57% |
| Exterminatus S.T | 89.05% | 92.29% | -3.25 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.02 pp | 92.67% | 4.26% |
| Infernus S.T | 92.02% | 90.78% | +1.24 pp | 93.15% | 61.55% |
| Whirlwind S.T | 91.19% | 92.28% | -1.09 pp | 93.43% | 47.83% |
| Servo-Skull | 91.21% | 91.70% | -0.49 pp | 91.27% | 21.99% |
| Demolitionist S.T | 91.64% | 91.49% | +0.15 pp | 93.27% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
