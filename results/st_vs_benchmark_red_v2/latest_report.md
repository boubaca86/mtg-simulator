# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 169,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.71% | +5.80 pp | 93.79% | 65.01% |
| Eradicator S.T | 93.14% | 88.70% | +4.44 pp | 95.04% | 57.86% |
| Drop Pod S.T | 92.99% | 88.87% | +4.12 pp | 93.24% | 57.49% |
| Terminator S.T | 92.84% | 89.10% | +3.74 pp | 93.33% | 65.48% |
| Pyroclast Squad S.T | 92.81% | 89.13% | +3.68 pp | 93.60% | 64.40% |
| Exterminatus S.T | 88.99% | 92.28% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.35% | -2.06 pp | 92.68% | 4.21% |
| Infernus S.T | 92.01% | 90.74% | +1.27 pp | 93.14% | 61.47% |
| Whirlwind S.T | 91.18% | 92.24% | -1.06 pp | 93.50% | 47.78% |
| Servo-Skull | 91.20% | 91.68% | -0.48 pp | 91.25% | 22.04% |
| Demolitionist S.T | 91.64% | 91.43% | +0.22 pp | 93.29% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
