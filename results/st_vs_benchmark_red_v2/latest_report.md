# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 337,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.68% | +5.75 pp | 93.72% | 64.94% |
| Eradicator S.T | 93.04% | 88.70% | +4.34 pp | 94.99% | 57.74% |
| Drop Pod S.T | 92.93% | 88.79% | +4.14 pp | 93.20% | 57.40% |
| Terminator S.T | 92.77% | 89.02% | +3.74 pp | 93.27% | 65.51% |
| Pyroclast Squad S.T | 92.77% | 89.03% | +3.74 pp | 93.57% | 64.30% |
| Exterminatus S.T | 88.93% | 92.21% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.33% | -2.19 pp | 92.48% | 4.25% |
| Infernus S.T | 91.90% | 90.74% | +1.17 pp | 93.05% | 61.37% |
| Whirlwind S.T | 91.13% | 92.14% | -1.01 pp | 93.52% | 47.80% |
| Servo-Skull | 91.01% | 91.64% | -0.63 pp | 91.03% | 22.01% |
| Demolitionist S.T | 91.56% | 91.38% | +0.18 pp | 93.26% | 47.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
