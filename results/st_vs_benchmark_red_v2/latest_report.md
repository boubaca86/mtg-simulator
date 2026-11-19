# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 378,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.67% | +5.76 pp | 93.71% | 64.99% |
| Eradicator S.T | 93.03% | 88.71% | +4.31 pp | 95.01% | 57.73% |
| Drop Pod S.T | 92.93% | 88.78% | +4.14 pp | 93.20% | 57.41% |
| Pyroclast Squad S.T | 92.77% | 89.02% | +3.75 pp | 93.59% | 64.28% |
| Terminator S.T | 92.75% | 89.05% | +3.71 pp | 93.26% | 65.52% |
| Exterminatus S.T | 88.94% | 92.21% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.33% | -2.19 pp | 92.36% | 4.25% |
| Infernus S.T | 91.92% | 90.71% | +1.21 pp | 93.07% | 61.38% |
| Whirlwind S.T | 91.14% | 92.11% | -0.97 pp | 93.55% | 47.80% |
| Servo-Skull | 91.04% | 91.63% | -0.59 pp | 91.07% | 22.05% |
| Demolitionist S.T | 91.55% | 91.40% | +0.14 pp | 93.25% | 47.02% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
