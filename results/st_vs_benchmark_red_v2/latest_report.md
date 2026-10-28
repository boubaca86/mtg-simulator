# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 283,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.65% | +5.80 pp | 93.73% | 64.97% |
| Eradicator S.T | 93.05% | 88.66% | +4.39 pp | 94.99% | 57.78% |
| Drop Pod S.T | 92.92% | 88.80% | +4.12 pp | 93.17% | 57.41% |
| Terminator S.T | 92.80% | 88.97% | +3.83 pp | 93.29% | 65.47% |
| Pyroclast Squad S.T | 92.76% | 89.04% | +3.72 pp | 93.56% | 64.28% |
| Exterminatus S.T | 88.92% | 92.21% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.34% | -2.21 pp | 92.38% | 4.24% |
| Infernus S.T | 91.93% | 90.69% | +1.24 pp | 93.05% | 61.35% |
| Whirlwind S.T | 91.13% | 92.14% | -1.01 pp | 93.50% | 47.80% |
| Servo-Skull | 91.09% | 91.62% | -0.53 pp | 91.10% | 22.05% |
| Demolitionist S.T | 91.58% | 91.35% | +0.24 pp | 93.24% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
