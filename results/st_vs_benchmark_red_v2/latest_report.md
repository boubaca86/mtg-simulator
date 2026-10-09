# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 51,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.75% | +5.67 pp | 93.69% | 64.98% |
| Eradicator S.T | 93.22% | 88.41% | +4.81 pp | 95.08% | 58.06% |
| Drop Pod S.T | 93.03% | 88.63% | +4.41 pp | 93.36% | 57.52% |
| Pyroclast Squad S.T | 92.87% | 88.90% | +3.97 pp | 93.66% | 64.27% |
| Terminator S.T | 92.71% | 89.19% | +3.53 pp | 93.22% | 65.54% |
| Exterminatus S.T | 89.16% | 92.17% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.32% | -2.13 pp | 92.77% | 4.14% |
| Infernus S.T | 91.81% | 90.98% | +0.83 pp | 92.98% | 61.42% |
| Whirlwind S.T | 91.25% | 91.99% | -0.74 pp | 93.47% | 47.84% |
| Servo-Skull | 91.13% | 91.63% | -0.51 pp | 91.22% | 21.77% |
| Demolitionist S.T | 91.68% | 91.22% | +0.46 pp | 93.40% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
