# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 24,000
**S.T win rate:** 91.61%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.69% | 87.45% | +6.24 pp | 93.96% | 65.24% |
| Eradicator S.T | 93.26% | 88.59% | +4.66 pp | 95.01% | 58.32% |
| Drop Pod S.T | 93.05% | 88.86% | +4.19 pp | 93.37% | 57.60% |
| Pyroclast Squad S.T | 92.99% | 88.98% | +4.01 pp | 93.71% | 63.75% |
| Exterminatus S.T | 89.50% | 92.18% | -2.68 pp | 0.00% | 0.00% |
| Terminator S.T | 92.51% | 89.86% | +2.66 pp | 92.95% | 65.48% |
| Vindicator S.T | 90.13% | 92.50% | -2.37 pp | 92.07% | 4.15% |
| Infernus S.T | 91.93% | 90.99% | +0.94 pp | 93.09% | 61.48% |
| Demolitionist S.T | 91.87% | 91.14% | +0.73 pp | 93.54% | 47.44% |
| Servo-Skull | 91.10% | 91.76% | -0.66 pp | 91.15% | 22.14% |
| Whirlwind S.T | 91.39% | 91.99% | -0.59 pp | 93.59% | 48.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
