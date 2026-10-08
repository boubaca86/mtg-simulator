# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 51,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.74% | +5.69 pp | 93.71% | 64.98% |
| Eradicator S.T | 93.23% | 88.39% | +4.84 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.04% | 88.61% | +4.43 pp | 93.37% | 57.55% |
| Pyroclast Squad S.T | 92.86% | 88.92% | +3.94 pp | 93.66% | 64.28% |
| Terminator S.T | 92.71% | 89.19% | +3.52 pp | 93.22% | 65.60% |
| Exterminatus S.T | 89.20% | 92.17% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.34% | -2.15 pp | 92.74% | 4.13% |
| Infernus S.T | 91.82% | 90.96% | +0.86 pp | 92.99% | 61.44% |
| Whirlwind S.T | 91.26% | 91.98% | -0.72 pp | 93.49% | 47.84% |
| Servo-Skull | 91.14% | 91.63% | -0.49 pp | 91.24% | 21.76% |
| Demolitionist S.T | 91.68% | 91.23% | +0.46 pp | 93.40% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
