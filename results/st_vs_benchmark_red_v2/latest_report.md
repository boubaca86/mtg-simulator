# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 56,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.71% | +5.72 pp | 93.70% | 64.91% |
| Eradicator S.T | 93.18% | 88.44% | +4.74 pp | 95.06% | 58.12% |
| Drop Pod S.T | 93.00% | 88.66% | +4.34 pp | 93.32% | 57.57% |
| Pyroclast Squad S.T | 92.86% | 88.88% | +3.98 pp | 93.64% | 64.28% |
| Terminator S.T | 92.69% | 89.22% | +3.47 pp | 93.19% | 65.43% |
| Exterminatus S.T | 89.13% | 92.17% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.29% | -2.06 pp | 92.99% | 4.18% |
| Whirlwind S.T | 91.21% | 92.03% | -0.82 pp | 93.44% | 47.82% |
| Infernus S.T | 91.77% | 91.02% | +0.75 pp | 92.93% | 61.38% |
| Servo-Skull | 91.14% | 91.62% | -0.48 pp | 91.23% | 21.88% |
| Demolitionist S.T | 91.64% | 91.28% | +0.36 pp | 93.31% | 47.37% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
