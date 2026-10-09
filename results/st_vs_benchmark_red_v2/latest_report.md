# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 57,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.72% | +5.68 pp | 93.67% | 64.89% |
| Eradicator S.T | 93.18% | 88.40% | +4.78 pp | 95.07% | 58.13% |
| Drop Pod S.T | 92.98% | 88.67% | +4.31 pp | 93.31% | 57.55% |
| Pyroclast Squad S.T | 92.85% | 88.87% | +3.98 pp | 93.62% | 64.22% |
| Terminator S.T | 92.69% | 89.18% | +3.50 pp | 93.18% | 65.46% |
| Exterminatus S.T | 89.13% | 92.16% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.29% | -2.08 pp | 92.88% | 4.19% |
| Whirlwind S.T | 91.19% | 92.03% | -0.84 pp | 93.44% | 47.81% |
| Infernus S.T | 91.78% | 90.96% | +0.82 pp | 92.93% | 61.39% |
| Servo-Skull | 91.17% | 91.59% | -0.43 pp | 91.25% | 21.92% |
| Demolitionist S.T | 91.60% | 91.31% | +0.29 pp | 93.28% | 47.38% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
