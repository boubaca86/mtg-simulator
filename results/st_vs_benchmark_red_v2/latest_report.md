# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 150,500
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.72% | +5.78 pp | 93.78% | 64.96% |
| Eradicator S.T | 93.12% | 88.71% | +4.42 pp | 95.04% | 57.86% |
| Drop Pod S.T | 93.02% | 88.81% | +4.21 pp | 93.27% | 57.43% |
| Terminator S.T | 92.84% | 89.08% | +3.76 pp | 93.34% | 65.52% |
| Pyroclast Squad S.T | 92.80% | 89.13% | +3.67 pp | 93.61% | 64.43% |
| Exterminatus S.T | 88.96% | 92.28% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.37% | -2.12 pp | 92.80% | 4.21% |
| Infernus S.T | 91.99% | 90.75% | +1.24 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.16% | 92.26% | -1.09 pp | 93.48% | 47.84% |
| Servo-Skull | 91.18% | 91.68% | -0.50 pp | 91.23% | 21.99% |
| Demolitionist S.T | 91.66% | 91.38% | +0.28 pp | 93.31% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
