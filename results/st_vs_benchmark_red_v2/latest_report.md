# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 77,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.66% | +5.85 pp | 93.75% | 64.96% |
| Eradicator S.T | 93.14% | 88.61% | +4.53 pp | 95.08% | 58.05% |
| Drop Pod S.T | 93.05% | 88.66% | +4.39 pp | 93.34% | 57.64% |
| Pyroclast Squad S.T | 92.84% | 89.04% | +3.79 pp | 93.65% | 64.17% |
| Terminator S.T | 92.77% | 89.14% | +3.63 pp | 93.29% | 65.64% |
| Exterminatus S.T | 89.25% | 92.18% | -2.93 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.37% | -2.18 pp | 92.83% | 4.20% |
| Whirlwind S.T | 91.16% | 92.21% | -1.05 pp | 93.45% | 47.82% |
| Infernus S.T | 91.89% | 90.90% | +0.99 pp | 93.04% | 61.34% |
| Servo-Skull | 91.27% | 91.63% | -0.36 pp | 91.34% | 21.97% |
| Demolitionist S.T | 91.64% | 91.37% | +0.28 pp | 93.32% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
