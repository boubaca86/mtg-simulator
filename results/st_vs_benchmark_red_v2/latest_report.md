# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 34,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.56% | +5.90 pp | 93.74% | 65.03% |
| Eradicator S.T | 93.18% | 88.35% | +4.83 pp | 94.99% | 58.28% |
| Drop Pod S.T | 93.07% | 88.45% | +4.62 pp | 93.41% | 57.67% |
| Pyroclast Squad S.T | 92.83% | 88.88% | +3.94 pp | 93.59% | 64.14% |
| Exterminatus S.T | 89.07% | 92.15% | -3.08 pp | 0.00% | 0.00% |
| Terminator S.T | 92.52% | 89.46% | +3.05 pp | 92.98% | 65.59% |
| Vindicator S.T | 90.24% | 92.23% | -1.98 pp | 92.78% | 4.13% |
| Infernus S.T | 91.84% | 90.80% | +1.04 pp | 93.03% | 61.47% |
| Whirlwind S.T | 91.23% | 91.92% | -0.70 pp | 93.54% | 47.72% |
| Demolitionist S.T | 91.67% | 91.14% | +0.54 pp | 93.45% | 47.28% |
| Servo-Skull | 91.17% | 91.57% | -0.40 pp | 91.25% | 21.99% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
