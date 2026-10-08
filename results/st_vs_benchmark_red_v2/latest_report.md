# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 49,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.76% | +5.68 pp | 93.73% | 65.04% |
| Eradicator S.T | 93.25% | 88.43% | +4.82 pp | 95.08% | 57.99% |
| Drop Pod S.T | 93.07% | 88.63% | +4.45 pp | 93.38% | 57.47% |
| Pyroclast Squad S.T | 92.87% | 88.96% | +3.91 pp | 93.66% | 64.26% |
| Terminator S.T | 92.72% | 89.24% | +3.47 pp | 93.23% | 65.60% |
| Exterminatus S.T | 89.15% | 92.21% | -3.06 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.34% | -2.10 pp | 92.92% | 4.12% |
| Infernus S.T | 91.86% | 90.95% | +0.91 pp | 93.02% | 61.48% |
| Whirlwind S.T | 91.25% | 92.05% | -0.80 pp | 93.50% | 47.79% |
| Servo-Skull | 91.14% | 91.66% | -0.52 pp | 91.21% | 21.79% |
| Demolitionist S.T | 91.72% | 91.23% | +0.49 pp | 93.43% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
