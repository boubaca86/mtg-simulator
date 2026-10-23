# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 224,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.64% | +5.82 pp | 93.75% | 64.93% |
| Eradicator S.T | 93.06% | 88.66% | +4.40 pp | 94.98% | 57.79% |
| Drop Pod S.T | 92.96% | 88.75% | +4.20 pp | 93.20% | 57.46% |
| Terminator S.T | 92.79% | 89.01% | +3.79 pp | 93.29% | 65.41% |
| Pyroclast Squad S.T | 92.77% | 89.03% | +3.74 pp | 93.58% | 64.32% |
| Exterminatus S.T | 88.82% | 92.25% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.32% | -2.15 pp | 92.55% | 4.23% |
| Infernus S.T | 91.94% | 90.69% | +1.25 pp | 93.07% | 61.42% |
| Whirlwind S.T | 91.13% | 92.15% | -1.02 pp | 93.50% | 47.85% |
| Servo-Skull | 91.10% | 91.62% | -0.52 pp | 91.13% | 22.04% |
| Demolitionist S.T | 91.60% | 91.33% | +0.27 pp | 93.25% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
