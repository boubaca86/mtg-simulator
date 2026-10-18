# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 164,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.75% | +5.76 pp | 93.79% | 65.00% |
| Eradicator S.T | 93.15% | 88.72% | +4.42 pp | 95.05% | 57.84% |
| Drop Pod S.T | 93.02% | 88.86% | +4.16 pp | 93.27% | 57.48% |
| Terminator S.T | 92.86% | 89.10% | +3.76 pp | 93.35% | 65.51% |
| Pyroclast Squad S.T | 92.84% | 89.12% | +3.73 pp | 93.63% | 64.42% |
| Exterminatus S.T | 89.02% | 92.29% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.37% | -2.08 pp | 92.79% | 4.21% |
| Infernus S.T | 92.01% | 90.78% | +1.23 pp | 93.14% | 61.49% |
| Whirlwind S.T | 91.19% | 92.27% | -1.09 pp | 93.51% | 47.79% |
| Servo-Skull | 91.19% | 91.70% | -0.50 pp | 91.25% | 22.04% |
| Demolitionist S.T | 91.65% | 91.45% | +0.21 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
