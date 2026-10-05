# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 8,000
**S.T win rate:** 92.22%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.15% | 88.68% | +5.47 pp | 95.71% | 58.61% |
| Servitor | 93.99% | 88.70% | +5.29 pp | 94.26% | 65.28% |
| Pyroclast Squad S.T | 93.49% | 89.73% | +3.76 pp | 94.28% | 64.41% |
| Drop Pod S.T | 93.41% | 89.97% | +3.43 pp | 93.64% | 57.59% |
| Exterminatus S.T | 89.69% | 92.93% | -3.23 pp | 0.00% | 0.00% |
| Terminator S.T | 93.10% | 90.56% | +2.54 pp | 93.41% | 65.25% |
| Servo-Skull | 90.57% | 92.72% | -2.15 pp | 90.60% | 22.20% |
| Vindicator S.T | 91.24% | 92.81% | -1.57 pp | 93.69% | 4.16% |
| Infernus S.T | 92.46% | 91.79% | +0.66 pp | 93.50% | 61.16% |
| Whirlwind S.T | 92.03% | 92.56% | -0.53 pp | 93.77% | 47.95% |
| Demolitionist S.T | 92.28% | 92.13% | +0.16 pp | 93.94% | 47.45% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
