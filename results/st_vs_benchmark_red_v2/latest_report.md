# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 145,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.73% | +5.79 pp | 93.80% | 64.97% |
| Eradicator S.T | 93.14% | 88.72% | +4.41 pp | 95.06% | 57.88% |
| Drop Pod S.T | 93.03% | 88.82% | +4.21 pp | 93.29% | 57.46% |
| Terminator S.T | 92.85% | 89.11% | +3.74 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.13% | +3.70 pp | 93.64% | 64.41% |
| Exterminatus S.T | 88.94% | 92.31% | -3.37 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.37% | -2.09 pp | 92.81% | 4.21% |
| Infernus S.T | 91.99% | 90.80% | +1.19 pp | 93.14% | 61.52% |
| Whirlwind S.T | 91.20% | 92.24% | -1.05 pp | 93.51% | 47.84% |
| Servo-Skull | 91.20% | 91.69% | -0.48 pp | 91.26% | 21.98% |
| Demolitionist S.T | 91.67% | 91.41% | +0.26 pp | 93.32% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
