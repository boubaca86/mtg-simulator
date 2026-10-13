# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 105,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.56% | +5.98 pp | 93.80% | 64.85% |
| Eradicator S.T | 93.14% | 88.59% | +4.56 pp | 95.06% | 57.94% |
| Drop Pod S.T | 92.99% | 88.75% | +4.24 pp | 93.30% | 57.50% |
| Pyroclast Squad S.T | 92.83% | 88.99% | +3.85 pp | 93.65% | 64.36% |
| Terminator S.T | 92.80% | 89.05% | +3.74 pp | 93.29% | 65.51% |
| Exterminatus S.T | 89.05% | 92.22% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.30% | -2.04 pp | 92.61% | 4.23% |
| Whirlwind S.T | 91.10% | 92.27% | -1.17 pp | 93.35% | 47.90% |
| Infernus S.T | 91.92% | 90.78% | +1.14 pp | 93.08% | 61.49% |
| Servo-Skull | 91.17% | 91.63% | -0.46 pp | 91.23% | 21.99% |
| Demolitionist S.T | 91.58% | 91.43% | +0.15 pp | 93.22% | 47.32% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
