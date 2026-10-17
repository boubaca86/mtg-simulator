# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 148,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.71% | +5.80 pp | 93.79% | 64.98% |
| Eradicator S.T | 93.13% | 88.70% | +4.43 pp | 95.05% | 57.87% |
| Drop Pod S.T | 93.02% | 88.82% | +4.20 pp | 93.27% | 57.44% |
| Terminator S.T | 92.84% | 89.08% | +3.77 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.81% | 89.13% | +3.69 pp | 93.62% | 64.41% |
| Exterminatus S.T | 88.94% | 92.30% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.37% | -2.11 pp | 92.84% | 4.21% |
| Infernus S.T | 91.99% | 90.76% | +1.23 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.18% | 92.24% | -1.06 pp | 93.49% | 47.84% |
| Servo-Skull | 91.19% | 91.68% | -0.49 pp | 91.24% | 21.99% |
| Demolitionist S.T | 91.66% | 91.39% | +0.27 pp | 93.31% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
