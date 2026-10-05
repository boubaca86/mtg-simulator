# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 10,000
**S.T win rate:** 92.03%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.91% | 88.23% | +5.68 pp | 94.14% | 65.51% |
| Eradicator S.T | 93.90% | 88.66% | +5.24 pp | 95.51% | 58.16% |
| Pyroclast Squad S.T | 93.32% | 89.51% | +3.81 pp | 94.09% | 64.25% |
| Drop Pod S.T | 93.15% | 89.90% | +3.25 pp | 93.42% | 57.56% |
| Exterminatus S.T | 89.63% | 92.68% | -3.05 pp | 0.00% | 0.00% |
| Terminator S.T | 92.98% | 90.22% | +2.76 pp | 93.35% | 65.25% |
| Vindicator S.T | 91.00% | 92.65% | -1.64 pp | 93.98% | 4.32% |
| Servo-Skull | 91.00% | 92.34% | -1.34 pp | 91.02% | 22.16% |
| Infernus S.T | 92.45% | 91.25% | +1.19 pp | 93.51% | 61.17% |
| Whirlwind S.T | 91.70% | 92.59% | -0.89 pp | 93.57% | 48.06% |
| Demolitionist S.T | 92.18% | 91.76% | +0.43 pp | 93.89% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
