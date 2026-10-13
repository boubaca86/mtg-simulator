# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 108,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.59% | +5.95 pp | 93.81% | 64.89% |
| Eradicator S.T | 93.15% | 88.62% | +4.53 pp | 95.07% | 57.89% |
| Drop Pod S.T | 93.00% | 88.78% | +4.23 pp | 93.30% | 57.49% |
| Pyroclast Squad S.T | 92.84% | 89.02% | +3.81 pp | 93.65% | 64.41% |
| Terminator S.T | 92.83% | 89.05% | +3.78 pp | 93.32% | 65.54% |
| Exterminatus S.T | 89.05% | 92.24% | -3.19 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.30% | -2.00 pp | 92.66% | 4.24% |
| Infernus S.T | 91.95% | 90.78% | +1.18 pp | 93.10% | 61.51% |
| Whirlwind S.T | 91.12% | 92.29% | -1.17 pp | 93.38% | 47.87% |
| Servo-Skull | 91.20% | 91.65% | -0.45 pp | 91.26% | 22.00% |
| Demolitionist S.T | 91.58% | 91.48% | +0.10 pp | 93.24% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
