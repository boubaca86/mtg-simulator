# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 37,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.67% | +5.76 pp | 93.73% | 65.02% |
| Eradicator S.T | 93.19% | 88.40% | +4.79 pp | 95.04% | 58.22% |
| Drop Pod S.T | 93.14% | 88.37% | +4.77 pp | 93.45% | 57.67% |
| Pyroclast Squad S.T | 92.87% | 88.86% | +4.01 pp | 93.69% | 64.14% |
| Terminator S.T | 92.54% | 89.48% | +3.07 pp | 93.05% | 65.48% |
| Exterminatus S.T | 89.12% | 92.16% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.27% | -2.04 pp | 92.62% | 4.16% |
| Infernus S.T | 91.84% | 90.87% | +0.97 pp | 93.04% | 61.36% |
| Whirlwind S.T | 91.25% | 91.93% | -0.68 pp | 93.61% | 47.64% |
| Servo-Skull | 91.10% | 91.62% | -0.51 pp | 91.19% | 21.89% |
| Demolitionist S.T | 91.66% | 91.21% | +0.45 pp | 93.51% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
