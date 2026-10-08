# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 39,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.68% | +5.73 pp | 93.71% | 64.96% |
| Eradicator S.T | 93.18% | 88.39% | +4.79 pp | 95.03% | 58.19% |
| Drop Pod S.T | 93.09% | 88.43% | +4.66 pp | 93.42% | 57.66% |
| Pyroclast Squad S.T | 92.89% | 88.78% | +4.11 pp | 93.69% | 64.22% |
| Exterminatus S.T | 89.10% | 92.15% | -3.05 pp | 0.00% | 0.00% |
| Terminator S.T | 92.53% | 89.48% | +3.05 pp | 93.03% | 65.50% |
| Vindicator S.T | 90.19% | 92.28% | -2.08 pp | 92.64% | 4.13% |
| Infernus S.T | 91.78% | 90.95% | +0.83 pp | 92.98% | 61.34% |
| Whirlwind S.T | 91.23% | 91.95% | -0.72 pp | 93.59% | 47.70% |
| Servo-Skull | 91.08% | 91.61% | -0.53 pp | 91.15% | 21.86% |
| Demolitionist S.T | 91.65% | 91.20% | +0.46 pp | 93.51% | 47.13% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
