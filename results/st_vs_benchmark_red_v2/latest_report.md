# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 106,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.58% | +5.96 pp | 93.80% | 64.86% |
| Eradicator S.T | 93.15% | 88.60% | +4.54 pp | 95.06% | 57.92% |
| Drop Pod S.T | 93.00% | 88.77% | +4.23 pp | 93.30% | 57.49% |
| Pyroclast Squad S.T | 92.84% | 88.99% | +3.85 pp | 93.66% | 64.37% |
| Terminator S.T | 92.81% | 89.05% | +3.76 pp | 93.30% | 65.53% |
| Exterminatus S.T | 89.07% | 92.22% | -3.15 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.31% | -2.03 pp | 92.62% | 4.23% |
| Whirlwind S.T | 91.11% | 92.29% | -1.18 pp | 93.35% | 47.90% |
| Infernus S.T | 91.94% | 90.79% | +1.15 pp | 93.09% | 61.49% |
| Servo-Skull | 91.19% | 91.64% | -0.45 pp | 91.25% | 21.99% |
| Demolitionist S.T | 91.59% | 91.44% | +0.14 pp | 93.23% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
