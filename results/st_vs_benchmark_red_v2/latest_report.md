# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 21,500
**S.T win rate:** 91.63%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.74% | 87.41% | +6.33 pp | 93.99% | 65.23% |
| Eradicator S.T | 93.33% | 88.51% | +4.82 pp | 95.00% | 58.41% |
| Drop Pod S.T | 93.04% | 88.93% | +4.11 pp | 93.30% | 57.65% |
| Pyroclast Squad S.T | 92.97% | 89.09% | +3.88 pp | 93.72% | 63.54% |
| Terminator S.T | 92.64% | 89.67% | +2.98 pp | 93.06% | 65.39% |
| Exterminatus S.T | 89.51% | 92.21% | -2.70 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.46% | -2.22 pp | 92.20% | 4.18% |
| Infernus S.T | 92.02% | 90.89% | +1.12 pp | 93.19% | 61.58% |
| Servo-Skull | 90.95% | 91.83% | -0.87 pp | 91.05% | 22.20% |
| Whirlwind S.T | 91.34% | 92.13% | -0.78 pp | 93.53% | 48.22% |
| Demolitionist S.T | 91.90% | 91.14% | +0.76 pp | 93.54% | 47.44% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
