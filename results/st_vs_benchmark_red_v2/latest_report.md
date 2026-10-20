# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 189,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.68% | +5.80 pp | 93.76% | 64.93% |
| Eradicator S.T | 93.10% | 88.67% | +4.44 pp | 95.01% | 57.86% |
| Drop Pod S.T | 92.96% | 88.82% | +4.14 pp | 93.22% | 57.46% |
| Terminator S.T | 92.80% | 89.08% | +3.72 pp | 93.30% | 65.46% |
| Pyroclast Squad S.T | 92.79% | 89.08% | +3.71 pp | 93.59% | 64.36% |
| Exterminatus S.T | 88.87% | 92.27% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.36% | -2.19 pp | 92.57% | 4.21% |
| Infernus S.T | 91.96% | 90.73% | +1.23 pp | 93.10% | 61.44% |
| Whirlwind S.T | 91.17% | 92.16% | -0.99 pp | 93.51% | 47.86% |
| Servo-Skull | 91.10% | 91.66% | -0.57 pp | 91.12% | 22.03% |
| Demolitionist S.T | 91.63% | 91.36% | +0.27 pp | 93.24% | 47.19% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
