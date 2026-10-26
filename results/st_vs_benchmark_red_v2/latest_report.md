# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 255,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.62% | +5.82 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.03% | 88.64% | +4.39 pp | 94.97% | 57.81% |
| Drop Pod S.T | 92.91% | 88.77% | +4.14 pp | 93.16% | 57.42% |
| Terminator S.T | 92.79% | 88.92% | +3.87 pp | 93.29% | 65.49% |
| Pyroclast Squad S.T | 92.73% | 89.05% | +3.68 pp | 93.54% | 64.26% |
| Exterminatus S.T | 88.85% | 92.21% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.31% | -2.18 pp | 92.39% | 4.24% |
| Infernus S.T | 91.91% | 90.67% | +1.24 pp | 93.04% | 61.37% |
| Whirlwind S.T | 91.10% | 92.15% | -1.05 pp | 93.48% | 47.83% |
| Servo-Skull | 91.07% | 91.60% | -0.53 pp | 91.09% | 22.04% |
| Demolitionist S.T | 91.57% | 91.32% | +0.25 pp | 93.25% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
