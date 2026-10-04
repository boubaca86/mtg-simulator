# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 1,000
**S.T win rate:** 92.80%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Pyroclast Squad S.T | 94.74% | 88.57% | +6.17 pp | 95.37% | 66.90% |
| Vindicator S.T | 89.11% | 94.86% | -5.75 pp | 93.02% | 4.30% |
| Exterminatus S.T | 89.04% | 93.85% | -4.81 pp | 0.00% | 0.00% |
| Eradicator S.T | 94.34% | 89.88% | +4.46 pp | 96.37% | 57.80% |
| Servitor | 94.05% | 90.24% | +3.80 pp | 94.52% | 65.70% |
| Servo-Skull | 90.21% | 93.59% | -3.38 pp | 90.18% | 22.40% |
| Drop Pod S.T | 93.74% | 91.14% | +2.60 pp | 94.01% | 56.80% |
| Demolitionist S.T | 93.33% | 91.89% | +1.44 pp | 95.70% | 46.50% |
| Terminator S.T | 93.10% | 92.24% | +0.86 pp | 93.67% | 64.80% |
| Whirlwind S.T | 92.50% | 93.30% | -0.79 pp | 95.61% | 47.80% |
| Infernus S.T | 92.85% | 92.72% | +0.13 pp | 93.61% | 61.00% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
