# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 28,500
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.75% | +5.84 pp | 93.84% | 65.29% |
| Eradicator S.T | 93.32% | 88.56% | +4.76 pp | 95.07% | 58.37% |
| Drop Pod S.T | 93.06% | 88.92% | +4.13 pp | 93.37% | 57.70% |
| Pyroclast Squad S.T | 92.96% | 89.12% | +3.83 pp | 93.68% | 63.99% |
| Terminator S.T | 92.69% | 89.62% | +3.07 pp | 93.13% | 65.35% |
| Exterminatus S.T | 89.45% | 92.24% | -2.80 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.51% | -2.32 pp | 92.23% | 4.15% |
| Infernus S.T | 91.91% | 91.15% | +0.76 pp | 93.04% | 61.53% |
| Demolitionist S.T | 91.90% | 91.19% | +0.71 pp | 93.61% | 47.35% |
| Servo-Skull | 91.25% | 91.76% | -0.51 pp | 91.24% | 22.00% |
| Whirlwind S.T | 91.48% | 91.92% | -0.44 pp | 93.72% | 47.74% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
