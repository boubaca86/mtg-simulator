# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 21,000
**S.T win rate:** 91.63%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.77% | 87.37% | +6.39 pp | 94.02% | 65.23% |
| Eradicator S.T | 93.36% | 88.48% | +4.88 pp | 95.03% | 58.36% |
| Drop Pod S.T | 93.02% | 88.98% | +4.05 pp | 93.27% | 57.70% |
| Pyroclast Squad S.T | 92.96% | 89.13% | +3.84 pp | 93.71% | 63.56% |
| Terminator S.T | 92.64% | 89.68% | +2.96 pp | 93.06% | 65.44% |
| Exterminatus S.T | 89.54% | 92.21% | -2.67 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.45% | -2.18 pp | 92.09% | 4.15% |
| Infernus S.T | 92.05% | 90.85% | +1.20 pp | 93.25% | 61.54% |
| Servo-Skull | 90.97% | 91.83% | -0.86 pp | 91.06% | 22.21% |
| Whirlwind S.T | 91.35% | 92.13% | -0.78 pp | 93.50% | 48.29% |
| Demolitionist S.T | 91.90% | 91.16% | +0.75 pp | 93.53% | 47.47% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
