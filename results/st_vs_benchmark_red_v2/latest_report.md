# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 38,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.66% | +5.75 pp | 93.71% | 65.03% |
| Eradicator S.T | 93.16% | 88.40% | +4.76 pp | 95.02% | 58.18% |
| Drop Pod S.T | 93.11% | 88.37% | +4.74 pp | 93.46% | 57.64% |
| Pyroclast Squad S.T | 92.87% | 88.82% | +4.05 pp | 93.67% | 64.14% |
| Terminator S.T | 92.54% | 89.44% | +3.10 pp | 93.05% | 65.51% |
| Exterminatus S.T | 89.09% | 92.14% | -3.05 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.26% | -2.06 pp | 92.60% | 4.14% |
| Infernus S.T | 91.80% | 90.89% | +0.91 pp | 93.02% | 61.39% |
| Whirlwind S.T | 91.23% | 91.93% | -0.70 pp | 93.60% | 47.66% |
| Servo-Skull | 91.03% | 91.62% | -0.59 pp | 91.10% | 21.86% |
| Demolitionist S.T | 91.65% | 91.19% | +0.46 pp | 93.52% | 47.12% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
